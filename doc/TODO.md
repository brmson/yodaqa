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

Short-Term TODO
---------------

Quality:
  * Some better (naive) question focus identification
  * More question analysis features:
    * Question word
    * Question verb
    * Is the focus noun last noun in sentence?
      (14 is the atomic number of what element?)
    * Focus dependents
  * Simple answer type system (in addition to TyCor in future; we can
    have both and combine them during evaluation); we can use the
    Taming Text training data
  * Involve the OpenNLP NamedEntity extractor
  * Use WordNet to find synsets for (some?) clues
  * Better passage scoring?
  * Walk through the QA chapter of Taming Text and add more TODO items. :)

Search:
  * Switch to SolrJ
  * SpanQuery?

Long-Term TODO
--------------

Parsing:
  * Shallower alternative to BerkeleyParser;
    * For starters, POS tagging?
    * Try MSTParser with conll_mcd_order2_0.1.model, KenLM?
  * Take a very good look at the RegEx Link Grammar
    <http://wiki.opencog.org/w/RelEx>

Quality:
  * Instead of fixed NE categories, use clustering
  * Type coercion component?
  * Add structured information sources!
    * DBPedia, WordNet, FrameNet, Lemon, PATTY, PPDB
  * Walk through IBM Watson papers and add more TODO items. :)

Performance Measurement:
  * Export full coefficient vectors of candidate answer and answer
    ranking, use that for parameter learning

Speed:
  * Multi-threaded CAS Processing - a massive boost!  Does this
    strictly require using UIMA-AS or can we do without it?
  * Persistent instances of expensive-to-initialize annotators

Interface:
  * Readline interface for the interactive IO?
  * Web interface.
  * Give context to answers, allow for clarifications and iterative
    conversation.

UIMA Infrastructure:
  * Add an origin record to each annotation - which annotator
    produced it? Will be useful when we have multiple possible
    annotation paths.
  * Add a per-CAS singleton containing unique id, id of the
    spawning document and id of the originating CAS; this will
    enable tracing full origin of each CAS.

Datasets:
  * **NP** Clean up the TREC datasets - weed out questions that
    cannot be answered from Wikipedia, update out-of-date answers,
    remove too ambiguous questions(?)
  * **NP** Prepare a corpus of documents and associated questions
    that can be answered based on information from the documents.
  * **NP** Prepare a corpus of complex questions that require some
    inference.
