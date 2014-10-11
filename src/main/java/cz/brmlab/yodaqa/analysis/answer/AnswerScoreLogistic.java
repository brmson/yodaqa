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
	 * (array([[ 0.19062282,  0.        ,  0.69901299,  0.        ,  0.43837462,
	 *         -0.2314449 ,  0.17428271, -0.2314449 , -0.26978136,  0.21261916,
	 *          0.06630427, -0.12346647, -0.29989136,  0.24272916,  0.1931069 ,
	 *         -0.2502691 , -0.50203657,  0.44487437, -0.20081361,  0.14365142,
	 *         -1.04782028, -0.17413659,  0.26842728, -0.32558948,  0.12884411,
	 *         -0.18600631,  1.84164989,  0.19841181, -0.14553963,  0.19841181,
	 *          0.04365706, -0.10081926,  0.02087168,  0.        ,  0.8934334 ,
	 *         -0.9505956 ,  0.62891196, -0.68607416,  0.27096126, -1.78036584,
	 *          0.80361539, -0.86077759,  0.54650025, -0.60366245, -1.24776901,
	 *          1.19060681]]), array([-0.0571622]))
	 *
	 * Cross-validation score mean 49.114% S.D. 3.184%
	 * (fullset) PERANS acc/prec/rcl/F2 = 0.741/1.000/0.262/0.308, @70 prec/rcl/F2 = 1.000/0.079/0.097, PERQ avail 0.695, any good = [0.504], simple 0.485
	 */
	public static double weights[] = {
	         0.19062282,  0.        ,  0.69901299,  0.        ,  0.43837462,
		-0.2314449 ,  0.17428271, -0.2314449 , -0.26978136,  0.21261916,
		 0.06630427, -0.12346647, -0.29989136,  0.24272916,  0.1931069 ,
		-0.2502691 , -0.50203657,  0.44487437, -0.20081361,  0.14365142,
		-1.04782028, -0.17413659,  0.26842728, -0.32558948,  0.12884411,
		-0.18600631,  1.84164989,  0.19841181, -0.14553963,  0.19841181,
		 0.04365706, -0.10081926,  0.02087168,  0.        ,  0.8934334 ,
		-0.9505956 ,  0.62891196, -0.68607416,  0.27096126, -1.78036584,
		 0.80361539, -0.86077759,  0.54650025, -0.60366245, -1.24776901,
		 1.19060681,
	};
	public static double intercept = -0.0571622;

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
