package cz.brmlab.yodaqa.analysis.question;

import cz.brmlab.yodaqa.model.Question.Concept;

/**
 * Classify each concept as either relevant or non-relevant.
 * We use the logistic regression model created by
 * data/ml/concepts/concepts_train_logistic.py script.
 */
public class ConceptClassifier {
	/* Training data - correct: 602 (16.109%), incorrect: 3135 (83.891%) */

	/* 10-fold cross-validation (with 0.20 test splits): */
	/* CV fold precision 93.717% (701/748) */
	/* CV fold precision 93.717% (701/748) */
	/* CV fold precision 94.385% (706/748) */
	/* CV fold precision 94.118% (704/748) */
	/* CV fold precision 93.449% (699/748) */
	/* CV fold precision 93.984% (703/748) */
	/* CV fold precision 94.118% (704/748) */
	/* CV fold precision 91.845% (687/748) */
	/* CV fold precision 93.048% (696/748) */
	/* CV fold precision 92.513% (692/748) */
	/* === CV average precision 93.489% (+-SD 0.761%) */

	/* Training set precision 93.497% (3494/3737) */
	/* Model (trained on the whole training set): */
	double[] weights = {
		0.345124, // editDist
		5.360875, // labelProbability
		0.440630, // logPopularity
		-1.085939, // getByLAT
		0.411121, // getByNE
		0.686013, // getBySubject
		1.258005, // getByFuzzyLookup
		-0.115068, // getByCWLookup
	};
	double intercept = -6.339254;
	
	public double calculateProbability(Concept l) {
		double[] features = new double[8];
		features[0] = l.getEditDistance();
		features[1] = l.getLabelProbability();
		features[2] = l.getLogPopularity();
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
