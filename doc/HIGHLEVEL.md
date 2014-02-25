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

  * "Keyword". (Possibly "Keyphrase".)
  * "NEAnswerType" with named entity types as features.
  * We may consider also other answer types than NE-based.

(As is the case in the rest of the flow, multiple annotators may
concurrently produce the same featuresets.  Also, the annotators
producing final featuresets may work on top of intermediate featuresets
produced by other annotators earlier.)

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

## Candidate Answer

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
