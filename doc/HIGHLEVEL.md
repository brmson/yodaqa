YodaQA High Level Design Notes
==============================

Here, we consider the basic design of a YodaQA pipeline within the UIMA
framework, how the Common Analysis Structures (CAS) are segmented and down to
the most important CAS types. This is heavily inspired by Epstein et al.,
Making Watson fast.  Please take a look at the YodaQA papers (see
http://ailao.eu/yodaqa/ for a list) first to get the general idea about
how YodaQA answers questions (it even has a diagram!); this is the description
of the same flow but on a a more technical level.

QA Pipeline Flow
----------------

Contrary to OAQA, our pipeline components are not shielded from UIMA
by a nice abstraction, instead they are "raw" analysis components that
directly deal with CAS management etc.  Many of them are CAS multipliers.
Specific NLP/ML algorithms are wrapped in annotators, here we are more
concerned about the high level analysis flow.

The configuration described below is a bit abstract, meant to give you
a rough idea about YodaQA organization, but certainly not 1:1 with
precisely what's implemented at any point.  It is not set in stone, especially
feel free to further split phases and introduce extra intermediate CASes.
Also, parts of it may still be hypothetical or not fully implemented.

Most of the CASes have a special "...Info" featurestructure that contains
description of the CAS origin, and some unique IDs.  Most of the generated
resources are also stored in the question dashboard, see below for details.

Some CASes are carried over to other CASes by the means of generating
a separate view.

A summary of the pipeline flow:
  * **Question Reader** loads the question from an input source
  * **Question Analysis** extracts NLP features from the question text and
    produces QA features based on them (clues, type, etc.)
  * **Answer Production** generates answers based on the question.
    Typically, this happens by performing a search (“primary search”) on
    a data source based on question clues, and either directly using search
    results as answers or filtering relevant passages from these and generating
    candidate answers from these passages; many answer features are already
    generated in this phase
  * **Answer Analysis** generates further answer features based on detailed
    analysis (most importantly, type determination and coercion to question
    type)
  * **Answer Merging and Scoring** consolidates the set of answers, removing
    duplicates and using a machine learned classifier to score answers based
    on their features
  * **Answer Writer** sends the question to an output sink

## Question Reader

The initial, IO phase is a collection reader that acquires question(s),
creates a **QuestionCAS** with the question as a sofa and passes it on.

## Question Analysis

In this phase, we simply take a **QuestionCAS** and run a bunch of annotators
on it. Notable types of final featuresets produced by the annotators:

  * **Clue.** These represent key information stored in the question that is
    then used in primary search.  E.g. "What was the first book written by
    Terry Pratchett?" should generate clues "first", "book", "first book"
    and "Terry Pratchett".  Most of the annotations below are *also* clues.

  * **Concept.** These represent the "topic" of the question, as determined
    by question sub-string (e.g. an existing clue) which happens to match
    a title or alias of an enwiki article.  These concepts are then used as
    a basis for more focused answer production than a general full-text search;
    be it from the article or from various structured knowledge bases which are
    associated with the matched enwiki article.  The concept also carries
    information about the "canonical name" of mentions as a Wikipedia redirect
    can match the question sub-string too.

  * **SV** (Selective Verb). These represent the coordinating verb of the
    question that "selects" the answer with regard to other clues. E.g.
    "Who has received the Nobel Prize for Physiology and Medicine?" will
    have "received" as SV; "When were they born?" will have "born";
    "How many colors do you need to color a planar graph?" will have "need".
    N.B. "Who was the inventor of transistor" will have "inventor"!
    SV is one of the primary clues but is found in a special way and
    might (or might not) be used specially in answer selection.

  * **Focus.** This is the focus point of the sentence indicating the
    main constraint of a subject of the answer.  In "What was the first book
    written by Terry Pratchett?", "book" is the focus.  In "The actor starring
    in Moon?", "actor" is the focus.  In "Who invented the transistor?",
    "who" will be the focus.  Typically, focus would be used by aligning
    algorithms and for type coercion.

  * **LAT** (Lexical Answer Type). These are words that should be
    type-coercable to the answer term. E.g. "Who starred in Moon?" should
    generate LATs "who", "actor", possibly "star".  Candidate answers
    will be matched against LATs to acquire score.  Focus is typically
    always also an LAT.  We should also carry information whether the
    LAT is a specific entity (in question, the-; in answer, a named entity)
    or generic role (in question, a-; in answer, a wordnet synset word).

  * **Subject.** This is the main "topic" of the answer and helps to indicate
    the primary sources.  In "The actor starring in Moon?", "Moon" is the
    subject.  In "Who invented the transistor?", "transistor" will be the
    subject.

As is the case in the rest of the flow, multiple annotators may
concurrently produce the same featuresets.  At the same time, not all
featuresets may be produced, especially in the beginning.

Also, the annotators producing final featuresets may work on top of
intermediate featuresets produced by other annotators earlier; e.g.,
tokenization will be done first and reused by anyone else.

See also Lally et al., Question analysis: How Watson reads a clue.

Eventually, we have use mainly for **clues** and **concepts** which represent
two approaches to find answer sources (search and direct topic data extraction)
and for the **LAT** which serves as the primary answer filter.

## Answer Production

Based on the question stored and analyzed in the **QuestionCAS**,
some candidate answers (one in a separate **CandidateAnswerCAS**
instance) are produced.  This can be done in multiple
possible flows; the primarily used flow is described below.

Other possible flows include querying a structured database on
relationships detected in the question, or doing something like
a primary search, but just using the title of found documents
as candidate answers directly.

In the future, for the purpose of supporting evidence gathering
for considered answers, we might want to actually recurse this flow,
presenting the candidate answer as a true/false question.

### Primary Search

This phase performs searches in a variety of data sources based
on the question featuresets, creating a new Search view in the **QuestionCAS**
that contains results of the searches, just in the form of handles, not
retrieved passages or documents.

In the next phase (result generator), these handles are converted
to separate CASes. We split it to two phases so that we can do
deduplication of results obtained via various search strategies.

However, in some cases even the actual search might be done in the
result generator, e.g. for relations stored in structured sources,
just like in some cases, we actually may store even Passage or actual
CandidateAnswer featuresets (see below) in the Search view - when
the search yields very specific short results, e.g. simply document
titles.  In other words, the Primary Search / Result Generator split
is kind of flexible, not set in stone.

### Result Generator

This phase is represented by a set of CAS multipliers that take a populated
**QuestionCAS** on input including search handles and produce a bunch
of **SearchResultCAS** on output.  The sofa of the SearchResultCAS
is a retrieved document or passage.  It also contains a copy of the
QuestionCAS featuresets in a Question view, and a few extra info
like a match degree rank.

Typically, one would have a separate Result Generator AE for each
engine (Solr, Indri, ...) and instantiated separately for each data
source.

### Result Analysis

We do a lot of steps similar to the Question Analysis phase, but with the
**SearchResultCAS** instead.  Generally, we tokenize the search result,
keep only the sentences that are related to our query and possibly some
surrounding sentences (and resolving coreferences) etc.  Interesting
sentences are rated based on their relevance, only the most interesting
ones are filtered and cross-referenced with the question clues and
answers can be generated from that in a variety of ways.

#### Passage Extraction

We extract relevant passages --- sentences that contain some of the clues.

Relevant passages are extracted and covered by a Passage annotation
in a Passages view (we use a dedicated view to be able to carry over
just the relevant subset of annotations and thus limit operation of
NLP analysis annotators like parsers just to this subset).

#### Passage Filtering

Passages are ranked by relevance and the most relevant ones are carried
to a PickedPassages view.

The ranking is based on score that is computed from various features
and based on machine learned classifier (that prefers passages matching
the correct answer regex).

#### Answer Extraction

In-depth sentence analysis is done on the remaining passages and candidate
answers are generated based on this.  (So many approaches are possible;
for starters, we choose a naive strategy of simply picking the NP clauses
that are not covered by clues.)  CandidateAnswer annotations (in the
PickedPassages view!) represent these.

As a more advanced strategy than the baselines above, we draw inspiration from
one of the state-of-art papers in the area, "Answer Extraction as Sequence
Tagging with Tree Edit Distance" (Yao and van Durme, 2013), except that we
dissect it a bit, doing two in principle independent things:

  * Aligning the parse tree of the passage with the parse tree of the
    question.  (Though this is not terribly novel in principle.)  Focus
    aligned sentence portions may be candidate answers.

  * Tagging passage tokens as B-I-O of the answer (beginning, inside,
    outside) based on a (CRF) sequence model that uses a variety of features,
    including alignment features above, and generates candidate answers
    based on the tags.

### Answer Generator

This phase is a simple CAS multiplier that grabs a **SearchResultCAS**,
and generates a **CandidateAnswerCAS** instance for each CandidateAnswer
annotation.

The sofa of the CandidateAnswerCAS is, well, the candidate answer (usually one
word or a few words), plus some features (like a score, hypothesis about the
type of the answer etc.), a copy of the question analysis in the Question
view and (maybe) copy of the search result information in the Result view.

## Answer Analysis

This phase runs a variety of analytics on the **CandidateAnswerCAS**
that extract various features, match them to the Question view,
possibly produce and consider supporting evidence, etc.

The most notable thing happenning here is type coercion, estimating how
well the answer fits the question.

## Answer Hitlist Scoring 0

In this phase, a CAS multiplier consumes all the analyzed
**CandidateAnswerCAS** instances and pours everything down to a single
**AnswerHitlistCAS** CAS.  Within this CAS, identical or similar answers
are merged or combined.

The answers have their features consolidated, and some auxiliary features
generated (e.g. hitlist-normalized versions of base features).

*At this point, the AnswerHitlistCAS may be serialized and stored
to disk, typically for the purpose of training a classifier.  The pipeline
is then stopped, and can be later started from this point on, restoring
the hitlists for each question.*

The answers are scored (by estimated probability of being correct, using
a machine learned model based on the features) and ranked (by their score).
This is taken care of the **AnswerScoringAE** pipeline.

## Answer Pruning, Merging and Evidence Diffusion

The answer hitlist may be pruned to top N (let's say N=100); the idea is
that scoring (esp. normalized) may work better with the most noise
wed out.

Within the collected answers, those which are deemed as equivalent
or partially equivalent share their features - basically, each such
answer includes the sum of scores of equivalent answers as a feature.

If the answers are completely equivalent (e.g. just an extra "the -"
etc.), the answers are **merged** by removing the answers equivalent
to the top-scored one.  If the answers are only partially equivalent
(e.g. country vs. its capital), the score is transferred but no removal
is done and we call this **evidence diffusion**.

(For merging, we might actually just merge the feature vectors themselves
rather than transfer the score to a separate feature; the evidence is
always diffused through features.)

## Answer Hitlist Scoring 1

Answers are re-scored and re-ranked.

In all scorings after Scoring 0, the classifier is trained while considering
only a single correct answer for each question, not all of them - the idea is
to focus the features more.  Also, the scores of previous scoring phases are
included as features for the classifier.

## Answer Evidence Gathering

New **CandidateAnswerCAS** instances are re-spawned for an "elite" portion
of top N answers scored previously, and extra, possibly more expensive,
analysis is run on them and evidence gathered.  Typically, this may involve
something like an extra set of searches for clues + answer and counting the
hits, or even something more expensive.  New features are generated based
on that.

## Answer Hitlist Scoring 2

Eventually, these CASes are merged back to the **AnswerHitlistCAS**,
updating the answer features, possibly some more merging occurs.

The top N answers are then re-scored (based on all the features, but a model
separate from scoring 1) and re-ranked.

## Answer Writer

This IO phase is a CAS consumer that serializes the **AnswerHitlistCAS** to
whatever output medium (the console, an IRC-connected pipe or whatever).

## Addendum: Question Dashboard

Independently of the UIMA CAS flow, we also collect information about
answers, including their origin snippets (passages or relations) and
sources (documents or entities), in the question dashboard (which resides
in the flow.dashboard sub-package).  This information is redundant to a
degree, it is used for serving question details and answers through the
web interface.  We'll probably redesign this somehow later...
