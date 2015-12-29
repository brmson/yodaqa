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
	/* N.B. This is trained on the moviesC-train dataset,
	 * not on curated or anything! */

	/* Training data - correct: 1163 (10.207%), incorrect: 10231 (89.793%) */

	/* 10-fold cross-validation (with 0.20 test splits): */
	/* CV fold precision 93.857% (2139/2279) */
	/* CV fold precision 95.437% (2175/2279) */
	/* CV fold precision 95.305% (2172/2279) */
	/* CV fold precision 93.550% (2132/2279) */
	/* CV fold precision 94.559% (2155/2279) */
	/* CV fold precision 94.603% (2156/2279) */
	/* CV fold precision 95.086% (2167/2279) */
	/* CV fold precision 94.954% (2164/2279) */
	/* CV fold precision 94.208% (2147/2279) */
	/* CV fold precision 94.559% (2155/2279) */
	/* === CV average precision 94.612% (+-SD 0.580%) */

	/* Training set precision 94.734% (10794/11394) */
	/* Model (trained on the whole training set): */
	double[] weights = {
		0.044246, // editDist
		4.671980, // labelProbability
		0.467709, // logPopularity
		4.503668, // relatedness
		-2.432906, // getByLAT
		0.318891, // getByNE
		0.059543, // getBySubject
		0.000000, // getByNgram
		0.941107, // getByFuzzyLookup
		-1.713363, // getByCWLookup
	};
	double intercept = -6.264199;


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
