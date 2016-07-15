package cz.brmlab.yodaqa.provider.rdf;

/**
 * Created by honza on 14.7.16.
 */
public class WikidataLookup extends CachedJenaLookup {
		public WikidataLookup() {
		super("https://query.wikidata.org/sparql",
				"PREFIX wikibase: <http://wikiba.se/ontology#>" +
				"PREFIX bd: <http://www.bigdata.com/rdf#>");
	}
}
