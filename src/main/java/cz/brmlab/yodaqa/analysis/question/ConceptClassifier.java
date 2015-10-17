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
	/* Training data - correct: 643 (16.458%), incorrect: 3264 (83.542%) */

	/* 10-fold cross-validation (with 0.20 test splits): */
	/* CV fold precision 91.816% (718/782) */
	/* CV fold precision 92.967% (727/782) */
	/* CV fold precision 92.711% (725/782) */
	/* CV fold precision 91.560% (716/782) */
	/* CV fold precision 93.223% (729/782) */
	/* CV fold precision 93.862% (734/782) */
	/* CV fold precision 93.223% (729/782) */
	/* CV fold precision 92.455% (723/782) */
	/* CV fold precision 94.118% (736/782) */
	/* CV fold precision 91.944% (719/782) */
	/* === CV average precision 92.788% (+-SD 0.813%) */

	/* Training set precision 92.731% (3623/3907) */
	/* Model (trained on the whole training set): */
	double[] weights = {
		0.310978, // editDist
		4.596500, // labelProbability
		0.599989, // logPopularity
		-1.430439, // getByLAT
		0.334976, // getByNE
		0.996069, // getBySubject
		0.000000, // getByNgram
		0.985908, // getByFuzzyLookup
		0.025107, // getByCWLookup
	};
	double intercept = -6.666614;
	
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
