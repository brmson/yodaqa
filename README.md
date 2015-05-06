YodaQA
======

YodaQA is an open source Question Answering system using on-the-fly
Information Extraction from various data sources (mainly enwiki).
Its goals are practicality and extensible design - it is not a purely
research project, even though we aim to develop YodaQA with appropriate
scientific rigor.  Right now, it is focused on answering factoid
questions and not optimized for speed at all; in the future, we hope
to add some deductive capabilities and include "personal assistant"
style conversation capabilities.

YodaQA stands for "Yet anOther Deep Answering pipeline" and the system is
inspired by the DeepQA (IBM Watson) papers.  It is built on top of the Apache
UIMA and developed as part of the Brmson platform.  For all the NLP logic
(including the NLP type system), we lean heavily on the DKPro UIMA bindings;
transitively, work like the StanfordParser and Princeton's Wordnet is crucial
for us.  We also draw some inspiration from the OpenQA project and the Taming
Text book.

The current version is a work-in-progress snapshot that already can answer
some questions, even though it's embarassingly often wrong; on the testing
corpus, while about 79.3% of questions have the correct answer *suggested*
in the process, it can currently choose the correct answer for about 32.6%
of questions (but 47.6% of questions have the correct answer in top three
and 52.7% in top five candidates).  You can read a pre-print of the first
paper on YodaQA at:

	http://pasky.or.cz/dev/brmson/yodaqa-poster2015.pdf

**Note: This is a fork of the default YodaQA pipeline that is aimed
at hybrid question answering over linked data at the QALD-5 challenge
of CLEF15 QA Labs.**

## Installation Instructions

Quick instructions for setting up, building and running (focused on Debian Wheezy):

  * We assume that you cloned YodaQA and are now in the directory that contains this README.
  * ``sudo apt-get install java7-jdk``
  * ``./gradlew check``
  * ``echo | ./gradlew run -q`` as a "dummy run" which will trigger download
    of all sorts of NLP resources and models.  This will amount to several
    hundreds of megabytes of download!
  * ``./gradlew run -q``

By default, YodaQA will try to connect to a remote Solr core serving Wikipedia;
see the section on Data Sources if connection fails.

Brmson should run on Windows as well, in theory - just have a Java7 JDK
installed and use ``gradlew.bat`` instead of ``./gradlew``.

## Usage

The ``./gradlew run -q`` starts YodaQA with the "interactive"
frontend which offers a prompt and answers questions interactively;
answer candidates and their confidence score are listed after a while
(the first question takes a bit longer to answer as the models etc. are
loaded).

It is also possible to let YodaQA answer many questions at once, e.g. to
measure the performance; use ``./gradlew tsvgs`` to feed YodaQA
the curated testing dataset from data/eval/.  (See also data/eval/README.md
for more details, and a convenient wrapper script ``train-and-eval.sh``.)
Support for connecting YodaQA to other programs (e.g. to IRC) is a work
in progress, see ``contrib/irssi-brmson-pipe.pl`` for an example.

By default, there is a lot of output regarding progress of the answering
process; redirect stderr, e.g. ``2>/dev/null``, to get rid of that.
Alternatively, if things don't go well, try passing an extra parameter
``-Dorg.slf4j.simpleLogger.defaultLogLevel=debug`` on the commandline,
or specifically ``-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa=debug``.

Sometimes, Java may find itself short on memory; don't try to run YodaQA
on systems with less than 8GB RAM.  You may also need to tweak the
minHeapSize and maxHeapSize parameters in ``build.gradle`` when running
on a 32-bit system.

## Data Sources

YodaQA uses Solr fulltext indexing framework as a data source, either
internally or externally.  By default, it will try to connect to the
author's computer, but the Solr Wikipedia instance there may not be
always running.

### Wikipedia Data Source

The remote instance configured by default provides English Wikipedia as a data
source.  It is not too difficult to set this up on your own, but it is very
memory and IO intensive process. You will need about 80-100GiB of disk space and
bandwidth to download 10GiB source file; indexing will require roughly 8GiB RAM.

To index and then search in Wikipedia, we need to set it up as a standalone Solr
source:

  * Download solr (http://www.apache.org/dyn/closer.cgi/lucene/solr/ - we use
    version 4.6.0), unpack and cd to the ``example/`` subdirectory.
  * Symlink or copy the ``data/enwiki/`` directory from this repository to the
    ``example/`` subdirectory; it contains the data import configuration.
  * Proceed with instructions in ``data/enwiki/README.md``.

You may want to edit the URL in ``src/main/java/cz/brmlab/yodaqa/pipeline/YodaQA.java``.

### DBpedia Data Source

Aside of using unstructured enwiki text indexed in solr as data source,
we are now also capable of using some of RDF data extraced from enwiki
by DBpedia.  We actually don't use it as semantic database (i.e. infobox
data and such) so far, but just for enwiki pages metadata --- lookup by
exact title string match (sort of named entity recognition, already linked
to page id we can use to fetch the page from solr) and redirect walking.

By default, we rely on a DBpedia-2014 SPARQL endpoint running on the author's
computer.  In case it is offline, you can try to switch it to the public
DBpedia SPARQL endpoint, though it is prone to outages and we shouldn't use
it too heavily anyway.  Simply edit the ``service`` attribute value in file
``src/main/java/cz/brmlab/yodaqa/provider/rdf/CachedJenaLookup.java``.

Detailed instrutions for setup of local DBpedia SPARQL endpoint can be found
in ``data/dbpedia/README.md``.

### Freebase Data Source

We can also leverage another structured data source, the Freebase.
We use its RDF export with SPARQL endpoint, running on infrastructure
provided by the author's academic group (Jan Šedivý's 3C Group at the
Dept. of Cybernetics, FEE CTU Prague).  If the endpoint is not available
for some reason, you can also simply disable Freebase usage by commenting
out the FreebaseOntologyAnswerProducer initialization in the code of
``src/main/java/cz/brmlab/yodaqa/pipeline/YodaQA.java``.

You can start your own instance by following the instructions in
``data/freebase/README.md`` but it is quite arduous and resource intensive.

## Development Notes

See the [High Level Design Notes](doc/HIGHLEVEL.md) document for
a brief description of YodaQA's design approach.  When hacking brmson
QA logic, you should understand basics of the UIMA framework we use,
see the [UIMA Intro](doc/UIMA-INTRO.md).  You will probably want to
switch back and forth between these two documents when learning about
brmson first.

### Package Organization

YodaQA itself lives in the cz.brmlab.yodaqa namespace, further organized
as such:

  * cz.brmlab.yodaqa.pipeline carries the major pipeline stage aggregates
    and multipliers that manage the data flow etc.; ideally, they shouldn't
    contain much interesting logic and instead delegate that to annotators
  * cz.brmlab.yodaqa.analysis.* carries various analytical tools, mainly
    the annotators containing actual algorithms that operate on the data,
    reading and generating CAS objects; however, not just annotators
  * cz.brmlab.yodaqa.model carries the UIMA type system; classes in this
    package are all auto-generated by JCas tooling
  * cz.brmlab.yodaqa.flow carries UIMA backend tools to execute the pipeline
  * cz.brmlab.yodaqa.io carries I/O modules for question-answer exchange,
    debugging dumps, etc.
  * cz.brmlab.yodaqa.provider carries wrappers for various data sources
    and analytical tools that aren't standalone UIMA annotators (e.g.
    Wordnet); mainly to avoid reinitialization and keep them as singletons

### Machine Learning

Some stages of the QA pipeline use machine learning for scoring snippets
(passages, answers) to pick those which deserve further consideration.
Models should be re-trained every time a non-trivial change in the
pipeline is made.  For details on managing this, please refer to
data/ml/README.md.
