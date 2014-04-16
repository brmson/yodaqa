YodaQA
======

YodaQA is a Question Answering system built on top of the Apache UIMA
framework.  It stands for "Yet anOther Deep Answering pipeline" and it
is inspired by the DeepQA (IBM Watson) papers and the OpenQA framework.
Its goals are practicality, clean design and maximum simplicity - we
believe that other nice features like usability for research will flow
naturally from this.

YodaQA is developed as part of the Brmson platform; many of its components
are or will be based on the work of the good scientists at CMU (bits of
OpenQA, the Ephyra project).  We follow a significantly different (more
flexible, we believe) architecture compared to OpenQA, though.  For all
the NLP logic (including the NLP type system), we lean heavily on the DKPro
UIMA bindings.

The current version is a work-in-progress snapshot that does nothing
useful yet.

## Installation Instructions

Quick instructions for setting up, building and running (focused on Debian Wheezy):
  * We assume that you cloned YodaQA and are now in the directory that contains this README.
  * ``sudo apt-get install default-jdk maven uima-utils``
  * ``mvn verify``
  * ``mvn -q exec:java``
By default, YodaQA will try to connect to a remote Solr core serving Wikipedia; see the section on Data Sources if connection fails.

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

	wget https://github.com/downloads/oaqa/helloqa/guten.tar.gz; mkdir -p data; tar -C data -xf guten.tar.gz

Then, you will need to modify the PrimarySearch portion of file

	src/main/java/cz/brmlab/yodaqa/YodaQAApp.java

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
  * Download and unpack http://pasky.or.cz/dev/brmson/solr-enwiki.zip Solr enwiki
    import configuration.
  * Download enwiki dump from http://dumps.wikimedia.org/enwiki/ (you want the
    ``enwiki-*-pages-articles.xml.bz2`` file), store in the ``enwiki/`` subdirectory.
    (Its size is many gigabytes!)
  * ``bunzip2 enwiki/enwiki*.bz2`` (This will take about 40-50 GiB of space and you
    can go get some coffee.)
  * Fix the enwiki XML file reference in ``enwiki/collection1/conf/data-import.xml``
  * Start standalone Solr server: ``java -Dsolr.solr.home=enwiki -jar start.jar``
  * In your web browser, open http://localhost:8983/solr/ - you should see a fancy page.
  * In your web browser, open http://localhost:8983/solr/dataimport?command=full-import
    (this will take 1-2 hours on a moderately fast machine and consume another few tens
    of gigabytes).

You may want to edit the URL in ``src/main/java/cz/brmlab/yodaqa/YodaQAApp.java``.


## Design Considerations

See the [High Level Design Notes](doc/HIGHLEVEL.md) document for
a brief description of YodaQA's design approach.

### Package Organization

We live in the cz.brmlab.yodaqa namespace. The rest is T.B.D. yet.
