package cz.brmlab.yodaqa.provider.rdf;

import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
/**
 * This class fetches possible correct dbpedia labels from a python flask server
 * */

public final class LabelLookup {
    /* Singleton */
	private static LabelLookup instance = new LabelLookup();

    private LabelLookup() {}
    public static synchronized LabelLookup getInstance() {return instance;}
    public synchronized List<String> getLabels(String name, Logger logger) {
		List<String> results = new LinkedList<>();
		try {
			String encodedName = URLEncoder.encode(name, "UTF-8").replace("+", "%20");
			String requestURL = "http://localhost:5000/search/" + encodedName;
			URL request = new URL(requestURL);
			URLConnection connection = request.openConnection();
			JsonReader jr = new JsonReader(new InputStreamReader(connection.getInputStream()));

			jr.beginObject();
			while (jr.hasNext()) {
				jr.skipValue(); //result :
				jr.beginArray();
				while (jr.hasNext()) {
					String query = jr.nextString();
					logger.debug("Server returned: {}", query);
					results.add(query);
				}
				jr.endArray();
			}
			jr.endObject();
		} catch (IOException e) {
			e.printStackTrace();
			return results;
		}
		return results;
	}
}
