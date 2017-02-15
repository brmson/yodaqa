package cz.brmlab.yodaqa.analysis.rdf;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import cz.brmlab.yodaqa.provider.UrlManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RNNScoring {
	final static Logger logger = LoggerFactory.getLogger(RNNScoring.class);

	private static final String MODEL_URL = UrlManager.lookUpUrl(UrlManager.DataBackends.PROPSCORE.ordinal());
	private static final String SEPARATOR = " # ";

	public static List<Double> getScores(String question, List<String> labels, int propertyNumber) {
		List<Double> res = null;
		while(true) {
			try {
				logger.debug("Before Request");
				URL url = new URL(MODEL_URL);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setDoOutput(true);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "application/json");

				String input = buildRequestBody(question, labels, propertyNumber);

				OutputStream os = conn.getOutputStream();
				os.write(input.getBytes());
				os.flush();

				res = processResponse(conn.getInputStream());
				logger.debug("End Request");
				conn.disconnect();
				return res;
			} catch (IOException e) {
				e.printStackTrace();
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	public static List<Double> getFullPathScores(String question, List<List<String>> labels) {
		List<String> fullLabelList = new ArrayList<>();
		for (List<String> pathLabels: labels) {
			String fullLabel = StringUtils.join(pathLabels, SEPARATOR);
			fullLabelList.add(fullLabel);
		}
		return getScores(question, fullLabelList, 0);
	}

	private static String buildRequestBody(String question, List<String> labels, int propertyNumber) {
		JsonObject jobject = new JsonObject();
		jobject.addProperty("qtext", question.toLowerCase());
		JsonArray propLabels = new JsonArray();
		for(String lab: labels) {
			propLabels.add(new JsonPrimitive(lab.toLowerCase()));
		}
		jobject.add("atext", propLabels);
		// FIXME Parameter property_number is no longer used in new version of scoring-api
		jobject.addProperty("property_number", propertyNumber);
		return jobject.toString();
	}

	@SuppressWarnings("unchecked")
	private static List<Double> processResponse (InputStream stream) {
		GsonBuilder builder = new GsonBuilder();
		Map<String, List<Double>> json = builder.create().fromJson(new InputStreamReader(stream), Map.class);
		return json.get("score");
	}
}
