YodaQA
======

YodaQA is an open source Factoid Question Answering system that can
produce answer both from databases and text corpora using on-the-fly
information extraction.
By default, open domain question answering is performed on top of
the Freebase and DBpedia knowledge bases as well as the texts of
enwiki articles.

YodaQA goals are practicality and extensible design, though it serves
as a research project as well.
Right now, we are still in early alpha regarding accuracy as well
as speed; in the future, we hope to also add some deductive capabilities
and include "personal assistant" style conversation capabilities.

YodaQA stands for "Yet anOther Deep Answering pipeline" and the system is
built on top of the Apache UIMA and DKpro UIMA bindings and developed as
part of the Brmson platform.
The QA logic is mostly original work, but much of the designs and componets
are inspired by the DeepQA (IBM Watson) and state-of-art papers.
See the Acknowledgements section of LICENCE.md for more.

The current version is a work-in-progress snapshot that already can answer
some questions, even though it's embarrassingly often wrong; on our reference
test set of questions, it can currently choose the correct answer for about 33%
of questions (but 46% of questions have the correct answer in top three).
Detailed performance info is available at:

	https://github.com/brmson/yodaqa/wiki/Benchmarks

More details on YodaQA plus links to some papers are available at:

	http://ailao.eu/yodaqa/

and you can play with a live demo at

	http://live.ailao.eu/

(this demo corresponds to the ``d/live`` branch of this git repo).

Also check out our movies QA demo at the ``d/movies`` branch and
http://movies.ailao.eu/ !  (This is actually our primary testbed right now;
it answers questions only using databases.)

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
``-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa=debug`` to gradle;
this is **highly recommended**!

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

By default, YodaQA uses preconfigured data sources running on the authors'
infrastructure that supply open domain information.  Detailed documentation
on setup of these open domain data sources is available below.

It is certainly possible to adapt YodaQA for a particular domain and use
custom data sources, but this process is not documented in detail yet.
Please contact ailao@ailao.eu for support and guidance if you are interested
and need help.

### Fulltext Data Source

YodaQA's original primary answer source involves information extraction
from free text organized into topical articles (like Wikipedia).
YodaQA uses Solr fulltext indexing framework as a data source.
By default, it will try to connect to the author's computer,
but the Solr Wikipedia instance there may not be always running.

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

### Database Data Source

The current development focus of YodaQA is on producing answers based
on database queries - we are talking about knowledge graph RDF databases.
We use SPARQL queries and code tailored for two databases, DBpedia and
Freebase; in principle, instantiating another database wouldn't be hard.

Regarding DBpedia, we share the backend code with the Ontology Data Source
below.

Regarding Freebase, we use its RDF export with SPARQL endpoint,
running on infrastructure
provided by the author's academic group (Jan Šedivý's 3C Group at the
Dept. of Cybernetics, FEE CTU Prague).  If the endpoint is not available
for some reason, you can also disable Freebase usage by editing the
method getConceptProperties() (instructions inside) of:

	src/main/java/cz/brmlab/yodaqa/pipeline/structured/FreebaseOntologyPrimarySearch.java

You can start your own instance by following the instructions in
``data/freebase/README.md`` but it is quite arduous and resource intensive.

### Ontology Data Source

YodaQA benefits from knowing metadata about the concepts in question as well
as in answers.  This means information about concept names and aliases (like
Wikipedia article names and redirects), and information about concept types
(like Wikipedia article categories; that Prague is a city, Václav Havel is
a president and a writer, etc.).

For open domain question answering, we use DBpedia as the data source
(as well as specialized concept label lookup services for question processing).
We have special DBpedia-specific code, but again it would be easy to adapt
it to other RDF data sources by just tweaking the respective SPARQL queries.

By default, we rely on a DBpedia-2014 SPARQL endpoint running on the author's
computer.  In case it is offline, you can try to switch it to the public
DBpedia SPARQL endpoint, though it is prone to outages and we shouldn't use
it too heavily anyway, or you can fairly easily set up a local instance of
DBpedia.  Detailed instrutions can be found in ``data/dbpedia/README.md``.

As a further example, were you doing biomedical QA, you could add a GeneOntology
ontology data source in addition to DBpedia to improve accuracy.  We actually
did just that in the d/clef15-bioasq-crfansx-go branch.

## Development Notes

See the [High Level Design Notes](doc/HIGHLEVEL.md) document for
a brief description of YodaQA's design approach.  When hacking brmson
QA logic, you should understand basics of the UIMA framework we use,
see the [UIMA Intro](doc/UIMA-INTRO.md).  You will probably want to
switch back and forth between these two documents when learning about
YodaQA first.

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
(passages, answers) to pick those which deserve further consideration,
as well as for other purposes like concept linking and selection of
database relations.

Models should be re-trained every time a non-trivial change in the
pipeline is made.  For details on managing this, please refer to
``data/ml/README.md``.

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
