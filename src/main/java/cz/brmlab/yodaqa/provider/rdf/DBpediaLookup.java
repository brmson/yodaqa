package cz.brmlab.yodaqa.provider.rdf;

/** This is an abstract base class for accessing DBpedia. */

public abstract class DBpediaLookup extends CachedJenaLookup {
	public DBpediaLookup() {
		super("http://dbpedia.ailao.eu:3031/dbpedia/query",
			"PREFIX : <http://cs.dbpedia.org/resource/>\n" +
			"PREFIX dbpedia2: <http://cs.dbpedia.org/property/>\n" +
			"PREFIX dbpedia: <http://cs.dbpedia.org/>\n" +
			"PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
			"");
	}
}
