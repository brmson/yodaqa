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
	 * (array([[ 0.21455015,  0.        ,  0.73177241,  0.        ,  0.6473394 ,
	 *         -0.30675121,  0.08585992, -0.30675121, -0.29468293,  0.07379164,
	 *          0.02586242, -0.2467537 , -0.49390687,  0.27301559,  0.18598053,
	 *         -0.40687182, -0.58846871,  0.36757742, -0.27277344,  0.05188216,
	 *         -0.92531711, -0.16488015, -0.64512994,  0.42423865,  0.07739563,
	 *         -0.29828691,  1.22743072,  0.15415349,  0.59389493,  0.15415349,
	 *         -0.07179225, -0.14909903,  0.04833571,  0.        ,  0.77053832,
	 *         -0.99142961,  0.57203072, -0.792922  ,  0.52859387, -1.58319834,
	 *          0.72337369, -0.94426497,  0.40765588, -0.62854717, -1.51270086,
	 *          1.29180957]]), array([-0.22089129]))
	 *
	 * (testset) PERANS acc/prec/rcl/F2 = 0.726/0.063/0.553/0.216, @70 prec/rcl/F2 = 0.117/0.353/0.251, PERQ avail 0.716, any good = [0.568], simple 0.480
	 */
	public static double weights[] = {
	         0.21455015,  0.        ,  0.73177241,  0.        ,  0.6473394 ,
		-0.30675121,  0.08585992, -0.30675121, -0.29468293,  0.07379164,
		 0.02586242, -0.2467537 , -0.49390687,  0.27301559,  0.18598053,
		-0.40687182, -0.58846871,  0.36757742, -0.27277344,  0.05188216,
		-0.92531711, -0.16488015, -0.64512994,  0.42423865,  0.07739563,
		-0.29828691,  1.22743072,  0.15415349,  0.59389493,  0.15415349,
		-0.07179225, -0.14909903,  0.04833571,  0.        ,  0.77053832,
		-0.99142961,  0.57203072, -0.792922  ,  0.52859387, -1.58319834,
		 0.72337369, -0.94426497,  0.40765588, -0.62854717, -1.51270086,
		 1.29180957,
	};
	public static double intercept = -0.22089129;

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
