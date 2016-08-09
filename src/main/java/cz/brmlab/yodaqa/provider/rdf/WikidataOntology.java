package cz.brmlab.yodaqa.provider.rdf;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.sparql.util.StringUtils;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.rdf.FBPathLogistic.PathScore;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceStructured;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class WikidataOntology extends WikidataLookup {
	public List<PropertyValue> query(String title, Logger logger) {
		for (String titleForm : cookedTitles(title)) {
			List<PropertyValue> results = queryTitleForm(titleForm, logger);
			if (!results.isEmpty())
				return results;
		}
		return new ArrayList<>();
	}

	private List<PropertyValue> queryTitleForm(String title, Logger logger) {
//		title = super.capitalizeTitle(title);
		title = title.trim();

		String quotedTitle = title.replaceAll("\"", "").replaceAll("\\\\", "").replaceAll("\n", " ");
		String rawQueryStr =
			"?res rdfs:label \"" + quotedTitle + "\"@cs .\n" +
			"{	?res ?propres ?val .	}\n" +
			// XXX: This direction of relationship could produce a very large result set or lead to a query timeout
			// Example: quotedTitle = Spojené království - 355424 Results in 17140 ms on web SPARQL client
			// timeout in YodaQA
			//"UNION\n" +
			//"{	?valres ?propres ?res .	}\n" +
			"?prop wikibase:directClaim ?propres .\n" +
			"SERVICE wikibase:label {\n" +
			"	bd:serviceParam wikibase:language \"cs\"\n" +
			"}" +
			"?val rdfs:label ?vallabel\n" +
//			"BIND( IF(BOUND(?val), ?val, ?vallabel) AS ?val )\n" +
			"FILTER( LANG(?vallabel) = \"\" || LANGMATCHES(LANG(?vallabel), \"en\") )\n" +
			"";
		logger.debug("executing sparql query: {}", rawQueryStr);
		List<Literal[]> rawResults = rawQuery(rawQueryStr,
				new String[] { "propLabel", "vallabel", "val", "res" }, 0);
		return processResults(rawResults, title, logger);
	}


	public List<PropertyValue> queryFromLabel(PathScore ps, String title, Logger logger) {
		String quotedTitle = title.replaceAll("\"", "").replaceAll("\\\\", "").replaceAll("\n", " ");
		String rawQueryStr =
		"?res rdfs:label \"" + quotedTitle + "\"@cs .\n" +
		makeQuery(ps) +
		"BIND(" + getProperty(ps.path.get(0)) + " AS ?propres)\n" +
//		"OPTIONAL {\n" +
//		"  ?valres rdfs:label ?vallabel .\n" +
//		"  FILTER(LANGMATCHES(LANG(?vallabel), \"cs\"))\n" + // TODO not every vallabel needs to be in czech
//		"}\n" +
//		"BIND( IF(BOUND(?vallabel), ?vallabel, ?valres) AS ?value )\n" +
		"?prop wikibase:directClaim ?propres .\n" +
		"SERVICE wikibase:label {\n" +
		"	bd:serviceParam wikibase:language \"cs\"\n" +
		"}" +
		"";
		logger.debug("executing sparql query: {}", rawQueryStr);
		List<Literal[]> rawResults = rawQuery(rawQueryStr,
				new String[] { "propLabel", "valresLabel", "valres", "res" }, 0);
		return processResults(rawResults, title, logger);
	}

	private List<PropertyValue> processResults(List<Literal[]> rawResults, String title, Logger logger) {
		List<PropertyValue> results = new ArrayList<PropertyValue>(rawResults.size());
		for (Literal[] rawResult : rawResults) {
			String propLabel = rawResult[0].getString().
					replaceAll(".*/", "").
					replaceAll("_", " ").
					replaceAll("([a-z])([A-Z])", "$1 $2");
			String value = rawResult[1].getString().replaceAll("\\s+\\([^)]*\\)\\s*$", "");
			String valRes = rawResult[2] != null ? rawResult[2].getString() : null;
			String objRes = rawResult[3].getString();
			if (value != null && value.length() > 0 && value.charAt(0) == 'Q' && value.equals(valRes))
				continue; //No czech label
			logger.debug("Wikidata {} property: {} -> {} ({})", title, propLabel, value, valRes);
			AnswerFV fv = new AnswerFV();
			fv.setFeature(AF.OriginFreebaseOntology, 1.0);
			results.add(new PropertyValue(title, objRes, propLabel,
					value, valRes, null,
					fv, AnswerSourceStructured.ORIGIN_ONTOLOGY));
		}
		return results;
	}

	private String getProperty(String s) {
		try {
			return s.split(" ")[1];
		} catch (IndexOutOfBoundsException e) {
			return "";
		}
	}

	private String makeQuery(PathScore ps) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ps.path.size(); i++) {
			sb.append(ps.path.get(i)).append(" . \n");
		}
		return sb.toString();
	}
}
