package cz.brmlab.yodaqa.analysis.question;

import cz.brmlab.yodaqa.model.Question.Concept;

/**
 * Class for classifying concepts using logistic regression model created by
 * data/ml/concepts/concepts_train_logistic.py script
 */
public class ConceptClassifier {
					   /*edit dist	probability  	score	 getByLat	 getByNE	 getBySubj	  getByFuzzy   getByCW */
	double[] weights = {0.10753278, 7.08861734, 0.53732016, -1.40466615, 0.95174967, 0.77767372, -1.52172064, -2.45157362};
	double intercept = -3.34765646;
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
