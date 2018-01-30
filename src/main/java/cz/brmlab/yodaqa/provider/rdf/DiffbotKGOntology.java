package cz.brmlab.yodaqa.provider.rdf;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.rdf.FBPathLogistic;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceStructured;
import cz.brmlab.yodaqa.provider.UrlManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class DiffbotKGOntology {
	private static final String ENDPOINT_URL = UrlManager.lookUpUrl(UrlManager.DataBackends.DIFFBOT_KG.ordinal());

	public List<PropertyValue> queryFromLabel(FBPathLogistic.PathScore ps, String id, String title, Logger logger) throws IOException {
		String encodedQuery = URLEncoder.encode("id:" + id, "UTF-8");
		String token = System.getProperty("cz.brmlab.yodaqa.provider.rdf.DiffbotToken");
		if (token == null) {
			// XXX File with token could be safer? (Token could be save in bash history this way)
			logger.error("No Diffbot token specified. Please use cz.brmlab.yodaqa.provider.rdf.DiffbotToken system property.");
			return new ArrayList<>();
		}
		String requestURL = ENDPOINT_URL + "?type=query&token=" + token + "&query=" + encodedQuery;
		URL request = new URL(requestURL);
		URLConnection connection = request.openConnection();
		List<PropertyValue> results = new ArrayList<>();

		logger.debug("Request URL: {}", requestURL);
		JsonReader jr = new JsonReader(new InputStreamReader(connection.getInputStream()));
		JsonElement je = new JsonParser().parse(jr);
		je = je.getAsJsonObject().get("data").getAsJsonArray().get(0).getAsJsonObject();
		for (int i = 0; i < ps.path.size(); i++) {
			if (je instanceof JsonObject) {
				je = ((JsonObject) je).get(ps.path.get(i));
			} else if (je instanceof JsonArray) {
				je = ((JsonArray) je).get(0).getAsJsonObject().get(ps.path.get(i));
			} else {
				logger.error("JsonElement is instance of {} which is neither a Object nor an Array."
						+ " This should not have happened!", je.getClass().getSimpleName());
				// TODO illegal branch
			}
			if (je == null) {
				break;
			}
		}
		if (je != null) {
			AnswerFV fv = new AnswerFV();
			fv.setFeature(AF.OriginFreebaseOntology, 1.0);
			// TODO possible valRes as an answer entity ID/URI
			String value = je.getAsString();
			value = value.replaceAll("[\\r\\n]", "");
			PropertyValue pv = new PropertyValue(title, title, ps.path.toString(), value, null,
					null, fv, AnswerSourceStructured.ORIGIN_ONTOLOGY);
			pv.setScore(ps.proba);
			results.add(pv);
		}
		return results;
	}
}
