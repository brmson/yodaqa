package cz.brmlab.yodaqa.provider.rdf;

import com.hp.hpl.jena.rdf.model.Literal;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
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
		title = super.capitalizeTitle(title);

		String quotedTitle = title.replaceAll("\"", "").replaceAll("\\\\", "").replaceAll("\n", " ");
		String rawQueryStr =
			"?res rdfs:label \"" + quotedTitle + "\"@cs .\n" +
			"{	?res ?propres ?valres .	}\n" +
			"UNION\n" +
			"{	?valres ?propres ?res .	}\n" +
			"?prop wikibase:directClaim ?propres .\n" +
			"		SERVICE wikibase:label {\n" +
			"	bd:serviceParam wikibase:language \"cs\"\n" +
			"}" +
			"";
//		logger.debug("executing sparql query: {}", rawQueryStr);
		List<Literal[]> rawResults = rawQuery(rawQueryStr,
				new String[] { "propLabel", "valresLabel", "valres", "res" }, 0);

		List<PropertyValue> results = new ArrayList<PropertyValue>(rawResults.size());
		for (Literal[] rawResult : rawResults) {
			String propLabel = rawResult[0].getString().
					replaceAll(".*/", "").
					replaceAll("_", " ").
					replaceAll("([a-z])([A-Z])", "$1 $2");
			String value = rawResult[1].getString().replaceAll("\\s+\\([^)]*\\)\\s*$", "");
			String valRes = rawResult[2] != null ? rawResult[2].getString() : null;
			String objRes = rawResult[3].getString();
			logger.debug("Wikidata {} property: {} -> {} ({})", title, propLabel, value, valRes);
			AnswerFV fv = new AnswerFV();
			fv.setFeature(AF.OriginDBpOntology, 1.0);
			results.add(new PropertyValue(title, objRes, propLabel,
					value, valRes, null,
					fv, AnswerSourceStructured.ORIGIN_ONTOLOGY));
		}
		return results;
	}


}
