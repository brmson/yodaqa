package cz.brmlab.yodaqa.provider.rdf;

import cz.brmlab.yodaqa.provider.url.UrlConstants;
import cz.brmlab.yodaqa.provider.url.UrlManager;

/** This is an abstract base class for accessing DBpedia. */

public abstract class DBpediaLookup extends CachedJenaLookup {
	public DBpediaLookup() {
		/* Replace the first URL below with http://dbpedia.org/sparql
		 * to use the public DBpedia SPARQL endpoint. */
		super(UrlManager.getInstance().getUrl(UrlConstants.DBPEDIA),
			"PREFIX : <http://dbpedia.org/resource/>\n" +
			"PREFIX dbpedia2: <http://dbpedia.org/property/>\n" +
			"PREFIX dbpedia: <http://dbpedia.org/>\n" +
			"PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
			"");
	}
}
