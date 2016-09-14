package cz.brmlab.yodaqa.provider.rdf;

/**
 * Created by honza on 14.7.16.
 */
public class WikidataLookup extends CachedJenaLookup {
		public WikidataLookup() {
		super("https://query.wikidata.org/sparql",
				"PREFIX wikibase: <http://wikiba.se/ontology#>\n" +
				"PREFIX bd: <http://www.bigdata.com/rdf#>\n" +
				"PREFIX wd: <http://www.wikidata.org/entity/>\n" +
				"PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
				"PREFIX schema: <http://schema.org/>\n" +
				"PREFIX p: <http://www.wikidata.org/prop/>\n" +
				"PREFIX pq: <http://www.wikidata.org/prop/qualifier/>\n");
	}
}
