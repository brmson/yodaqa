package cz.brmlab.yodaqa.analysis.rdf;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cz.brmlab.yodaqa.model.Question.SV;
import cz.brmlab.yodaqa.model.TyCor.LAT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

/**
 * Freebase Property Path generator using logistic regression.
 *
 * This is an analytical component that is used as a support for primary
 * search in Freebase (but possibly also other RDF stores).  Based on
 * QuestionCAS features, it suggests the properties (or property paths
 * - sequences of properties to traverse) that are most likely to hold
 * the answer.  We can use this to:
 *
 *   (i) Add extra score feature for direct concept properties we fetch.
 *   (ii) Add extra non-direct properties that we query for specifically.
 *
 * See data/ml/fbpath/ for the training counterpart of this class and
 * some more documentation.  This is a simple classifier field that has
 * one (logreg) classifier per a specific fbpath, of course this does not
 * generalize well and needs further work, but should help a lot for the
 * common topics.
 *
 * XXX: We may want to apply this also to e.g. DBpedia.  We'll grow the
 * class hierarchy and abstractions as we need them.  We also might want
 * to store our analysis in a CAS, but we can't do this easily just in
 * the structured search pipeline as we need to treat QuestionCAS as
 * read-only there...
 */
public class FBPathLogistic {
	final Logger logger = LoggerFactory.getLogger(FBPathLogistic.class);

	protected class LogRegClassifier {
		public Map<String, Double> weights;
		public double intercept;

		public LogRegClassifier() {
			weights = new HashMap<>();
		}

		public double predictProba(List<String> feats) {
			double t = intercept;
			for (String f : feats) {
				Double w = weights.get(f);
				if (w != null)
					t += w;
			}
			double prob = 1.0 / (1.0 + Math.exp(-t));
			return prob;
		}
	};

	protected String modelName;
	protected Map<String, LogRegClassifier> pathCfiers = new HashMap<>();

	public void initialize() throws ResourceInitializationException {
		modelName = "FBPathLogistic.model";

		/* Load and parse the model. */
		try {
			loadModel(FBPathLogistic.class.getResourceAsStream(modelName));
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}

	protected void loadModel(InputStream model_stream) throws Exception {
		Gson gson = new Gson();
		JsonReader br = new JsonReader(new InputStreamReader(model_stream));
		br.setLenient(true);

		Type modelMap = new TypeToken<Map<String, Map<String, Double>>>(){}.getType();
		Map<String, Map<String, Double>> data = gson.fromJson(br, modelMap);
		for (String fbPath : data.keySet()) {
			Map<String, Double> weights = data.get(fbPath);
			LogRegClassifier cfier = new LogRegClassifier();
			cfier.intercept = weights.remove("_");
			cfier.weights = weights;
			pathCfiers.put(fbPath, cfier);
		}
	}

	/** Generate a set of string features from QuestionCAS.
	 * This is then passed to our other public methods. */
	public List<String> questionFeatures(JCas questionView) {
		List<String> feats = new ArrayList<>();
		for (SV sv : JCasUtil.select(questionView, SV.class)) {
			feats.add("sv=" + sv.getCoveredText());
		}
		for (LAT lat : JCasUtil.select(questionView, LAT.class)) {
			feats.add("lat/" + lat.getText() + "/" + lat.getClass().getSimpleName());
		}
		return feats;
	}


	/** Given question features and a given path, output its [0,1] score.
	 * In this case, it is the predicted probability of it holding
	 * the answer.  Returns null if we do not know this path.
	 *
	 * N.B. properties are in the Freebase /slash/separated format.
	 * Nodes are separated by |. */
	public Double getPathScore(List<String> feats, String fbPath) {
		LogRegClassifier cfier = pathCfiers.get(fbPath);
		if (cfier == null)
			return null;

		return cfier.predictProba(feats);
	}


	public class FBPathScore {
		/** Property path, where the properties are in the Freebase
		 * /slash/separated format and nodes are separated by |.
		 * E.g.:
		 *   * /tv/tv_program/tv_producer|/tv/tv_producer_term/producer
		 *   * /type/object/name
		 */
		public String fbPath;

		public double proba;

		public FBPathScore(String fbPath, double proba) {
			this.fbPath = fbPath;
			this.proba = proba;
		}
	};

	/** Given question features, predict the paths to query.  Returns
	 * a sorted list of paths, from most promising.  You will want
	 * to look just at the top N, as it eventually contains all the
	 * paths we know about. */
	public List<FBPathScore> getPaths(List<String> feats) {
		List<FBPathScore> scores = new ArrayList<>();
		for (Entry<String, LogRegClassifier> e : pathCfiers.entrySet()) {
			double proba = e.getValue().predictProba(feats);
			FBPathScore fbPathScore = new FBPathScore(e.getKey(), proba);
			scores.add(fbPathScore);
		}
		/* Sort by proba, from highest. */
		Collections.sort(scores, new Comparator<FBPathScore>(){ @Override
			public int compare(FBPathScore fbps1, FBPathScore fbps2){
				return Double.valueOf(fbps2.proba).compareTo(Double.valueOf(fbps1.proba));
			} } );
		return scores;
	}
}
