package cz.brmlab.yodaqa.provider.diffbot;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import cz.brmlab.yodaqa.provider.UrlManager;
import cz.brmlab.yodaqa.provider.rdf.DBpediaTitles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DiffbotEntityLinker {
	private static final Logger logger = LoggerFactory.getLogger(DiffbotEntityLinker.class);
	private static final String ENDPOINT_URL = UrlManager.lookUpUrl(UrlManager.DataBackends.DIFFBOT_EL.ordinal());
	private static final double CONFIDENCE = 0.0;
	private static final int MAX_TAGS = 100;

	public static class Article {
		public class KG {
			public String id;
			public String decription;
		}
		public class Offset {
			public List<List<Integer>> title;
		}
		public int count;
		public String label;
		public List<String> surfaceForms;
		public String origin;
		public double score;
		public String uri;

		public Offset offsets;
		public KG kg;
	}

	public List<Article> entityLookup(String query) throws IOException {
		String encodedQuery = URLEncoder.encode(query, "UTF-8");
		String requestURL = ENDPOINT_URL + "?confidence=" + CONFIDENCE+ "&includeKG&maxTags=" + MAX_TAGS
				+ "&lang=en&text=&title=" + encodedQuery;
		logger.debug("Diffbot EL request URL: {}", requestURL);
		URL request = new URL(requestURL);
		URLConnection connection = request.openConnection();
		Gson gson = new Gson();

		JsonReader jr = new JsonReader(new InputStreamReader(connection.getInputStream()));
		JsonElement je = new JsonParser().parse(jr);
		JsonArray ja = je.getAsJsonObject().getAsJsonArray("all-tags");

		List<Article> results = new LinkedList<>();
		logger.debug("Ja size {} ", ja.size());
		for(JsonElement e: ja) {
			results.add(gson.fromJson(e, Article.class));
		}
		return results;
	}
}
