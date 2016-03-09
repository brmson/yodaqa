package cz.brmlab.yodaqa.analysis.rdf;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;

public class RNNScoring {

	private static final String MODEL_URL = "http://pichl.ailao.eu:5000/score";

	public static List<Double> getScores(String question, List<String> labels, int propertyNumber) {
		List<Double> res = null;
		try {
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
//			BufferedReader br = new BufferedReader(new InputStreamReader(
//					(conn.getInputStream())));

//			String output;
//			System.out.println("Output from Server .... \n");
//			while ((output = br.readLine()) != null) {
//				System.out.println(output);
//			}
			conn.disconnect();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}

	private static String buildRequestBody(String question, List<String> labels, int propertyNumber) {
		JsonObject jobject = new JsonObject();
		jobject.addProperty("question", question);
		JsonArray propLabels = new JsonArray();
		for(String lab: labels) {
			propLabels.add(new JsonPrimitive(lab));
		}
		jobject.add("prop_labels", propLabels);
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
