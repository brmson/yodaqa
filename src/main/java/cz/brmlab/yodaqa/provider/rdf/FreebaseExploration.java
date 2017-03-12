package cz.brmlab.yodaqa.provider.rdf;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceStructured;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.Question.Concept;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;


public class FreebaseExploration {
	private static final String PATH = "data/ml/fbpath-emb/fbconcepts/";
	private static final String FREEBASE_URL = "https://www.googleapis.com/freebase/v1/topic/m/";
	protected Logger logger = LoggerFactory.getLogger(FreebaseExploration.class);

	private FreebaseOntology fbo;
	private HashMap<String, String> labelCache;
	private HashMap<Integer, Set<FreebaseOntology.TitledMid>> midCache;


	public FreebaseExploration() {
		fbo = new FreebaseOntology();
		labelCache = new HashMap<>();
		midCache = new HashMap<>();
	}

	public List<List<PropertyValue>> getConceptNeighbourhood(Concept c, List<Concept> witnesses, Collection<Clue> clues) {
		InputStream is = loadConcept(c);
		if (is == null) return new ArrayList<>();
		JsonReader jr = new JsonReader(new InputStreamReader(is));
		JsonParser jp = new JsonParser();
		List<String> witLabels = new ArrayList<>();
		for(Clue clue: clues) {
			witLabels.add(clue.getLabel());
		}
		List<List<PropertyValue>> n = walkNode(jp.parse(jr), new ArrayList<PropertyValue>(), new ArrayList<PropertyValue>(), witnesses, witLabels);
//		for(List<PropertyValue> path: n) {
//			logger.debug("Return path");
//			for(PropertyValue pv: path)
//				logger.debug(pv.getProperty() + " " + pv.getPropRes());
//		}
		return n;
	}

	private List<List<PropertyValue>> walkNode(JsonElement json, List<PropertyValue> pathPrefix, List<PropertyValue> pathSuffix,
											   List<Concept> witnesses, List<String> witLabels) {
		List<List<PropertyValue>> relPaths = new ArrayList<>();

		List<PropertyValue> pathSuffixes = new ArrayList<>(pathSuffix);
		if ((!witnesses.isEmpty() || !witLabels.isEmpty()) && !pathPrefix.isEmpty()) {
			for(Map.Entry<String, JsonElement> e: json.getAsJsonObject().get("property").getAsJsonObject().entrySet()) {
				if (isFiltered(e.getKey())) continue;
				for (JsonElement val: e.getValue().getAsJsonObject().get("values").getAsJsonArray()) {
					if (val.getAsJsonObject().has("id")) {
						String id = val.getAsJsonObject().get("id").getAsString();
						id = "m." + id.substring(3);
//						logger.debug("ID " + id);
						boolean match = false;
						for(Concept w: witnesses) {
							if (id.equals(w.getFreebaseID())) {
								PropertyValue pv = makePV(e.getKey(), id);
								pv.setConcept(w);
								pathSuffixes.add(pv);
								break;
							}
						}
					}
					for(String label: witLabels) {
						if (val.getAsJsonObject().get("text").getAsString().contains(label)) {
							pathSuffixes.add(makePV(e.getKey(), null));
						}
					}
				}
			}
		}
		for(Map.Entry<String, JsonElement> e: json.getAsJsonObject().get("property").getAsJsonObject().entrySet()) {
			if (isFiltered(e.getKey())) continue;
			for(PropertyValue suffix: pathSuffixes) {
				List<PropertyValue> path = new ArrayList<>();
				path.addAll(pathPrefix);
				path.add(makePV(e.getKey(), json.getAsJsonObject().get("id").getAsString()));
				path.add(suffix);
				relPaths.add(path);
			}
			if (pathSuffixes.isEmpty()) {
				List<PropertyValue> path = new ArrayList<>();
				path.addAll(pathPrefix);
				path.add(makePV(e.getKey(), json.getAsJsonObject().get("id").getAsString()));
				relPaths.add(path);
			}
		}
		for(Map.Entry<String, JsonElement> e: json.getAsJsonObject().get("property").getAsJsonObject().entrySet()) {
			if (isFiltered(e.getKey())) continue;
			for (JsonElement val: e.getValue().getAsJsonObject().get("values").getAsJsonArray()) {
				if (val.getAsJsonObject().has("property")) {
					List<PropertyValue> newPrefix = new ArrayList<>();
					newPrefix.addAll(pathPrefix);
					newPrefix.add(makePV(e.getKey(), json.getAsJsonObject().get("id").getAsString()));
					relPaths.addAll(walkNode(val, newPrefix, pathSuffixes, witnesses, witLabels));
				}
			}
		}
		return relPaths;
	}

//	private Set<FreebaseOntology.TitledMid> getMids(int pageId) {
//		Set<FreebaseOntology.TitledMid> mids;
//		if (midCache.containsKey(pageId)) mids = midCache.get(pageId);
//		else {
//			mids = fbo.queryTopicByPageID(pageId, logger);
//			midCache.put(pageId, mids);
//		}
//		return mids;
//	}

	private boolean isFiltered(String property) {
		String[] filters = new String[]{"/type", "/common"};
		for (int i = 0; i < filters.length; i++) {
			if (property.startsWith(filters[i])) return true;
		}
		return false;
	}

	private PropertyValue makePV(String property, String mid) {
		AnswerFV fv = new AnswerFV();
		fv.setFeature(AF.OriginFreebaseOntology, 1.0);
		property = property.substring(1).replaceAll("/",".");
//		logger.debug("Property {}", property);
		if (!labelCache.containsKey(property)) labelCache.put(property, fbo.queryPropertyLabel(property));
		String label = labelCache.get(property);
		String rmid = mid;
		if (mid != null && mid.startsWith("/")) rmid = mid.substring(1).replace("/", ".");
		PropertyValue pv = new PropertyValue(null, rmid, label, null, null, null,
				fv, AnswerSourceStructured.ORIGIN_ONTOLOGY);
		pv.setPropRes(property);
		return pv;
	}

	private InputStream loadConcept(Concept c) {
//		logger.debug("API {}", System.getProperty("cz.brmlab.yodaqa.provider.rdf.FreebaseExploration.ApiKey"));
		InputStream is = null;
//		logger.debug("Concept {} with page ID {}", c.getFullLabel(), c.getPageID());

		String fullPath = PATH + c.getFreebaseID() + ".json";
		try {
//				logger.debug("PATH {}", fullPath);
			is = new FileInputStream(fullPath);
		} catch (FileNotFoundException e) {
//				logger.debug("No such file {}. Querying freebase API...", mid.mid);
			HttpURLConnection conn = null;
			try {
				String urlString = FREEBASE_URL + c.getFreebaseID().substring(2, c.getFreebaseID().length());
				String key = System.getProperty("cz.brmlab.yodaqa.provider.rdf.FreebaseExploration.ApiKey");
				if (key != null && !key.isEmpty()) urlString += "?key=" + key;
//					logger.debug("API {}", System.getProperty("cz.brmlab.yodaqa.provider.rdf.FreebaseExploration.ApiKey"));
//					logger.debug("URL {}", urlString);
				URL url = new URL(urlString);
				conn = (HttpURLConnection) url.openConnection();
				is = conn.getInputStream();
				FileUtils.copyInputStreamToFile(is, new File(fullPath));
				conn.disconnect();
				logger.debug("Exists " + new File(fullPath).exists());
				is = new FileInputStream(fullPath);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return is;
	}
}
