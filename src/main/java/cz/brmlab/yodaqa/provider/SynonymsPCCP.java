package cz.brmlab.yodaqa.provider;

import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/** Wrapper around Tomas Vesely's PCCP Czech Synonym search engine. */
public class SynonymsPCCP {
	private static final String MODEL_URL = "http://cloudlab.ailao.eu:5060/synonyms";

	public static class Synonym {
		protected String wordid; // can contain various suffixes like _^(*2)
		protected String word;
		protected double score;

		public Synonym(String wordid, String word, double score) {
			this.wordid = wordid;
			this.word = word;
			this.score = score;
		}

		public String getWordid() {
			return wordid;
		}
		public String getWord() {
			return word;
		}
		public double getScore() {
			return score;
		}
	}

	public static List<Synonym> getSynonyms(String word) {
		List<Synonym> res = null;
		while (true) {
			try {
				res = getSynonymsDo(word);
				break; // Success!
			} catch (IOException e) {
				notifyRetry(e);
			}
		}
		return res;
	}

	protected static List<Synonym> getSynonymsDo(String word) throws IOException {
		URL url = new URL(MODEL_URL);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");

		String input = buildRequestBody(word);
		OutputStream os = conn.getOutputStream();
		os.write(input.getBytes());
		os.flush();

		List<Synonym> res = processResponse(conn.getInputStream());
		conn.disconnect();
		return res;
	}

	protected static void notifyRetry(Exception e) {
		e.printStackTrace();
		System.err.println("*** " + MODEL_URL + " SynonymsPCCP Query (temporarily?) failed, retrying in a moment...");
		try {
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e2) { // oof...
			e2.printStackTrace();
		}
	}

	private static String buildRequestBody(String word) {
		JsonObject jobject = new JsonObject();
		jobject.addProperty("word", word);
		jobject.addProperty("k", 50);
		return jobject.toString();
	}

	@SuppressWarnings("unchecked")
	private static List<Synonym> processResponse(InputStream stream) {
		List<Synonym> res = new ArrayList<>();
		JsonParser parser = new JsonParser();
		JsonArray jsonArray = parser.parse(new InputStreamReader(stream)).getAsJsonObject().get("syn").getAsJsonArray();
		for (int i = 0; i < jsonArray.size(); i++) {
			JsonObject js = (JsonObject) jsonArray.get(i);

			String wordid = js.get("word").getAsString();
			String word = wordid.replaceAll("[_-].*", "");
			double score = js.get("score").getAsDouble();

			Synonym s = new Synonym(wordid, word, score);
			res.add(s);
		}
		return res;
	}
}
