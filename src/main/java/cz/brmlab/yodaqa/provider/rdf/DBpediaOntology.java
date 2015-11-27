package cz.brmlab.yodaqa.provider.rdf;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Literal;

import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceStructured;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;

/** A wrapper around DBpedia dataset that maps concepts to curated
 * infobox extracted entity properties. This can then serve as an
 * information source.
 *
 * This is cleaner dataset than DBpediaProperties. */

public class DBpediaOntology extends DBpediaLookup {
	private static final Log logger =
		LogFactory.getLog(DBpediaOntology.class);

	/** Query for a given title, returning a set of PropertyValue instances. */
	public List<PropertyValue> query(String title, Logger logger) {
		for (String titleForm : cookedTitles(title)) {
			List<PropertyValue> results = queryTitleForm(titleForm, logger);
			if (!results.isEmpty())
				return results;
		}
		return new ArrayList<PropertyValue>();
	}

	/** Query for a given specific title form, returning a set of
	 * PropertyValue instances. */
	public List<PropertyValue> queryTitleForm(String title, Logger logger) {
		/* XXX: Case-insensitive search via SPARQL turns out
		 * to be surprisingly tricky.  Cover 91% of all cases
		 * by capitalizing words that are not stopwords  */
		title = super.capitalizeTitle(title);

		String quotedTitle = title.replaceAll("\"", "").replaceAll("\\\\", "").replaceAll("\n", " ");
		/* If you want to paste this to e.g.
		 * 	http://dbpedia.org/snorql/
		 * just pass the block below through
		 * 	echo 'SELECT ?property ?value WHERE {'; perl -pe 'undef $_ unless /"/; s/\s*"//; s/\\n" \+$//;'; echo '}'
		 */
		String rawQueryStr =
			"{\n" +
			   // (A) fetch resources with @title label
			"  ?res rdfs:label \"" + quotedTitle + "\"@en.\n" +
			"} UNION {\n" +
			   // (B) fetch also resources targetted by @title redirect
			"  ?redir dbo:wikiPageRedirects ?res .\n" +
			"  ?redir rdfs:label \"" + quotedTitle + "\"@en .\n" +
			"}\n" +
			 // set the output variables
			"?res ?property ?valres .\n" +
			 // ?val might be a resource - in that case, convert it to a label
			"OPTIONAL { ?valres rdfs:label ?vlabel . }\n" +
			"BIND ( IF(BOUND(?vlabel), ?vlabel, ?valres) AS ?value )\n" +

			 // weed out resources that are categories and other in-namespace junk
			"FILTER ( !regex(str(?res), '^http://dbpedia.org/resource/[^_]*:', 'i') )\n" +
			 // select only relevant properties
			"FILTER ( regex(str(?property), '^http://dbpedia.org/ontology', 'i') )\n" +
			"FILTER ( !regex(str(?property), '^http://dbpedia.org/ontology/(wiki|abstract)', 'i') )\n" +
			"";
		// logger.debug("executing sparql query: {}", rawQueryStr);
		List<Literal[]> rawResults = rawQuery(rawQueryStr,
			new String[] { "property", "value", "/valres", "/res" }, 0);

		List<PropertyValue> results = new ArrayList<PropertyValue>(rawResults.size());
		for (Literal[] rawResult : rawResults) {
			/* Convert
			 * http://dbpedia.org/ontology/PopulatedPlace/populationDensity
			 * to "population density" and such. */
			String propLabel = rawResult[0].getString().
				replaceAll(".*/", "").
				replaceAll("_", " ").
				replaceAll("([a-z])([A-Z])", "$1 $2");
			/* Remove trailing (...) (e.g. (disambiguation) in
			 * links). */
			String value = rawResult[1].getString().replaceAll("\\s+\\([^)]*\\)\\s*$", "");
			String valRes = rawResult[2] != null ? rawResult[2].getString() : null;
			String objRes = rawResult[3].getString();
			logger.debug("DBpedia {} property: {} -> {} ({})", title, propLabel, value, valRes);
			AnswerFV fv = new AnswerFV();
			fv.setFeature(AF.OriginDBpOntology, 1.0);
			results.add(new PropertyValue(title, objRes, propLabel,
						value, valRes, null,
						fv, AnswerSourceStructured.ORIGIN_ONTOLOGY));
		}

		return results;
	}
}
