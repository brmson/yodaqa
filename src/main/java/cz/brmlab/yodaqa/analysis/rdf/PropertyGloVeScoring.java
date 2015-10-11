package cz.brmlab.yodaqa.analysis.rdf;

import cz.brmlab.yodaqa.provider.glove.MbWeights;
import cz.brmlab.yodaqa.provider.glove.Relatedness;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Counts probability of property containing a correct answer to given question.
 * More info can be found at https://github.com/brmson/Sentence-selection
 */
public class PropertyGloVeScoring {

	private static PropertyGloVeScoring pgs = new PropertyGloVeScoring();

	public static PropertyGloVeScoring getInstance() {
		return pgs;
	}

	private Relatedness r = new Relatedness(new MbWeights(PropertyGloVeScoring.class.getResourceAsStream("Mbprop.txt")));

	public double relatedness(String q, String prop) {
		List<String> ql = tokenize(q);
		List<String> a = tokenize(prop);

		double res = r.probability(ql, a);
		return res;
	}

	/** For legacy reasons, we use our own tokenization.
	 * We also lower-case while at it, and might do some other
	 * normalization steps...
	 * XXX: Rely on pipeline instead? */
	public static List<String> tokenize(String str) {
		return new ArrayList<>(Arrays.asList(str.toLowerCase().split("[\\p{Punct}\\s]+")));
	}
}
