Brmson-Specific UIMA Intro
==========================

We use UIMA (Unstructured Information Management Architecture) as a generic
framework for building the NLP pipeline for processing the question and
possible answers.

The CAS Data Structure
----------------------

UIMA works with **artifacts** (sentence, document, named entity...) that are
coupled with **featuresets** that carry various metadata (features) pertaining
the artifacts.  Most often, these featuresets are **annotations** of the
artifacts, i.e. covering a specific subset (start - end offset span) of the
artifact.  All of the above is stored in a container called **CAS** (Common
Analysis Structure), which has a Java interface class **JCas**.

To recapitulate, **CAS** is a container that holds the artifact and its
various annotations (e.g. token segmentation, dependency parse) and other
featuresets (like LAT, answer feature records).  The artifact can be present
in the CAS in various **views** or forms of the artifact - at least that was
the original idea, but the way we use it, we use the views to carry different
artifacts.  For example, we have views that hold the *Question*, *SearchResult*,
*Passages*, *PickedPassages*, *CandidateAnswer* and *AnswerHitlist*.  We use
these different views to carry and refer to multiple artifacts at once, e.g.
it is quite helpful to be able to refer to Question LATs in CandidateAnswer
processing annotators.  Each view has its own **JCas** Java interface.

The various annotations and other feature sets are defined in so-called
"type systems".  These are ugly XML files that you can find in the
``src/main/typesystem/`` directory.  A magic ``jcasgen`` tool autogenerates
Java classes for them.

The Pipeline Flow
-----------------

A *simple* NLP processing pipeline in UIMA is composed from

  * **Collection Reader**, which generates a CAS for each artifact it loads
    from somewhere (e.g. the standard input or a TSV file).
  * **Analysis Engines** (AE) which take the CAS as a paremeter and when
    processing it turn in turn, they annotate it with various featuresets.
    Some AEs just invoke a sequence of other AEs: these are **Aggregate AEs**,
    while we call those that do the grunt work **annotators**.
  * **CAS Consumer** which does something with the annotated CAS, like print
    it to the output.

In our situation, things are more complex since we work with multiple CASes;
the collection reader generates a CAS for the question, but we will then want
to create a CAS for each search result, and even each candidate answer.
The idea here is that analysis engines will pass each information to each
other just within the CASes, and when we juggle multiple CASes, we can use
that for scale-out to multiple cluster machines.  That's the point of even
having CASes.

Therefore, some of our AEs are **CAS Multipliers** which, for each input CAS
(e.g. a question), generate a bunch of output CASes (e.g. search results).
They generally still carry the input artifact in a separate view of the
output CASes.  Then, near the end, the flow reaches a **CAS Merger** which
will combine input CASes in a single output CAS (e.g. AnswerHitlist).
Therefore, the pipeline is essentially a directed acyclic graph, with
CASes passing on the edges.

The complete flow (except input and output, which can be different for
various YodaQA users and interfaces) is encapsulated in a single large
aggregate AE which is constructed in:

	src/main/java/cz/brmlab/yodaqa/pipeline/YodaQA.java
