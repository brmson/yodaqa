package cz.brmlab.yodaqa.provider.rdf;

/** This is an abstract base class for accessing DBpedia. */

public abstract class DBpediaLookup extends CachedJenaLookup {
	public DBpediaLookup() {
		/* Replace the first URL below with http://dbpedia.org/sparql
		 * to use the public DBpedia SPARQL endpoint. */
		super("http://dbpedia.ailao.eu:3030/dbpedia/query",
			"PREFIX : <http://dbpedia.org/resource/>\n" +
			"PREFIX dbpedia2: <http://dbpedia.org/property/>\n" +
			"PREFIX dbpedia: <http://dbpedia.org/>\n" +
			"PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
			"");
	}
}
