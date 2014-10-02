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
	 * (array([[ 0.15143994,  0.        ,  0.28830829,  0.        ,  0.45316269,
	 *          -0.57582294,  0.01936958, -0.10290432, -0.61398354,  0.53044881,
	 *           0.32465292, -0.40818765, -0.6779909 ,  0.21245516, -0.6768397 ,
	 *           0.59330497, -0.28027459,  0.19673985,  1.06471387,  0.14374396,
	 *           0.05771756,  0.14374396,  0.28030463, -0.36383936,  0.01237182,
	 *           0.        ,  0.53462394, -0.61815868,  0.50520475, -0.58873949,
	 *           0.0172001 , -1.57577466,  0.86407878, -0.94761351,  0.53758617,
	 *          -0.6211209 , -1.39606464,  1.3125299 ]]), array([-0.08353474]))
	 */
	public static double weights[] = {
		 0.15143994,  0.        ,  0.28830829,  0.        ,  0.45316269,
		-0.57582294,
		0, 0, 0, 0,
	     	 0.01936958, -0.10290432, -0.61398354,  0.53044881,
		 0.32465292, -0.40818765,
		 0, 0,
		 -0.6779909 ,  0.21245516, -0.6768397 ,
		 0.59330497, -0.28027459,  0.19673985,  1.06471387,  0.14374396,
		 0.05771756,  0.14374396,  0.28030463, -0.36383936,  0.01237182,
		 0.        ,  0.53462394, -0.61815868,  0.50520475, -0.58873949,
		 0.0172001 , -1.57577466,  0.86407878, -0.94761351,  0.53758617,
		-0.6211209 , -1.39606464,  1.3125299,
	};
	public static double intercept = -0.08353474;

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
