DBpedia SPARQL Endpoint
=======================

By default, we use the author's personal instance of DBpedia running
on his home computer.  So it follows that it may be offline time by
time.

Alternatively, we could (and had in the past) use the public SPARQL endpoint
at http://dbpedia.org/sparql for our DBpedia queries, however time by time
it would get stuck at HTTP 502 status even for hours.  Also, the results might
not be reproducible as DBpedia version available there varies over time.

To set up your own, you need to first download the set of DBpedia data files
and set up a RDF database on top of them.

Download Data Files
===================

	mkdir 2015-10-cs
	cd 2015-10-cs
	wget http://downloads.dbpedia.org/2015-10/dbpedia_2015-10.owl
	wget http://downloads.dbpedia.org/2015-10/core-i18n/cs/labels_cs.ttl.bz2
	wget http://downloads.dbpedia.org/2015-10/core-i18n/cs/transitive_redirects_cs.ttl.bz2
	wget http://downloads.dbpedia.org/2015-10/core-i18n/cs/redirects_cs.ttl.bz2
	wget http://downloads.dbpedia.org/2015-10/core-i18n/cs/page_ids_cs.ttl.bz2
	wget http://downloads.dbpedia.org/2015-10/core-i18n/cs/instance_types_cs.ttl.bz2
	wget http://downloads.dbpedia.org/2015-10/links/wordnet_links.nt.bz2
	wget http://downloads.dbpedia.org/2015-10/links/yago_types.nt.bz2
	wget http://downloads.dbpedia.org/2015-10/links/yago_taxonomy.nt.bz2
	wget http://downloads.dbpedia.org/2015-10/core-i18n/cs/mappingbased_literals_cs.ttl.bz2
	wget http://downloads.dbpedia.org/2015-10/core-i18n/cs/mappingbased_objects_cs.ttl.bz2
	wget http://downloads.dbpedia.org/2015-10/core-i18n/cs/specific_mappingbased_properties_cs.ttl.bz2
	wget http://downloads.dbpedia.org/2015-10/core-i18n/cs/infobox_properties_cs.ttl.bz2
	wget http://downloads.dbpedia.org/2015-10/core-i18n/cs/infobox_property_definitions_cs.ttl.bz2
	#wget http://downloads.dbpedia.org/2015-10/core-i18n/cs/disambiguations_cs.ttl.bz2
	wget http://downloads.dbpedia.org/2015-10/core-i18n/cs/short_abstracts_cs.ttl.bz2
	wget http://downloads.dbpedia.org/2015-10/core-i18n/cs/interlanguage_links_cs.ttl.bz2
	bunzip2 -k *.bz2
	cd ..

We also generated a list of Czech ontology relation labels based on the infobox
mapping rules, as ``data/dbpedia/Mapping_cs.xml.nt``; copy it over too!

Set Up RDF Database
===================

We will use Apache Jena Fuseki as RDF database and SPARQL endpoint.
From http://jena.apache.org/download/index.cgi, download the Jena distribution
tar.gz (2.12.1 at the time of writing) and Jena Fuseki distribution tar.gz
(1.1.1 at the time of writing).

	tar xvvfz apache-jena-2.12.1.tar.gz
	tar xvvfz jena-fuseki-1.1.1-distribution.tar.gz
	cd jena-fuseki-1.1.1
	mkdir db
	../apache-jena-2.12.1/bin/tdbloader2 --loc db ../2015-10-cs/*.owl ../2015-10-cs/*.ttl ../2015-10-cs/*.nt

To start the Fuseki server, run then (in jena-fuseki-1.1.1)

	./fuseki-server --port 3031 --loc db /dbpedia

and edit ``src/main/java/cz/brmlab/yodaqa/provider/rdf/DBpediaLookup.java``
changing default value of the ``service`` attribute.  It should work.

Fuzzy Label Lookup Service
==========================

An important use of DBpedia is to lookup question substrings that happen to
be enwiki article titles, i.e. some sort of concepts.  As a more flexible,
fuzzier lookup mechanism than plain SPARQL, we use

	https://github.com/brmson/label-lookup

which contains some details on how to set it up.  To make YodaQA use your
own instance, edit the appropriate http URL in the file
``src/main/java/cz/brmlab/yodaqa/provider/rdf/DBpediaTitles.java``.
