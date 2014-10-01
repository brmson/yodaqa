package cz.brmlab.yodaqa.analysis.answer;

import java.util.LinkedList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;

/**
 * Annotate the AnswerHitlistCAS Answer FSes with score based on the
 * present AnswerFeatures.  This particular implementation uses
 * the estimated probability of the answer being correct as determined
 * by logistic regression based classifier trained on the training set
 * using the data/ml/train-answer.py script. */


public class AnswerScoreLogistic extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(AnswerScoreLogistic.class);

	/** The weights of individual elements of the FV.  These weights
	 * are output by data/ml/train-answer.py as this:
	 * (array([[ 0.1762562 ,  0.        ,  0.25589369,  0.        , -0.02734615,
	 *          -0.6981034 ,  0.13498186,  0.13297878, -0.26006545,  0.52802609,
	 *           0.575468  , -0.30750736,  0.2877974 ,  0.18623573,  0.99268805,
	 *           0.20342337,  0.08307843,  0.20342337,  0.37166926, -0.10370862,
	 *           0.08163508,  0.        ,  0.70315325, -0.43519261,  0.69678887,
	 *          -0.42882823,  0.12024469, -1.79032655]]), array([ 0.26796064]))
	 */
	public static double weights[] = {
		 0.1762562 ,  0.        ,  0.25589369,  0.        , -0.02734615,
		-0.6981034 ,  0.13498186,  0.13297878, -0.26006545,  0.52802609,
		 0.575468  , -0.30750736,  0.2877974 ,  0.18623573,  0.99268805,
		 0.20342337,  0.08307843,  0.20342337,  0.37166926, -0.10370862,
		 0.08163508,  0.        ,  0.70315325, -0.43519261,  0.69678887,
		-0.42882823,  0.12024469, -1.79032655,
		0, 0,  0, 0,  0, 0,
	};
	public static double intercept = 0.26796064;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	protected class AnswerScore {
		public Answer a;
		public double score;

		public AnswerScore(Answer a_, double score_) {
			a = a_;
			score = score_;
		}
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		List<AnswerScore> answers = new LinkedList<AnswerScore>();

		for (Answer a : JCasUtil.select(jcas, Answer.class)) {
			AnswerFV fv = new AnswerFV(a);

			double t = intercept;
			double fvec[] = fv.getFV();
			for (int i = 0; i < fvec.length; i++) {
				t += fvec[i] * weights[i];
			}

			double prob = 1.0 / (1.0 + Math.exp(-t));
			answers.add(new AnswerScore(a, prob));
		}

		/* Reindex the touched answer info(s). */
		for (AnswerScore as : answers) {
			as.a.removeFromIndexes();
			as.a.setConfidence(as.score);
			as.a.addToIndexes();
		}
	}
}
