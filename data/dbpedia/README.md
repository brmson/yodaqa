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

	mkdir 2014
	cd 2014
	wget http://downloads.dbpedia.org/2014/dbpedia_2014.owl.bz2
	wget http://downloads.dbpedia.org/2014/en/labels_en.nt.bz2
	wget http://downloads.dbpedia.org/2014/en/redirects_transitive_en.nt.bz2
	wget http://downloads.dbpedia.org/2014/en/page_ids_en.nt.bz2
	wget http://downloads.dbpedia.org/2014/en/instance_types_en.nt.bz2
	wget http://downloads.dbpedia.org/2014/en/instance_types_heuristic_en.nt.bz2
	wget http://downloads.dbpedia.org/2014/links/wordnet_links.nt.bz2
	wget http://downloads.dbpedia.org/2014/links/yago_types.nt.bz2
	wget http://downloads.dbpedia.org/2014/links/yago_taxonomy.nt.bz2
	wget http://downloads.dbpedia.org/2014/en/mappingbased_properties_cleaned_en.nt.bz2
	wget http://downloads.dbpedia.org/2014/en/specific_mappingbased_properties_en.nt.bz2
	wget http://downloads.dbpedia.org/2014/en/infobox_properties_en.nt.bz2
	wget http://downloads.dbpedia.org/2014/en/infobox_property_definitions_en.nt.bz2
	wget http://downloads.dbpedia.org/2014/en/disambiguations_en.nt.bz2
	bunzip2 -k *.bz2
	cd ..

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
	../apache-jena-2.12.1/bin/tdbloader2 --loc db ../2014/dbpedia_2014.owl ../2014/*.nt

To start the Fuseki server, run then (in jena-fuseki-1.1.1)

	./fuseki-server --loc db /dbpedia

and edit ``src/main/java/cz/brmlab/yodaqa/provider/rdf/DBpediaLookup.java``
changing default value of the ``service`` attribute.  It should work.
