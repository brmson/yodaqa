package cz.brmlab.yodaqa.provider.rdf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Literal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;

/** A wrapper around DBpedia dataset that maps concepts to
 * dbpedia2:wordnet_type ontology pieces. This gives some evidence
 * of the type of the concept that we directly match with other
 * WordNet data.
 *
 * XXX: The WordNet types here also disambiguate word senses.
 * We drop this information for now, instead of converting this
 * to synset numbers. */

public class DBpediaWNTypes extends CachedJenaLookup {
	private static final Log logger =
		LogFactory.getLog(DBpediaWNTypes.class);

	/** Query for a given title, returning a set of types. */
	public List<String> query(String title, Logger logger) {
		for (String titleForm : cookedTitles(title)) {
			List<String> results = queryTitleForm(titleForm, logger);
			if (!results.isEmpty())
				return results;
		}
		return new ArrayList<String>();
	}

	/** Query for a given specific title form, returning a set
	 * of types. */
	public List<String> queryTitleForm(String title, Logger logger) {
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
			 // set the output variable
			"?res dbpedia2:wordnet_type ?type .\n" +

			 // weed out resources that are categories and other in-namespace junk
			"FILTER ( !regex(str(?res), '^http://dbpedia.org/resource/[^_]*:', 'i') )\n" +
			"";
		//logger.debug("executing sparql query: {}", rawQueryStr);
		List<Literal[]> rawResults = rawQuery(rawQueryStr,
			new String[] { "type" });

		List<String> results = new LinkedList<String>();
		for (Literal[] rawResult : rawResults) {
			String typeLabel = cookTypeLabel(rawResult[0].getString());

			// logger.debug("DBpedia {} wntype: [[{}]]", title, typeLabel);
			results.add(typeLabel);
		}

		return results;
	}

	protected static String cookTypeLabel(String typeLabel) {
		typeLabel = typeLabel.replaceAll("^synset-([^-]*)-.*$", "$1").replaceAll("_", " ");
		return typeLabel;
	}
}
