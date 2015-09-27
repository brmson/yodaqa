package cz.brmlab.yodaqa.analysis.question;

import cz.brmlab.yodaqa.model.Question.Concept;

/**
 * Classify each concept as either relevant or non-relevant.
 * We use the logistic regression model created by
 * data/ml/concepts/concepts_train_logistic.py script.
 */
public class ConceptClassifier {
	/* Model (trained on the whole training set): */
	double[] weights = {
		0.107533, // editDist
		7.088617, // probability
		0.537320, // score
		-1.404666, // getByLAT
		0.951750, // getByNE
		0.777674, // getBySubject
		-1.521721, // getByFuzzyLookup
		-2.451574, // getByCWLookup
	};
	double intercept = -3.347656;
	
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
		return sigmoid(sum);
	}

	public double sigmoid(double z) {
		return 1 / (1 + Math.exp(-z));
	}
}
