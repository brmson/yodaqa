package cz.brmlab.yodaqa.analysis.question;

import cz.brmlab.yodaqa.model.Question.Concept;

/**
 * Classify each concept as either relevant or non-relevant.
 * We use the logistic regression model created by
 * data/ml/concepts/concepts_train_logistic.py script.
 */
public class ConceptClassifier {
	/* Training data - correct: 708 (17.230%), incorrect: 3401 (82.770%) */

	/* 10-fold cross-validation (with 0.20 test splits): */
	/* CV fold precision 90.754% (746/822) */
	/* CV fold precision 91.849% (755/822) */
	/* CV fold precision 91.606% (753/822) */
	/* CV fold precision 90.268% (742/822) */
	/* CV fold precision 91.727% (754/822) */
	/* CV fold precision 90.754% (746/822) */
	/* CV fold precision 92.336% (759/822) */
	/* CV fold precision 91.119% (749/822) */
	/* CV fold precision 91.119% (749/822) */
	/* CV fold precision 91.606% (753/822) */
	/* === CV average precision 91.314% (+-SD 0.589%) */

	/* Training set precision 91.190% (3747/4109) */
	/* Model (trained on the whole training set): */
	double[] weights = {
		0.109481, // editDist
		5.898663, // probability
		0.507325, // score
		-1.419685, // getByLAT
		0.813817, // getByNE
		0.777757, // getBySubject
		-0.652947, // getByFuzzyLookup
		-1.630315, // getByCWLookup
	};
	double intercept = -4.075044;
	
	public double calculateProbability(Concept l) {
		double[] features = new double[8];
		features[0] = l.getEditDistance();
		features[1] = l.getProbability();
		features[2] = l.getScore();
		features[3] = l.getByLAT() ? 1 : 0;
		features[4] = l.getByNE() ? 1 : 0;
		features[5] = l.getBySubject() ? 1 : 0;
		features[6] = l.getByFuzzyLookup() ? 1 : 0;
		features[7] = l.getByCWLookup() ? 1 : 0;
		double sum = intercept;
		for(int i = 0; i < 8; i++) {
			sum += features[i]*weights[i];
		}
		double probability = sigmoid(sum);
		return probability;
	}

	public double sigmoid(double z) {
		return 1 / (1 + Math.exp(-z));
	}
}
