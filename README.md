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
some questions, even though it's embarassingly often wrong; on our training
corpus, while 67% of questions have the correct answer *suggested* in the
process, it can currently choose the correct answer for about 14.5% of
questions (but 27.5% of questions have the correct answer in top three and
34.5% in top five).

## Installation Instructions

Quick instructions for setting up, building and running (focused on Debian Wheezy):
  * We assume that you cloned YodaQA and are now in the directory that contains this README.
  * ``sudo apt-get install default-jdk maven uima-utils``
  * Install the Wordnet ontological database:
	``cd data/wordnet; wget http://wordnetcode.princeton.edu/wn3.1.dict.tar.gz; tar xf wn*tar.gz; cd ../..``
  * ``mvn verify``
  * ``mvn -q exec:java -Pinteractive``
By default, YodaQA will try to connect to a remote Solr core serving Wikipedia; see the section on Data Sources if connection fails.

## Usage

The ``mvn -q exec:java -Pinteractive`` starts YodaQA with the "interactive"
frontend which offers a prompt and answers questions interactively;
answer candidates and their confidence score are listed after a while
(the first question takes a bit longer to answer as the models etc. are
loaded).

It is also possible to let YodaQA answer many questions at once, e.g. to
measure the performance; use ``mvn -q exec:java -Ptrecnew`` to feed YodaQA
the TREC dataset from data/trec/.

By default, there is a lot of output regarding progress of the answering
process; redirect stderr, e.g. ``2>/dev/null``, to get rid of that.
Alternatively, if things don't go well, try passing an extra parameter
``-Dorg.slf4j.simpleLogger.defaultLogLevel=debug`` on the mvn commandline,
or specifically ``-Dorg.slf4j.simpleLogger.log.cz.brmlab.yodaqa=debug``.

Sometimes, Java may find itself short on memory; don't try to run YodaQA
on systems with less than 8GB RAM.  Anyhow, you may need to invoke it as
``MAVEN_OPTS="-Xms2048m -Xmx4500m" mvn -q exec:java ...``.  Another issue
that can arise is that if you are running measurements in parallel, one
of the java processes decides to spawn bazillion threads to perform
garbage collection; to prevent that, include ``-XX:-UseParallelGC
-XX:-UseConcMarkSweepGC`` in ``MAVEN_OPTS``.

## Data Sources

YodaQA uses Solr fulltext indexing framework as a data source, either
internally or externally.  By default, it will try to connect to the
author's computer, but the Solr Wikipedia instance there may not be
always running.

### Local Corpus

You may want to instead run YodaQA on a local corpus, either your custom
data or e.g. Project Gutenberg fulltext index.

The performance on the (default) Project Gutenberg corpus is actually not
that good.  But try asking e.g. about "Who was the sixteenth President
of the United States?"

To set it up, first download the index:

	wget https://github.com/downloads/oaqa/helloqa/guten.tar.gz; tar -C data -xf guten.tar.gz

Then, you will need to modify the SolrNamedSource portion of file

	src/main/java/cz/brmlab/yodaqa/pipeline/YodaQA.java

(follow the instructions in comments - it just involves commenting
out a piece of code and uncommenting another). Rerun ``mvn verify``.

### Wikipedia Data Source

The remote instance configured by default provides English Wikipedia as a data
source.  It is not too difficult to set this up on your own, but it is very
memory and IO intensive process. You will need about 80-100GiB of disk space and
bandwidth to download 10GiB source file; indexing will require roughly 8GiB RAM.

To index and then search in Wikipedia, we need to set it up as a standalone Solr
source:

  * Download solr (http://www.apache.org/dyn/closer.cgi/lucene/solr/),
    unpack and cd to the ``example/`` subdirectory.
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

By default, we rely on the public DBpedia SPARQL endpoint, but it is prone
to outages and we shouldn't use it too heavily anyway.  Instrutions for
setup of local DBpedia SPARQL endpoint are work-in-progress in
``data/dbpedia/README.md``.


## Design Considerations

See the [High Level Design Notes](doc/HIGHLEVEL.md) document for
a brief description of YodaQA's design approach.

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

We also carry some sources in the de.tudarmstadt.ukp.dkpro namespace
and types in the desc namespace; these are just tweaked copies of some
DKPro upstream classes that contain bugfixes we require - we expect to
get rid of them with the next upstream releases.
