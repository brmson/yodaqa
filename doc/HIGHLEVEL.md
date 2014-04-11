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

Each CAS has a CASId feature uniquely describing its origin.

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
    SV is one of the primary clues but is found in a special way and
    might (or might not) be used specially in answer selection.

  * **Focus.** This is the focus point of the sentence where you should
    be able to place the answer.  In "What was the first book written by
    Terry Pratchett?", "what" is the focus.  In "The actor starring in Moon?",
    "the actor" is the focus (though that doesn't work terribly well).
    Typically, focus would be used by aligning algorithms.

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
a copy of the QuestionCAS featuresets, and a few extra info like a match degree
rank.

Typically, one would have a separate Primary Search AE for each
engine (Solr, Indri, ...) and instantiated separately for each data
source.

We may want to split this phase to a separate search and retrieval
phases, but it's not clear what the advantage would be yet.

## Answer Generator

This phase is represented by CAS multipliers that grab a **SearchResultCAS**,
annotate it and generate some **CandidateAnswerCAS** instances.

The sofa of the CandidateAnswerCAS is, well, the candidate answer (usually one
word or a few words), plus some features (like a score, hypothesis about the
type of the answer etc.) and a copy of the question analysis.

## Answer Ranker

Tihs phase is a CAS multiplier that consumes all **CandidateAnswerCAS**,
ranks them and outputs a single **FinalAnswerCAS** CAS containing the ranked
list of answers (with confidence scores), possibly also adding some combined
answers.

## Answer Writer

This IO phase is a CAS consumer that serializes the **FinalAnswerCAS** to
whatever output medium (the console, an IRC-connected pipe or whatever).
