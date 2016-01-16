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
	/* N.B. This is trained on the moviesF-train dataset,
	 * not on curated or anything! */
	/* Training data - correct: 1217 (9.896%), incorrect: 11081 (90.104%) */

	/* 10-fold cross-validation (with 0.20 test splits): */
	/* CV fold precision 93.740% (2306/2460) */
	/* CV fold precision 94.756% (2331/2460) */
	/* CV fold precision 94.390% (2322/2460) */
	/* CV fold precision 94.350% (2321/2460) */
	/* CV fold precision 94.309% (2320/2460) */
	/* CV fold precision 94.797% (2332/2460) */
	/* CV fold precision 94.837% (2333/2460) */
	/* CV fold precision 95.122% (2340/2460) */
	/* CV fold precision 94.187% (2317/2460) */
	/* CV fold precision 94.512% (2325/2460) */
	/* === CV average precision 94.500% (+-SD 0.374%) */

	/* Training set precision 94.609% (11635/12298) */
	/* Model (trained on the whole training set): */
	double[] weights = {
		0.008670, // editDist
		4.447859, // labelProbability
		0.506590, // logPopularity
		4.145722, // relatedness
		-1.986273, // getByLAT
		0.500504, // getByNE
		0.270530, // getBySubject
		0.000000, // getByNgram
		0.837017, // getByFuzzyLookup
		-1.686557, // getByCWLookup
	};
	double intercept = -6.556334;
	
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
