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
 * rdf:type ontology pieces. The main point of using this is to give
 * an evidence of the type of the concept.
 *
 * We also weed out any abstractions, keeping only the leaf types. */

public class DBpediaTypes extends DBpediaLookup {
	private static final Log logger =
		LogFactory.getLog(DBpediaTypes.class);

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
		 * to be surprisingly tricky.  Cover 91% of all cases
		 * by capitalizing words that are not stopwords  */
		title = super.capitalizeTitle(title);

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
			"?res rdf:type ?type .\n" +

			 // gather information about supertypes so that we can
			 // remove them from the output set
			"OPTIONAL {\n" +
			"  ?type rdfs:subClassOf ?superType .\n" +
			"}\n" +

			 // keep only resources; e.g. /property/ objects also
			 // have types!
			"FILTER ( regex(str(?res), '^http://dbpedia.org/resource/', 'i') )\n" +
			 // weed out resources that are categories and other in-namespace junk
			"FILTER ( !regex(str(?res), '^http://dbpedia.org/resource/[^_]*:', 'i') )\n" +
			"";
		//logger.debug("executing sparql query: {}", rawQueryStr);
		List<Literal[]> rawResults = rawQuery(rawQueryStr,
			new String[] { "type", "superType" }, 0);

		/* Blacklist of abstractions (types that are supertypes
		 * of other types) */
		Set<String> abstractions = new HashSet<String>();

		List<String> results = new LinkedList<String>();
		for (Literal[] rawResult : rawResults) {
			String typeLabel = cookTypeLabel(rawResult[0].getString());
			if (typeLabel.equals("Thing") || typeLabel.equals(" Feature")) {
				// just skip this, about everything is tagged as owl#thing
				// and most stuff is tagger as a " Feature" (sic)
				continue;
			}

			// logger.debug("DBpedia {} type: [[{}]]", title, typeLabel);
			/* There are these other really common labels that
			 * just spam us and don't cary anything much useful. */
			// ??? what is q?
			if (typeLabel.equals("Q")
			    // "English Language" has gazillion of these
			    || typeLabel.startsWith("Languages Of ")
			    // countries are economies etc.
			    || typeLabel.equals("Economy"))
				continue;

			results.add(typeLabel);

			if (rawResult[1] != null) {
				/* XXX: Yago has some "sub-leaf" types like
				 * http://dbpedia.org/class/yago/OnlineEncyclopedias
				 * that shouldn't cause blacklisting of true
				 * concept leafs like
				 * http://dbpedia.org/class/yago/Encyclopedia106427387 */
				if (rawResult[1].getString().matches("^.*[0-9]{5}$")
				    && !rawResult[0].getString().matches("^.*[0-9]{5}$")) {
					// logger.debug("ignoring abstraction {}", rawResult[1].getString());
					continue;
				}
				String superTypeLabel = cookTypeLabel(rawResult[1].getString());
				// logger.debug("abstraction {}", superTypeLabel);
				abstractions.add(superTypeLabel);
			}
		}

		/* Revisit results and weed out blacklisted entries. */
		Iterator<String> it = results.iterator();
		while (it.hasNext()) {
			String typeLabel = it.next();
			// logger.debug("checking type {}", typeLabel);
			if (abstractions.contains(typeLabel)) {
				it.remove();
				// logger.debug("removing type {}", typeLabel);
			}
		}

		return results;
	}

	protected static String cookTypeLabel(String typeLabel) {
		typeLabel = typeLabel.
			replaceAll("_", " ").
			replaceAll("[0-9]*$", "").
			replaceAll("([a-z])([A-Z])", "$1 $2");
		return typeLabel;
	}
}
