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
	/* Training data - correct: 666 (13.077%), incorrect: 4427 (86.923%) */

	/* 10-fold cross-validation (with 0.20 test splits): */
	/* CV fold precision 93.327% (951/1019) */
	/* CV fold precision 93.916% (957/1019) */
	/* CV fold precision 92.934% (947/1019) */
	/* CV fold precision 94.504% (963/1019) */
	/* CV fold precision 93.621% (954/1019) */
	/* CV fold precision 94.799% (966/1019) */
	/* CV fold precision 94.406% (962/1019) */
	/* CV fold precision 93.916% (957/1019) */
	/* CV fold precision 94.014% (958/1019) */
	/* CV fold precision 94.112% (959/1019) */
	/* === CV average precision 93.955% (+-SD 0.529%) */

	/* Training set precision 94.384% (4807/5093) */
	/* Model (trained on the whole training set): */
	double[] weights = {
		0.144153, // editDist
		4.514770, // labelProbability
		0.532836, // logPopularity
		4.807308, // relatedness
		-0.888569, // getByLAT
		0.683580, // getByNE
		0.203821, // getBySubject
		0.000000, // getByNgram
		1.399657, // getByFuzzyLookup
		-1.383187, // getByCWLookup
	};
	double intercept = -7.103194;
	
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
