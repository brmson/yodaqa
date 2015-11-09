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

	http://freebase.ailao.eu:8890/sparql

Furthermore, this endpoint contains BaseKB ("Gold Ultimate" version),
not plain Freebase.  We use a Virtuoso instance with the database of
AWS instance of BaseKB.  (More details to come.)


When rolling your own, edit
``src/main/java/cz/brmlab/yodaqa/provider/rdf/FreebaseLookup.java``
changing default value of the ``service`` attribute.  It should work.
