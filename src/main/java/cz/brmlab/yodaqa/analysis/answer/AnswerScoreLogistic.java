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
	 * (array([[ 0.2305034 ,  0.        ,  0.68277173,  0.        ,  0.60335226,
	 *         -0.25214261,  0.06732056, -0.25214261, -0.37807179,  0.19324974,
	 *         -0.01269023, -0.17213182, -0.68662168,  0.50179963,  0.16178059,
	 *         -0.34660264, -0.45714576,  0.27232371, -0.76230149, -0.21904971,
	 *         -0.62134309,  0.43652104, -0.2336461 ,  0.04882405,  1.50876384,
	 *          0.20468528, -0.23270901,  0.20468528,  0.01770639, -0.20252844,
	 *          0.02750212,  0.        ,  0.47669419, -0.66151624,  0.42931474,
	 *         -0.61413679, -0.23898986, -1.68250799,  0.66325937, -0.84808142,
	 *          0.43857594, -0.62339799, -1.19579198,  1.01096993]]), array([-0.18482205]))
	 *
	 * (testset) perans acc/prec/rcl/F2 = 0.739/0.051/0.531/0.184, @70 prec/rcl = [0.110]/0.335, perq avail 0.712, any good picked = 0.523, simple 0.477
	 */
	public static double weights[] = {
		 0.2305034 ,  0.        ,  0.68277173,  0.        ,  0.60335226,
		-0.25214261,  0.06732056, -0.25214261, -0.37807179,  0.19324974,
		-0.01269023, -0.17213182, -0.68662168,  0.50179963,  0.16178059,
		-0.34660264, -0.45714576,  0.27232371, -0.76230149, -0.21904971,
		-0.62134309,  0.43652104, -0.2336461 ,  0.04882405,  1.50876384,
		 0.20468528, -0.23270901,  0.20468528,  0.01770639, -0.20252844,
		 0.02750212,  0.        ,  0.47669419, -0.66151624,  0.42931474,
		-0.61413679, -0.23898986, -1.68250799,  0.66325937, -0.84808142,
		 0.43857594, -0.62339799, -1.19579198,  1.01096993
	};
	public static double intercept = 0.09030682;

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
