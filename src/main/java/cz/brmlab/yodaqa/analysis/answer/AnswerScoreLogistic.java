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
	 * (array([[ 0.14213875,  0.        ,  0.26711944,  0.        ,  0.04749269,
	 *          -0.39346911,  0.00577676,  0.15180313, -0.36292408,  0.52050398,
	 *           0.34582868, -0.18824878, -0.66028164,  0.33980725,  1.3931512 ,
	 *           0.1813575 , -0.14635492,  0.1813575 ,  0.43095354, -0.27337365,
	 *           0.02388198,  0.        ,  0.75669859, -0.5991187 ,  0.76564006,
	 *          -0.60806017,  0.11702063, -1.64688582,  0.88069801, -0.72311811,
	 *           0.58711918, -0.42953929, -1.14065297,  1.29823286]]), array([ 0.15757989]))
	 */
	public static double weights[] = {
		 0.14213875,  0.        ,  0.26711944,  0.        ,  0.04749269,
		-0.39346911,  0.00577676,  0.15180313, -0.36292408,  0.52050398,
		 0.34582868, -0.18824878, -0.66028164,  0.33980725,
		 0, 0, 0, 0,
		 1.3931512 ,
		 0.1813575 , -0.14635492,  0.1813575 ,  0.43095354, -0.27337365,
		 0.02388198,  0.        ,  0.75669859, -0.5991187 ,  0.76564006,
		-0.60806017,  0.11702063, -1.64688582,  0.88069801, -0.72311811,
		 0.58711918, -0.42953929, -1.14065297,  1.29823286,
	};
	public static double intercept = 0.15757989;

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
