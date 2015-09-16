package cz.brmlab.yodaqa.provider.glove;

import eu.ailao.glove.GloveDictionary;
import org.jblas.DoubleMatrix;

import java.util.List;

/**
 * Counts probability of sentence(property) containing a correct answer to given question.
 * Uses word embeddings (GloVe) box-of-words.
 * More at https://github.com/brmson/Sentence-selection.
 */
public class Relatedness {

	private MbWeights mb;
	private GloveDictionary dict;

	public Relatedness(MbWeights mb) {
		this.mb = mb;
		this.dict = GloveDictionary.getInstance();
	}

	public double probability(List<String> q, List<String> a) {
		return probability(gloveBOW(q, this.dict), gloveBOW(a, this.dict));
	}

	/**
	 * Counts probability from q,a glove vectors.
	 */
	private double probability(DoubleMatrix q, DoubleMatrix a) {
		DoubleMatrix M = this.mb.getM();
		double b = this.mb.getB();
		return 1 / (1 + Math.exp(-z(q, M, a, b)));
	}

	/**
	 * Returns glove vectors from input sentence, dictionary. Uses box-of-words(mean of all words) approach.
	 */
	private DoubleMatrix gloveBOW(List<String> words, GloveDictionary dict) {
		DoubleMatrix bow = DoubleMatrix.zeros(50, 1);
		int w = 0;
		for (int i = 0; i < words.size(); i++) {
			double[] x = dict.get(words.get(i));
			if (x != null) {
				DoubleMatrix glove = new DoubleMatrix(x);
				w++;
				bow = bow.add(glove);
			}
		}
		if (w != 0) bow = bow.div(w);
		return bow;
	}

	/**
	 * inputs: word embedding vectors q(represents question),a(represents answer),
	 * weight matrix M, intercept b
	 * ouput=q^T*M*a+b
	 */
	private double z(DoubleMatrix q, DoubleMatrix M, DoubleMatrix a, double b) {
		return q.transpose().mmul(M).mmul(a).get(0) + b;
	}
}