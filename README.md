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
for us.  For machine learning, we use a mix of scikit-learn and crfsuite
(wrapped by ClearTK, jcrfsuite).  We also draw some inspiration from the
OpenQA project and the Taming Text book.

This branch (d/movies) contains a domain-specific adaptation of YodaQA on
the movies domain - movies, directors, actors, screenplays, academy awards...
We aim to maximize performance on this specific set, gather specific
datasets, etc.  We use specifically trained models, but attempt not
to implement specifically tuned heuristics or hard-coded rules.
We check this on the (i) moviesC, and (ii) WebQuestions datasets.
Detailed performance info is available at:

	https://github.com/brmson/yodaqa/wiki/Benchmarks

More details on YodaQA plus links to some papers are available at:

	http://ailao.eu/yodaqa/

and you can play with a live demo of d/movies at

	http://movies.ailao.eu/

## Installation Instructions

Quick instructions for setting up, building and running (focused on Debian Wheezy):

  * We assume that you cloned YodaQA and are now in the directory that contains this README.
  * ``sudo apt-get install java7-jdk``
  * ``./gradlew check``
  * ``echo | ./gradlew run`` as a "dummy run" which will trigger download
    of all sorts of NLP resources and models.  This will amount to several
    hundreds of megabytes of download!
  * ``./gradlew run -q`` (command line) or ``./gradlew web -q`` (web interface)

By default, YodaQA will try to connect to various remote databases;
see the section on Data Sources if connection fails.

Brmson should run on Windows as well, in theory - just have a Java7 JDK
installed and use ``gradlew.bat`` instead of ``./gradlew``.

## Usage

The ``./gradlew run -q`` starts YodaQA with the "interactive"
frontend which offers a prompt and answers questions interactively;
answer candidates and their confidence score are listed after a while
(the first question takes a bit longer to answer as the models etc. are
loaded).
Alternatively, you can use the "web" frontend by executing
``./gradlew web -q`` and opening e.g. http://localhost:4567/ in your browser.
A shinier web interface is available at https://github.com/brmson/YodaQA-client
and you can also use the web frontend as a REST API.

By default, there is a lot of output regarding progress of the answering
process; redirect stderr, e.g. ``2>/dev/null``, to get rid of that.
Alternatively, if things don't go well or you would like to watch YodaQA
think, try passing an extra command line parameter
``-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa=debug`` to gradle.

Sometimes, Java may find itself short on memory; don't try to run YodaQA
on systems with less than 8GB RAM.  You may also need to tweak the
minHeapSize and maxHeapSize parameters in ``build.gradle`` when running
on a 32-bit system.  By default, YodaQA will try to use *half* of the logical
CPU cores available; set the YODAQA_N_THREADS environment variable to change
the number of threads used.

It is also possible to let YodaQA answer many questions at once, e.g. to
measure the performance; use ``./gradlew tsvgs`` to feed YodaQA
the curated testing dataset from data/eval/.  (See also data/eval/README.md
for more details, and a convenient wrapper script ``train-and-eval.sh``.)
To connect YodaQA to IRC, see ``contrib/irssi-brmson-pipe.pl``.

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
it too heavily anyway, or you can fairly easily set up a local instance of
DBpedia.  Detailed instrutions can be found in ``data/dbpedia/README.md``.

### Freebase Data Source

We can also leverage another structured data source, the Freebase.
We use its RDF export with SPARQL endpoint, running on infrastructure
provided by the author's academic group (Jan Šedivý's 3C Group at the
Dept. of Cybernetics, FEE CTU Prague).  If the endpoint is not available
for some reason, you can also disable Freebase usage by editing the
method getConceptProperties() (instructions inside) of:

	src/main/java/cz/brmlab/yodaqa/pipeline/structured/FreebaseOntologyPrimarySearch.java

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

### Interactive Groovy Shell

The easiest way to get a feel of how various YodaQA classes (esp. helper
classes like the provider.* packages) behave is using a Groovy shell.
Example (hint - use tab completion):

	$ ./gradlew -q shell
	This is a gradle Application Shell.
	You can import your application classes and act on them.
	groovy:000> import cz.brmlab.yodaqa.provider.rdf.DBpediaTypes;
	===> cz.brmlab.yodaqa.provider.rdf.DBpediaTypes
	groovy:000> dbt = DBpediaTypes.newInstance();
	===> cz.brmlab.yodaqa.provider.rdf.DBpediaTypes@499e542d
	groovy:000> dbt.query("Albert Einstein", null);
	===> [Natural Person, Writer, Philosopher, Academician, th-century American People, th-century German People, th-century Swiss People, th-century Swiss People, Alumnus, Laureate, Academics Of Charles University In Prague, Citizen, Emigrant, Inventor, Agnostic, Colleague, Pacifist, American Humanitarians, Humanitarian, American Inventors, American Pacifists, American People Of German-Jewish Descent, American People Of Swiss-Jewish Descent, American Physicists, Cosmologist, Cosmologists, Deist, Deists, Displaced Person, ETHZurich Alumni, Examiner, Fellows Of The Leopoldina, German Emigrants To Switzerland, German Humanitarians, German Inventors, German Nobel Laureates, German Pacifists, German Philosophers, German Physicists, Jewish Agnostics, Jewish American Scientists, Jewish American Writers, Jewish Inventors, Jewish Pacifists, Jewish Philosophers, Jewish Physicists, Naturalized Citizens Of The United States, Nobel Laureates In Physics, Patent Examiners, People Associated With The University Of Zurich, rttemberg, People From Ulm, People In AFirst-cousin Relationship, Stateless Persons, Swiss Emigrants To The United States, Swiss Humanitarians, Swiss Inventors, Swiss Nobel Laureates, Swiss Pacifists, Swiss Philosophers, Swiss Physicists, Theoretical Physicists]
