package cz.brmlab.yodaqa.analysis.rdf;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import cz.brmlab.yodaqa.provider.rdf.PropertyValue;
import org.jblas.DoubleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.ailao.glove.GloveDictionary;

/**
 * Created by honza on 22.2.16.
 */
public class LogisticRegressionFBPathRanking {
	private class Model {
		@SerializedName("weight_vector")
		public List<Double> weightVector;
		@SerializedName("intercept")
		public Double intercept;
	}

	final Logger logger = LoggerFactory.getLogger(LogisticRegressionFBPathRanking.class);
	private static final String modelName = "logistic-regression-ranker.model";
	private Model model;
	private GloveDictionary dict;

	public LogisticRegressionFBPathRanking() {
		Gson gson = new Gson();
		JsonReader reader = new JsonReader(new InputStreamReader(LogisticRegressionFBPathRanking.class.getResourceAsStream(modelName)));
		reader.setLenient(true);
		model = gson.fromJson(reader, Model.class);
		dict = GloveDictionary.getInstance();
	}

	public Double getScore(List<PropertyValue> path) {
		double[] vec = new double[1 + 51 * 3];
		Arrays.fill(vec, 0.0);
		vec[0] = path.size();
		for (int i = 0; i < path.size(); i++) {
			double[] v = gloveBOW(tokenize(path.get(i).getProperty()));
			double score = path.get(i).getScore();
			vec[1 + i*51] = score;
			System.arraycopy(v, 0, vec, 2 + i*51, 50);
		}
		return classify(vec);
	}

	private double[] gloveBOW(List<String> words) {
		DoubleMatrix bow = DoubleMatrix.zeros(50, 1);
		int w = 0;
		for (String word: words) {
			double[] x = dict.get(word);
			if (x != null) {
				DoubleMatrix glove = new DoubleMatrix(x);
				w++;
				bow = bow.add(glove);
			}
		}
		if (w != 0) bow = bow.div(w);
		return bow.toArray();
	}

	public static List<String> tokenize(String str) {
		return new ArrayList<>(Arrays.asList(str.toLowerCase().split("[\\p{Punct}\\s]+")));
	}

	private Double classify(double[] vec) {
		double t = model.intercept;
		for (int j = 0; j < vec.length; j++) {
			t += vec[j] * model.weightVector.get(j);
		}
		return 1.0 / (1.0 + Math.exp(-t));
	}
}
