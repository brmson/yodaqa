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
	/* CV fold precision 93.966% (1090/1160) */
	/* CV fold precision 94.655% (1098/1160) */
	/* CV fold precision 93.879% (1089/1160) */
	/* CV fold precision 94.397% (1095/1160) */
	/* CV fold precision 94.483% (1096/1160) */
	/* CV fold precision 94.741% (1099/1160) */
	/* CV fold precision 94.138% (1092/1160) */
	/* CV fold precision 95.086% (1103/1160) */
	/* CV fold precision 94.655% (1098/1160) */
	/* CV fold precision 92.845% (1077/1160) */
	/* === CV average precision 94.284% (+-SD 0.595%) */

	/* Training set precision 94.413% (5475/5799) */
	/* Model (trained on the whole training set): */
	double[] weights = {
		0.252353, // editDist
		4.379324, // labelProbability
		0.556761, // logPopularity
		3.848883, // relatedness
		-1.137121, // getByLAT
		0.803509, // getByNE
		0.482381, // getBySubject
		0.000000, // getByNgram
		1.259178, // getByFuzzyLookup
		-1.368221, // getByCWLookup
	};
	double intercept = -7.157712;
	
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
