package cz.brmlab.yodaqa.provider.rdf;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Literal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;

/** A wrapper around DBpedia dataset that maps concepts to
 * rdf:type ontology pieces. The main point of using this is to give
 * an evidence of the type of the concept. */

public class DBpediaTypes extends CachedJenaLookup {
	private static final Log logger =
		LogFactory.getLog(DBpediaTypes.class);

	/** Query for a given title, returning a set of articles. */
	public List<String> query(String title, Logger logger) {
		/* XXX: Case-insensitive search via SPARQL turns out
		 * to be surprisingly tricky.  Cover 90% of all cases
		 * by force-capitalizing the first letter in the sought
		 * after title. */
		title = Character.toUpperCase(title.charAt(0)) + title.substring(1);

		title = title.replaceAll("\"", "").replaceAll("\\\\", "").replaceAll("\n", " ");
		String rawQueryStr =
			"{\n" +
			   // (A) fetch resources with @title label
			"  ?res rdfs:label \"" + title + "\"@en.\n" +
			"} UNION {\n" +
			   // (B) fetch also resources targetted by @title redirect
			"  ?redir dbo:wikiPageRedirects ?res .\n" +
			"  ?redir rdfs:label \"" + title + "\"@en .\n" +
			"}\n" +
			 // set the output variables
			"?res rdf:type ?t .\n" +

			 // weed out resources that are categories and other in-namespace junk
			"FILTER ( !regex(str(?res), '^http://dbpedia.org/resource/[^_]*:', 'i') )\n" +
			"";
		//logger.debug("executing sparql query: {}", rawQueryStr);
		List<Literal[]> rawResults = rawQuery(rawQueryStr,
			new String[] { "t" });

		List<String> results = new ArrayList<String>(rawResults.size());
		for (Literal[] rawResult : rawResults) {
			String typeLabel = rawResult[0].getString().
				replaceAll("_", " ").
				replaceAll("[0-9]*$", "").
				replaceAll("([a-z])([A-Z])", "$1 $2");
			if (typeLabel.equals("Thing")) {
				// just skip this, about everything is tagged as owl#thing
				continue;
			}
			//logger.debug("DBpedia {} type: [[{}]]", title, typeLabel);
			results.add(typeLabel);
		}

		return results;
	}
}
