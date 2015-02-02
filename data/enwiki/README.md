enwiki Solr Home Directory
==========================

This is a solr home directory that contains the configuration necessary
for indexing a full XML dump of the English Wikipedia with Solr.

It is based on the receipe of

   <http://wiki.apache.org/solr/DataImportHandler#Example:_Indexing_wikipedia>

In addition to the original recipe, we attempt to avoid redirects
more thoroughly and also evade disambiguation pages, etc.

Setup Instructions
------------------

We expect this enwiki directory to live in the ``example/`` subdirectory
of a normal Solr distribution - copy it over. All the instructions steps
assume we are in the enwiki directory over there.

First, we get the latest enwiki dump and remove evil things like Mediawiki
markup, tables, links, categories or redirects, generating a much simpler
and somewhat smaller dump.  Then, we import this into Solr.

### Plaintext Wikipedia

  * Download the [enwiki dump](http://dumps.wikimedia.org/enwiki/)
    (you want the ``enwiki-*-pages-articles.xml.bz2`` file),
    store in this directory.  (Its size is many gigabytes!)
    As a reference version, we currently use the 20150112 dump; if you
    want to reproduce our environment, it's long-time archived at:
    ``http://v.or.cz/~pasky/brmson/``
  * Get our version of WikiExtractor by ``git clone https://github.com/brmson/wikipedia-extractor``
  * Prepare plaintext dump directory by ``mkdir enwiki-text``
  * Run WikiExtractor on the dump like

        bzcat enwiki*.bz2 | wikipedia-extractor/WikiExtractor.py -o enwiki-text -H -c -x

    (This will take about 10 GiB of space and you can go get some coffee.
    A lot of coffee - this may run for about 5 hours?)
  * Create a single XML file by running ``bin/extracted2xml.sh enwiki-text.xml``
    (ideally adding the date of the dump to the filename).

### Solr Import

  * Revise the enwiki-text XML file reference in ``collection1/conf/data-config.xml``
    according to the dump date you used.
  * In the parent directory (``example/``), start the standalone Solr server:
    ``java -Dsolr.solr.home=enwiki -jar start.jar``
  * In your web browser, open <http://localhost:8983/solr/> - you should see a fancy page.
  * In your web browser, open <http://localhost:8983/solr/dataimport?command=full-import>
    (this will take 1-2 hours on a moderately fast machine and consume another few tens
    of gigabytes).

The standalone Solr server can now answer YodaQA's Solr queries.

Files In This Directory
-----------------------

Most files in this directory are just copied over from the default Solr example.
We just brought our own data-config.xml and customized solrconfig.xml (adding
the DataImportHandler) and schema.xml. In the future, we may want to customize
this way more.

Licence
-------

Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

  <http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
