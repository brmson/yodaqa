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
	 * are output by data/ml/answer-train.py as this:
	 * (array([[  1.79894253e-01,   0.00000000e+00,   8.16199346e-01,
	 *           0.00000000e+00,   7.08827240e-01,  -2.71630416e-01,
	 *           7.94043960e-02,  -2.71630416e-01,  -2.45540637e-01,
	 *           5.33146168e-02,   1.78410882e-01,  -3.70636902e-01,
	 *          -4.68989748e-01,   2.76763728e-01,   2.84187292e-01,
	 *          -4.76413312e-01,  -6.46456455e-01,   4.54230435e-01,
	 *          -2.66712369e-01,   7.44863491e-02,  -1.24530202e+00,
	 *          -2.02395525e-01,  -6.00091064e-01,   4.07865044e-01,
	 *          -3.63507826e-04,  -1.91862512e-01,   1.30568505e+00,
	 *           8.92581430e-02,   1.75584417e-01,   8.92581430e-02,
	 *          -4.16349835e-02,  -1.50591036e-01,   1.11961322e-03,
	 *           0.00000000e+00,   0.00000000e+00,  -1.92226020e-01,
	 *          -4.54976949e-01,   2.62750929e-01,   2.02701856e-02,
	 *          -1.51120741e-01,   9.13914721e-01,  -1.10614074e+00,
	 *           6.49897600e-01,  -8.42123620e-01,   0.00000000e+00,
	 *          -1.92226020e-01]]), array([-0.19222602]))
	 *
	 * Cross-validation score mean 47.281% S.D. 3.666%
	 * (fullset) PERANS acc/prec/rcl/F2 = 0.763/1.000/0.241/0.285, @70 prec/rcl/F2 = 1.000/0.085/0.104, PERQ avail 0.702, any good = [0.493], simple 0.460
	 */
	public static double weights[] = {
		 1.79894253e-01, 0,   0.00000000e+00,   8.16199346e-01, 0,
		 0.00000000e+00,   7.08827240e-01,  0, -2.71630416e-01,
		 7.94043960e-02, 0,  -2.71630416e-01,  -2.45540637e-01, 0,
		 5.33146168e-02,   1.78410882e-01,  0, -3.70636902e-01,
		-4.68989748e-01, 0,   2.76763728e-01,   2.84187292e-01, 0,
		-4.76413312e-01,  -6.46456455e-01,  0,  4.54230435e-01,
		-2.66712369e-01, 0,   7.44863491e-02,  -1.24530202e+00, 0,
		-2.02395525e-01,  -6.00091064e-01,  0,  4.07865044e-01,
		-3.63507826e-04, 0,  -1.91862512e-01,   1.30568505e+00, 0,
		 8.92581430e-02,   1.75584417e-01,  0,  8.92581430e-02,
		-4.16349835e-02, 0,  -1.50591036e-01,   1.11961322e-03, 0,
		 0.00000000e+00,   0.00000000e+00,  0, -1.92226020e-01,
		-4.54976949e-01, 0,   2.62750929e-01,   2.02701856e-02, 0,
		-1.51120741e-01,   9.13914721e-01,  0, -1.10614074e+00,
		 6.49897600e-01, 0,  -8.42123620e-01,   0.00000000e+00, 0,
		-1.92226020e-01,
	};
	public static double intercept = -0.19222602;

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
		AnswerStats astats = new AnswerStats(jcas);

		List<AnswerScore> answers = new LinkedList<AnswerScore>();

		for (Answer a : JCasUtil.select(jcas, Answer.class)) {
			AnswerFV fv = new AnswerFV(a, astats);

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
