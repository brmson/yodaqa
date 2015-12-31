package cz.brmlab.yodaqa.analysis.question;

import java.util.List;

import org.apache.uima.jcas.JCas;

import cz.brmlab.yodaqa.analysis.rdf.PropertyGloVeScoring;
import cz.brmlab.yodaqa.model.Question.Concept;

/**
 * Classify each concept as either relevant or non-relevant.
 * We use the logistic regression model created by
 * data/ml/concepts/concepts_train_logistic.py script.
 */
public class ConceptClassifier {
	/* N.B. This is trained on the moviesE-train dataset, with question dump made by 43c197f
	 * not on curated or anything! */

	/* Training data - correct: 1187 (10.325%), incorrect: 10309 (89.675%) */

	/* 10-fold cross-validation (with 0.20 test splits): */
	/* CV fold precision 95.261% (2191/2300) */
	/* CV fold precision 94.565% (2175/2300) */
	/* CV fold precision 94.783% (2180/2300) */
	/* CV fold precision 94.826% (2181/2300) */
	/* CV fold precision 95.000% (2185/2300) */
	/* CV fold precision 95.174% (2189/2300) */
	/* CV fold precision 94.696% (2178/2300) */
	/* CV fold precision 94.609% (2176/2300) */
	/* CV fold precision 94.696% (2178/2300) */
	/* CV fold precision 94.522% (2174/2300) */
	/* === CV average precision 94.813% (+-SD 0.241%) */

	/* Training set precision 94.807% (10899/11496) */
	/* Model (trained on the whole training set): */
	double[] weights = {
		0.043782, // editDist
		4.750452, // labelProbability
		0.471023, // logPopularity
		4.550636, // relatedness
		-2.480552, // getByLAT
		0.323143, // getByNE
		0.099597, // getBySubject
		0.000000, // getByNgram
		0.963777, // getByFuzzyLookup
		-1.749823, // getByCWLookup
	};
	double intercept = -6.321102;

	public double calculateProbability(JCas questionView, Concept l) {
		List<String> qtoks = ConceptGloVeScoring.questionRepr(questionView);
		List<String> desctoks;
		if (l.getDescription() != null) {
			desctoks = ConceptGloVeScoring.tokenize(l.getDescription());
			l.setRelatedness(ConceptGloVeScoring.getInstance().relatedness(qtoks, desctoks));
		}

		double[] features = new double[weights.length];
		features[0] = l.getEditDistance();
		features[1] = l.getLabelProbability();
		features[2] = l.getLogPopularity();
		features[3] = l.getRelatedness();
		features[4] = l.getByLAT() ? 1 : 0;
		features[5] = l.getByNE() ? 1 : 0;
		features[6] = l.getBySubject() ? 1 : 0;
		features[7] = l.getByNgram() ? 1 : 0;
		features[8] = l.getByFuzzyLookup() ? 1 : 0;
		features[9] = l.getByCWLookup() ? 1 : 0;
		double sum = intercept;
		for(int i = 0; i < weights.length; i++) {
			sum += features[i]*weights[i];
		}
		double probability = sigmoid(sum);
		return probability;
	}

	public double sigmoid(double z) {
		return 1 / (1 + Math.exp(-z));
	}
}
