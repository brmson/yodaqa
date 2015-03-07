Tasks Needing Attention
=======================

The tasks are divided to short-term items - these need attention
in order to wrap up the current performance improvement phase
and are fairly detailed - and long-term items that outline more
general projects of whose development this project would benefit.

Most of these tasks require Java (or Scala, Groovy?) programming
and (willing to pick up) some UIMA knowledge.  However, if you
want to help out but aren't a programmer, there are tasks marked
as **NP** that are suitable for non-coders too!

Apologies if this list is not kept 100% up-to-date all the time.

Immediate TODO Items
--------------------

These are ideas for simple things to try quickly (this list is
fairly volatile):

  * Revamp LAT features; try carrying wordnetSp values within
    the LAT features (WIP; so far overfitted)

  * Generate a special "boring" feature for answers that contain
    no interesting information; this is important for SolrHitsCount
  * Special handling of "name" queries (simple focus proxy is
    not enough, maybe we want a special feature and even a special
    answer generator?)
  * Try a small decision forest instead of logistic regression
    to capture some simple feature dependencies

(Note that this list is often getting stale.  Please ask pasky@ucw.cz
if you are looking for some interesting tasks.  We will probably move
to tracking these in GitHub issues in the futuer.)

v1.0 Roadmap
------------

Our "release criteria" and general goals for the 1.0 version are
to have a reasonably performing system with all the essential
pipeline stages implemented.  More concretely:

  * About 37.5% accuracy and 75% recall on the test set.  I.e. we
    can generate a correct candidate answer for 3/4 of factoid questions
    and pick it as the correct answer half of the time.  These are
    fairly modest goals but this is just v1.0. :-)

  * The basic logic should bear *conceptual* similarity to DeepQA
    to enable some comparisons and reproducing DeepQA results.
    E.g. I have some alternatives to the LAT concept in mind and
    believe in reasoning using word vector embeddings and RNNs,
    but that should just wait after v1.0.

  * The baseline runs should be fully reproducible (no calls to
    resources out of our control, e.g. Bing/Google).

  * Ideally, all the core QA logic (not NLP or ML tools, of course)
    would still be self-contained and easy to review at this point
    (since that will probably swiftly change afterwards).

To get there, we have to take care of at least the following:

  * Revise the curated dataset, especially numeric answers;
    in other words, have humans annotate right/wrong answers and
    update the regexes accordingly
  * Answer Scoring
    * We probably have a fair amount of overfitting at this stage;
      try different classifiers and clean up the answer FV
  * Passage Answer Extraction
    * Our current strategy of going with all NPs is unreasonable,
      as many answers are only part of larger answers which have
      irrelevant focus
  * Evidence Gathering stage
    * Extra Fulltext Search: An answer feature for normalized
      number of fulltext search hits for clues + candidate answer
    * Extra LAT Extraction: An answer feature for extracting LATs
      from nouns in first sentence of concept enwiki base article.
    * Extra Type Coercion: An answer feature for normalized number
      of fulltext search hits for question LAT + candidate answer

We plan to publish our results in a paper when 1.0 is done.  For that,
we will want to do some comparison benchmarks:

  * Re-benchmark with other datasets:
    * Plain TREC QA dataset and regexes (full + curated-test questions
      only).
    * WebQuestions Dataset (SEMPRE)
    * CMU "Question-Answer Dataset"
  * Benchmark with some sensible "sub-baseline" pipelines.
  * Consider benchmarking with plain TREC QA dataset + the original
    data sources http://trec.nist.gov/data/qa/t9_qadata.html#t9docs
    (I feel opposed to it; this is clumsy and not open science. --pasky)
  * Check the TamingText dataset
  * Benchmark OpenEphyra, BlanQA and Jacana on the same datasets.

For the first paper or for a next stage survey-ish publication:

  * Write a Google-based QA scraping tool and benchmark on the same
    datasets.
  * Benchmark the TamingText "pipeline".
  * Maybe ask IBM Watson and SRI teams for cooperation?

Long-Term Task Ideas
--------------------

This is a collection of might-be-useful things.  Not all of them
might turn out to make sense, this is just a kind of scratchbook.

Parsing:
  * Take a very good look at the RegEx Link Grammar
    <http://wiki.opencog.org/w/RelEx>

Question Analysis Quality:
  * Use (WordNet) ontology relationships (synsets etc.) to generate
    extra clues
  * Perform spellcheck on the question or just clues:
    "When did Alexandra Graham Bell invent the telephone?"
  * Multi-tiered clues for passage search; required and optional

CandidateAnswer Recall Quality:
  * Generate questions from structured information sources!
    * DBPedia, FrameNet, Lemon, PATTY, PPDB
  * More advanced NE extraction, multi-word NEs
  * Use SpanQuery for more accurate Solr IE?

FinalAnswer Precision Quality:
  * A framework for merging related answers:
    * Bare-bone done, but needs work - esp. have a separate rescoring
    * Some LAT-specific equivalence classes, e.g. for persons (surnames)
      or dates (YYYY-MM-DD vs. Month D, YYYY, etc.)
  * A framework for evidence diffusion:
    * Bare-bone work done (syntactic diffusion)
    * Syntactic diffusion through stripping of numeric suffixes
    * If a capital has a lot of evidence and the question is for a country,
      diffuse the evidence to the country too
  * A type coercion classifier that outputs a generic coercion score
    (or pair of scores like match + specificity) - we need a LAT training
    set for that
  * If an LAT is found in sentence governing an APPOS dependency,
    create the dependent as a candidate answer, as in "Who is the
    brother of Sherlock Holmes?" vs. "His brother, Mycroft, seven
    years his senior"...
  * Consider generating Clues also for CandidateAnswer
    and matching them similar to TyCor (ClueCor? :-)
  * Extend WordNet ontology (so far used for TyCor) with more
    resources - dkpro lsr or uby
  * Label-less LATs via NE clustering

FinalAnswer Prediction Machine Learning:
  * Try training a multi-layer perceptron instead of logistic
    classifier
  * Disable resultLogScore feature for concept-based results
  * The scoreSimple feature turns out to be fairly important
    (avgscore 0.474 becomes 0.493); this indicates something
    fishy about our current logistic regression based model,
    we should try to find out why that feature works

General Quality:
  * Evidence gathering feedback loops - initially maybe just
    a single feedback loop for candidate answer evidence gathering,
    but we may also gather evidence for LATs (and their synsets!) etc.

Speed:
  * Multi-threaded CAS Processing - a massive boost!  Does this
    strictly require using UIMA-AS or can we do without it?
  * Shallower alternative to StanfordParser when parsing results?
    * For starters, POS tagging?
    * Try MSTParser with conll_mcd_order2_0.1.model, KenLM?

Interface:
  * Readline interface for the interactive IO?
  * Web interface.
  * Give context to answers, allow for clarifications and iterative
    conversation; add a lot of hooks but I think this is pretty key
    for a useful web interface and can make the product so much more
    valuable for actual usage.

Janitorial:
  * There are many common chunks of code (e.g. wrt feature addition
    to answer featuresets) that should be factored out to utility
    classes and some common abstract base classes
  * Do some pipeline stage class renaming - instead of *Generator,
    use *Splitter terminology, e.g. AnswerGenerator to
    AnswerResultSplitter, AnswerCASSplitter to AnswerHitlistSplitter...
  * Log answer processing during batch evaluations to separate files
  * Switch to SolrJ, extWordNet
    * extWordNet would have a big tangible benefit as we'd gain access
      e.g. to useCount of WordNet synsets, but it seems to mess with
      our logging subsystem
  * Add an origin record to each annotation - which annotator
    produced it? Will be useful when we have multiple possible
    annotation paths.
  * Add a per-CAS singleton containing unique id, id of the
    spawning document and id of the originating CAS; this will
    enable tracing full origin of each CAS.
  * Factor out magic constants and other numeric parameters hardcoded
    in annotators and make them AE parameters instead; mark them in
    a unified way that will make it possible to collect and summarize
    all the tunable parameters.
  * The type system distinction by pipeline phases does not work well;
    .tycor is a step in the right direction, but now CandidateAnswerCAS
    also has Focus from QuestionTypes, which does not fit in .tycor...

Datasets:
  * Figure out how to crowdsource / involve humans in answer
    classification; regexes don't do a good enough job
  * **NP** Prepare a corpus of documents and associated questions
    that can be answered based on information from the documents.
  * **NP** Prepare a corpus of complex questions that require some
    inference.
