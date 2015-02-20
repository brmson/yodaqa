package cz.brmlab.yodaqa.provider.rdf;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Literal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;

/** A wrapper around DBpedia dataset that maps concepts to curated
 * infobox extracted entity properties. This can then serve as an
 * information source.
 *
 * This is cleaner dataset than DBpediaProperties; see
 * DBpediaRelationPrimarySearch for detailed discussion. */

public class DBpediaOntology extends DBpediaLookup {
	private static final Log logger =
		LogFactory.getLog(DBpediaOntology.class);

	/** An (object, property, value) tuple, where object is its
	 * English string label, property is an English phrase
	 * describing the value ("population", "area total",
	 * "country", "known for", ...) and value is a string with
	 * some entity - name, quantity, ... */
	public class PropertyValue {
		protected String object;
		protected String property;
		protected String value;

		PropertyValue(String object_, String property_, String value_) {
			object = object_;
			property = property_;
			value = value_;
		}

		public String getObject() { return object; }
		public String getProperty() { return property; }
		public String getValue() { return value; }
	}

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
		 * to be surprisingly tricky.  Cover 90% of all cases
		 * by force-capitalizing the first letter in the sought
		 * after title. */
		title = Character.toUpperCase(title.charAt(0)) + title.substring(1);

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
			new String[] { "property", "value" });

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
			logger.debug("DBpedia {} property: {} -> {}", title, propLabel, value);
			results.add(new PropertyValue(title, propLabel, value));
		}

		return results;
	}
}
