package cz.brmlab.yodaqa.provider;

import java.util.concurrent.TimeUnit;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;

/** Wrapper around a deep learning based "STS" sentence pair scorer API.
 * Talks to https://github.com/brmson/dataset-sts scoring-api tool,
 * asking it to use its models to score pairs of token sequences - in our
 * case, that'd be, say, tuples of (question, passage) or (question,
 * proplabel).  dataset-sts implements a variety of models, from BM25 TFIDF
 * to advanced attention-based deep neural networks.
 *
 * TODO: Rather than a static class, the exact same mechanism as SolrNamedSource
 * when we do both property and passage selection. */
public class STSScoring {
	private static final String MODEL_URL = "http://pichl.ailao.eu:5000/score";

	public static List<Double> getScores(String qtext, List<String> atexts) {
		List<Double> res = null;
		while (true) {
			try {
				res = getScoresDo(qtext, atexts);
				break; // Success!
			} catch (IOException e) {
				notifyRetry(e);
			}
		}
		return res;
	}

	protected static List<Double> getScoresDo(String qtext, List<String> atexts) throws IOException {
		URL url = new URL(MODEL_URL);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");

		String input = buildRequestBody(qtext, atexts);

		OutputStream os = conn.getOutputStream();
		os.write(input.getBytes());
		os.flush();

		List<Double> res = processResponse(conn.getInputStream());
		conn.disconnect();
		return res;
	}

	protected static void notifyRetry(Exception e) {
		e.printStackTrace();
		System.err.println("*** " + MODEL_URL + " STS Query (temporarily?) failed, retrying in a moment...");
		try {
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e2) { // oof...
			e2.printStackTrace();
		}
	}

	private static String buildRequestBody(String qtext, List<String> atexts) {
		JsonObject jobject = new JsonObject();
		jobject.addProperty("qtext", qtext);
		JsonArray jatexts = new JsonArray();
		for(String atext : atexts) {
			jatexts.add(new JsonPrimitive(atext));
		}
		jobject.add("atext", jatexts);
		return jobject.toString();
	}

	@SuppressWarnings("unchecked")
	private static List<Double> processResponse(InputStream stream) {
		GsonBuilder builder = new GsonBuilder();
		Map<String, List<Double>> json = builder.create().fromJson(new InputStreamReader(stream), Map.class);
		return json.get("score");
	}
}
