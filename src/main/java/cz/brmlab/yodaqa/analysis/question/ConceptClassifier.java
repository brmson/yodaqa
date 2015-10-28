package cz.brmlab.yodaqa.analysis.question;

import cz.brmlab.yodaqa.model.Question.Concept;

/**
 * Classify each concept as either relevant or non-relevant.
 * We use the logistic regression model created by
 * data/ml/concepts/concepts_train_logistic.py script.
 */
public class ConceptClassifier {
	/* N.B. This is trained on the moviesC-train dataset,
	 * not on curated or anything! */
	/* Training data - correct: 671 (11.571%), incorrect: 5128 (88.429%) */

	/* 10-fold cross-validation (with 0.20 test splits): */
	/* CV fold precision 93.276% (1082/1160) */
	/* CV fold precision 94.052% (1091/1160) */
	/* CV fold precision 93.362% (1083/1160) */
	/* CV fold precision 93.707% (1087/1160) */
	/* CV fold precision 93.534% (1085/1160) */
	/* CV fold precision 94.655% (1098/1160) */
	/* CV fold precision 93.362% (1083/1160) */
	/* CV fold precision 94.397% (1095/1160) */
	/* CV fold precision 93.707% (1087/1160) */
	/* CV fold precision 92.155% (1069/1160) */
	/* === CV average precision 93.621% (+-SD 0.654%) */

	/* Training set precision 93.844% (5442/5799) */
	/* Model (trained on the whole training set): */
	double[] weights = {
		0.238905, // editDist
		5.083306, // labelProbability
		0.634009, // logPopularity
		-1.379002, // getByLAT
		0.754571, // getByNE
		0.761837, // getBySubject
		0.000000, // getByNgram
		1.413489, // getByFuzzyLookup
		-1.549633, // getByCWLookup
	};
	double intercept = -7.072493;
	
	public double calculateProbability(Concept l) {
		double[] features = new double[9];
		features[0] = l.getEditDistance();
		features[1] = l.getLabelProbability();
		features[2] = l.getLogPopularity();
		features[3] = l.getByLAT() ? 1 : 0;
		features[4] = l.getByNE() ? 1 : 0;
		features[5] = l.getBySubject() ? 1 : 0;
		features[6] = l.getByNgram() ? 1 : 0;
		features[7] = l.getByFuzzyLookup() ? 1 : 0;
		features[8] = l.getByCWLookup() ? 1 : 0;
		double sum = intercept;
		for(int i = 0; i < 9; i++) {
			sum += features[i]*weights[i];
		}
		double probability = sigmoid(sum);
		return probability;
	}

	public double sigmoid(double z) {
		return 1 / (1 + Math.exp(-z));
	}
}
