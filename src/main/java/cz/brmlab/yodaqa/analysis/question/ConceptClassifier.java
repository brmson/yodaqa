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
	/* Training data - correct: 671 (11.571%), incorrect: 5128 (88.429%) */

	/* 10-fold cross-validation (with 0.20 test splits): */
	/* CV fold precision 93.534% (1085/1160) */
	/* CV fold precision 94.483% (1096/1160) */
	/* CV fold precision 93.276% (1082/1160) */
	/* CV fold precision 94.224% (1093/1160) */
	/* CV fold precision 94.052% (1091/1160) */
	/* CV fold precision 95.000% (1102/1160) */
	/* CV fold precision 94.483% (1096/1160) */
	/* CV fold precision 95.259% (1105/1160) */
	/* CV fold precision 94.828% (1100/1160) */
	/* CV fold precision 93.448% (1084/1160) */
	/* === CV average precision 94.259% (+-SD 0.645%) */

	/* Training set precision 94.378% (5473/5799) */
	/* Model (trained on the whole training set): */
	double[] weights = {
		0.248860, // editDist
		4.457111, // labelProbability
		0.577820, // logPopularity
		4.231233, // relatedness
		-1.064028, // getByLAT
		0.683527, // getByNE
		0.491508, // getBySubject
		0.000000, // getByNgram
		1.178016, // getByFuzzyLookup
		-1.393301, // getByCWLookup
	};
	double intercept = -7.214133;
	
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
