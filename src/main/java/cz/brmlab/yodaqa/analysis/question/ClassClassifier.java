package cz.brmlab.yodaqa.analysis.question;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import cz.brmlab.yodaqa.model.Question.QuestionClass;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.Question.SV;
import cz.brmlab.yodaqa.model.TyCor.ImplicitQLAT;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.WordnetLAT;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.*;

/**
 * Generate class fro each question. It uses logistic regression classifier
 * to determine question class. The classifier needs to be trained by
 * data/ml/qclass/train_question_classifier.py and the model needs to placed
 * in src/main/resources/cz/brmlab/yodaqa/analysis/question/question-classifier.model
 */
public class ClassClassifier extends JCasAnnotator_ImplBase {
	private class Model {
		@SerializedName("weight_vector")
		public List<List<Double>> weightVector;
		@SerializedName("intercept")
		public List<Double> intercept;
		@SerializedName("feature_indices")
		public Map<String, Integer> featureIndices;
		@SerializedName("labels")
		public List<String> labels;
	}
	final Logger logger = LoggerFactory.getLogger(ClassClassifier.class);
	private static final String modelName = "question-classifier.model";
	private Model model;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		//Load model
		Gson gson = new Gson();
		JsonReader reader = new JsonReader(new InputStreamReader(ClassClassifier.class.getResourceAsStream(modelName)));
		reader.setLenient(true);
		model = gson.fromJson(reader, Model.class);
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		double[] vector;
		try {
			vector = new double[model.weightVector.get(0).size()];
		} catch (IndexOutOfBoundsException e) {
			throw new AnalysisEngineProcessException(e);
		}
		Arrays.fill(vector, 0.0);
		String featureName;
		for (LAT lat : JCasUtil.select(jCas, LAT.class)) {
			if (lat instanceof WordnetLAT) continue;
			featureName = "lat/" + lat.getText() + "/" + lat.getClass().getSimpleName();
			if (model.featureIndices.containsKey(featureName))
			vector[model.featureIndices.get(featureName)] = 1;
		}
		Collection<SV> supportVerbs = JCasUtil.select(jCas, SV.class);
		for (SV sv : supportVerbs) {
			featureName = "sv=" + sv.getCoveredText();
			if (model.featureIndices.containsKey(featureName))
				vector[model.featureIndices.get(featureName)] = 1;
		}
		if (supportVerbs.size() == 0) vector[model.featureIndices.get("sv_not_present")] = 1;
		List<Double> probs = classify(vector);
		int maxIdx = -1;
		double max = Double.MIN_VALUE;
		for (int i = 0; i < probs.size(); i++) {
			if (probs.get(i) > max) {
				max = probs.get(i);
				maxIdx = i;
			}
		}
		String cls = model.labels.get(maxIdx);
		QuestionClass qc = new QuestionClass(jCas);
		qc.setQuestionClass(cls);
		qc.addToIndexes();
	}

	private List<Double> classify(double[] vec) {
		ArrayList<Double> res = new ArrayList<>();
		//Sparse vector, possible optimization
		for (int i = 0; i < model.labels.size(); i++) {
			double t = model.intercept.get(i);
			for (int j = 0; j < vec.length; j++) {
				t += vec[j] * model.weightVector.get(i).get(j);
			}
			double prob = 1.0 / (1.0 + Math.exp(-t));
			res.add(prob);
		}
		return res;
	}
}
