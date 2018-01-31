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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

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
		List<JsonElement> candidates = new ArrayList<>();
		List<JsonElement> candidatesTmp = new LinkedList<>();
		candidates.add(je.getAsJsonObject().get("data").getAsJsonArray().get(0).getAsJsonObject());
		for (int i = 0; i < ps.path.size(); i++) {
			JsonElement first = candidates.get(0);
			if (first instanceof JsonObject) {
				for(JsonElement c: candidates) {
					candidatesTmp.add(c.getAsJsonObject().get(ps.path.get(i)));
				}
			} else if (first instanceof JsonArray) {
				for(JsonElement c: candidates) {
					for(JsonElement c2: c.getAsJsonArray()) {
						candidatesTmp.add(c2.getAsJsonObject().get(ps.path.get(i)));
					}
				}
			} else {
				logger.error("JsonElement is instance of {} which is neither a Object nor an Array."
						+ " This should not have happened!", je.getClass().getSimpleName());
				// TODO illegal branch
			}
			candidates = new ArrayList<>(candidatesTmp);
			candidates.removeAll(Collections.singleton(null));
			candidatesTmp.clear();
			if (candidates.isEmpty()) {
				break;
			}
		}
		if (!candidates.isEmpty()) {
			// TODO possible valRes as an answer entity ID/URI
			for (JsonElement c: candidates) {
				if (c instanceof JsonArray) {
					for (JsonElement c2: c.getAsJsonArray()) {
						results.add(createPV(title, ps.path.toString(), c2.getAsString(), ps.proba));
					}
				} else {
					results.add(createPV(title, ps.path.toString(), c.getAsString(), ps.proba));
				}
			}

		}
		return results;
	}

	public PropertyValue createPV(String title, String path, String value, double score) {
		AnswerFV fv = new AnswerFV();
		fv.setFeature(AF.OriginFreebaseOntology, 1.0);
		value = value.replaceAll("[\\r\\n]", "");
		PropertyValue pv = new PropertyValue(title, title, path, value, null,
				null, fv, AnswerSourceStructured.ORIGIN_ONTOLOGY);
		pv.setScore(score);
		return pv;
	}
}
