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
	 * (array([[  1.51770480e-01,   0.00000000e+00,   8.53533232e-01,
	 *           0.00000000e+00,   8.70762639e-01,  -1.16847360e-01,
	 *          -1.57679145e-01,  -1.16847360e-01,  -2.75131267e-01,
	 *           6.04761919e-04,   2.44755258e-01,  -5.19281763e-01,
	 *          -4.46202358e-01,   1.71675854e-01,   4.10294262e-02,
	 *          -3.15555931e-01,  -7.51911254e-01,   4.77384749e-01,
	 *          -4.11731051e-01,   1.37204546e-01,  -6.48110965e-01,
	 *          -2.87487696e-01,  -4.62184718e-01,   1.87658213e-01,
	 *          -5.69143303e-02,  -2.17612174e-01,   1.33249646e+00,
	 *           1.57573624e-01,   5.08007770e-01,   1.57573624e-01,
	 *          -3.00933619e-01,   2.64071138e-02,   4.85139006e-02,
	 *           0.00000000e+00,   6.44631173e-01,  -9.19157678e-01,
	 *           5.13373577e-01,  -7.87900082e-01,   4.25325381e-01,
	 *          -1.34851251e+00,   7.12790677e-01,  -9.87317182e-01,
	 *           4.63245432e-01,  -7.37771937e-01,  -1.38757391e+00,
	 *           1.11304741e+00]]), array([-0.2745265]))
	 *
	 * (testset) PERANS acc/prec/rcl/F2 = 0.756/0.056/0.590/0.204, @70 prec/rcl/F2 = 0.106/0.333/0.233, PERQ avail 0.670, any good = [0.560], simple 0.499
	 */
	public static double weights[] = {
		 1.51770480e-01,   0.00000000e+00,   8.53533232e-01,
		 0.00000000e+00,   8.70762639e-01,  -1.16847360e-01,
		-1.57679145e-01,  -1.16847360e-01,  -2.75131267e-01,
		 6.04761919e-04,   2.44755258e-01,  -5.19281763e-01,
		-4.46202358e-01,   1.71675854e-01,   4.10294262e-02,
		-3.15555931e-01,  -7.51911254e-01,   4.77384749e-01,
		-4.11731051e-01,   1.37204546e-01,  -6.48110965e-01,
		-2.87487696e-01,  -4.62184718e-01,   1.87658213e-01,
		-5.69143303e-02,  -2.17612174e-01,   1.33249646e+00,
		 1.57573624e-01,   5.08007770e-01,   1.57573624e-01,
		-3.00933619e-01,   2.64071138e-02,   4.85139006e-02,
		 0.00000000e+00,   6.44631173e-01,  -9.19157678e-01,
		 5.13373577e-01,  -7.87900082e-01,   4.25325381e-01,
		-1.34851251e+00,   7.12790677e-01,  -9.87317182e-01,
		 4.63245432e-01,  -7.37771937e-01,  -1.38757391e+00,
		 1.11304741e+00,
	};
	public static double intercept = -0.2745265;

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
