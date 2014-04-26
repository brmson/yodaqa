YodaQA High Level Design Notes
==============================

Here, we consider the basic design of a YodaQA pipeline within the UIMA
framework, how the Common Analysis Structures (CAS) are segmented and down to
the most important CAS types. This is heavily inspired by Epstein et al.,
Making Watson fast.  Maybe look at Fig. 1 there before reading further.

QA Pipeline Flow
----------------

Contrary to OAQA, our pipeline components are not shielded from UIMA
by a nice abstraction, instead they are "raw" analysis components that
directly deal with CAS management etc.  Many of them are CAS multipliers.
Specific NLP/ML algorithms are wrapped in annotators, here we are more
concerned about the high level analysis flow.

This is the barebones configuration. In time, we will want to add extra
stages like Supporting Evidence. It is not set in stone, especially
feel free to further split phases and introduce extra intermediate CASes.
Also, parts of it may still be hypothetical or not fully implemented.

Each CAS has a CASId feature uniquely describing its origin.

Some CASes are carried over to other CASes by the means of generating
a separate view.

## Question Reader

The initial, IO phase is a collection reader that acquires question(s),
creates a **QuestionCAS** with the question as a sofa and passes it on.

## Question Analysis

In this phase, we simply take a **QuestionCAS** and run a bunch of annotators
on it. Notable types of final featuresets produced by the annotators:

  * **Clue.** These represent key information stored in the question that is
    then used in primary search.  E.g. "What was the first book written by
    Terry Pratchett?" should generate clues "first", "book", "first book"
    and "Terry Pratchett".

  * **SV** (Selective Verb). These represent the coordinating verb of the
    question that "selects" the answer with regard to other clues. E.g.
    "Who has received the Nobel Prize for Physiology and Medicine?" will
    have "received" as SV; "When were they born?" will have "born";
    "How many colors do you need to color a planar graph?" will have "need".
    N.B. "Who was the inventor of transistor" will have "inventor"!
    SV is one of the primary clues but is found in a special way and
    might (or might not) be used specially in answer selection.

  * **Focus.** This is the focus point of the sentence indicating the
    main constraing of a subject of the answer.  In "What was the first book
    written by Terry Pratchett?", "book" is the focus.  In "The actor starring
    in Moon?", "actor" is the focus.  In "Who invented the transistor?",
    "who" will be the focus.  Typically, focus would be used by aligning
    algorithms and for type coercion.

  * **LAT** (Lexical Answer Type). These are words that should be
    type-coercable to the answer term. E.g. "Who starred in Moon?" should
    generate LATs "who", "actor", possibly "star".  Candidate answers
    will be matched against LATs to acquire score.  Focus is typically
    always also an LAT.

  * **NEAT** (Named Entity Answer Type). These are named entity types that
    should match named entity types of generated answers.  E.g. "Who
    starred in Moon?" should generate NEATs "NEperson", "NEactor".
    Candidate answers will be matched against NEATs to acquire score.

As is the case in the rest of the flow, multiple annotators may
concurrently produce the same featuresets.  At the same time, not all
featuresets may be produced, especially in the beginning.

Also, the annotators producing final featuresets may work on top of
intermediate featuresets produced by other annotators earlier; e.g.,
tokenization will be done first and reused by anyone else.

See also Lally et al., Question analysis: How Watson reads a clue.

## Primary Search

This phase is represented by a set of CAS multipliers that take a populated
**QuestionCAS** on input and produce a bunch of **SearchResultCAS** on output.

The sofa of the SearchResultCAS is a retrieved document or passage that is
fetched by a corpus search based on the question featuresets.  It also contains
a copy of the QuestionCAS featuresets in a Question view, and a few extra info
like a match degree rank.

Typically, one would have a separate Primary Search AE for each
engine (Solr, Indri, ...) and instantiated separately for each data
source.

We may want to split this phase to a separate search and retrieval
phases, but it's not clear what the advantage would be yet.

## Result Analysis

We do a lot of steps similar to the Question Analysis phase, but with the
**SearchResultCAS** instead.  Generally, we tokenize the search result,
keep only the sentences that are related to our query and possibly some
surrounding sentences (and resolving coreferences) etc.  Interesting
sentences are rated based on their relevance, only the most interesting
ones are filtered and cross-referenced with the question clues and
answers can be generated from that in a variety of ways.

### Passage Extraction

We extract relevant passages --- sentences that contain some of the clues.

Relevant passages are extracted and covered by a Passage annotation
in a Passages view (we use a dedicated view to be able to carry over
just the relevant subset of annotations and thus limit operation of
NLP analysis annotators like parsers just to this subset).

### Passage Filtering

Passages are ranked by relevance and the most relevant ones are carried
to a PickedPassages view.

### Answer Extraction

In-depth sentence analysis is done on the remaining passages and candidate
answers are generated based on this.  (So many approaches are possible;
for starters, we choose a naive strategy of simply picking the NP clauses
that are not covered by clues.)  CandidateAnswer annotations (back in
the Result view!) represent these.

## Answer Generator

This phase is a simple CAS multiplier that grabs a **SearchResultCAS**,
and generates a **CandidateAnswerCAS** instance for each CandidateAnswer
annotation.

The sofa of the CandidateAnswerCAS is, well, the candidate answer (usually one
word or a few words), plus some features (like a score, hypothesis about the
type of the answer etc.), a copy of the question analysis in the Question
view and (maybe) copy of the search result information in the Result view.

There is not much of a point of this intermediate CAS right now, but we
may insert additional stages here that will analyze candidate answers
in more depth, produce and consider supporting evidence, etc.

## Answer Ranker

Tihs phase is a CAS multiplier that consumes all **CandidateAnswerCAS**,
ranks them and outputs a single **FinalAnswerCAS** CAS containing the ranked
list of answers (with confidence scores), possibly also adding some combined
answers.

## Answer Writer

This IO phase is a CAS consumer that serializes the **FinalAnswerCAS** to
whatever output medium (the console, an IRC-connected pipe or whatever).
