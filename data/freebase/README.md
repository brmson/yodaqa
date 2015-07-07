Freebase SPARQL Endpoint
========================

Freebase is a curated database of human knowledge, which should have
higher quality data and, more importantly, much wider subjcet coverage
than DBpedia.  At the time of writing this, it is supposed to be in the
process of being phased out by Google in favor of Wikidata, but at this
point the data still seems to be vastly superior in Freebase in many
areas - e.g. astronomical objects or chemical elements.

Unfortunately, we know of no well-defined public Freebase SPARQL endpoint,
so we have to run our own:

	http://freebase.ailao.eu:3030/freebase/query

To set up your own, you need to first download the Freebase NT data file
and set up a RDF database on top of it.  Freebase is a bit on the bigger
side regarding size, you should be able to download 100GB file, have
a lot of memory and about 1.5T free space for the import procedure.

Download Data Files
===================

	http://commondatastorage.googleapis.com/freebase-public/rdf/freebase-rdf-latest.gz
	gunzip freebase-rdf-latest.gz

Note that unpacked, this is a 355GB file!  Unpacking it will take a while.

Set Up RDF Database
===================

We will use Apache Jena Fuseki as RDF database and SPARQL endpoint.
From http://jena.apache.org/download/index.cgi, download the Jena distribution
tar.gz (2.12.1 at the time of writing) and Jena Fuseki distribution tar.gz
(1.1.1 at the time of writing).

	tar xvvfz apache-jena-2.12.1.tar.gz
	tar xvvfz jena-fuseki-1.1.1-distribution.tar.gz
	cd jena-fuseki-1.1.1
	mkdir d-freebase
	../apache-jena-2.12.1/bin/tdbloader2 --loc d-freebase/ freebase-rdf-latest

Note that this has quite massive requirements.  We ran this with 20GB RAM
and I'm not sure if it'll work with less.  While the *final* space usage
of the database is currently 238GB, the temporary peak space usage during
import may be near 500GB.  Moreover, you will want to have about 500GB
free space on your /tmp partition!  Be sure to arrange your filesystems
appropriately.

If you run out of space during the import, you can edit the tdbloader2
shell script to resume from a point where you still have complete data,
to use a different directory than /tmp for some of its temporary data,
etc.  The import may run for a day or three.

You may need to edit the ``fuseki-server`` script to increase allowed Java
heap space - in the line

	JVM_ARGS=${JVM_ARGS:--Xmx1200M}

replace 1200 with 6400 or some such.

To start the Fuseki server, run then (in jena-fuseki-1.1.1)

	./fuseki-server --loc d-freebase /freebase

and edit ``src/main/java/cz/brmlab/yodaqa/provider/rdf/FreebaseLookup.java``
changing default value of the ``service`` attribute.  It should work.
