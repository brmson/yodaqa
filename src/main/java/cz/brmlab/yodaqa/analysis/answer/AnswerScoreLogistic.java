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
	 * (array([[ 0.17904531,  0.        ,  0.56477368,  0.        ,  0.017477  ,
	 *          -0.17086632,  0.26117313, -0.17086632, -0.19024857,  0.28055539,
	 *          -0.00306143,  0.09336825, -0.39608299,  0.48638981,  0.37138681,
	 *          -0.28107999, -0.24272425,  0.33303106, -1.17445066, -0.07558267,
	 *           0.36867132, -0.27836451,  0.02165901,  0.0686478 ,  1.14720362,
	 *           0.21294205, -0.17833509,  0.21294205,  0.42061173, -0.33030491,
	 *           0.02972627,  0.        ,  0.59033077, -0.50002396,  0.63492322,
	 *          -0.5446164 , -0.06628046, -1.56662507,  0.81008808, -0.71978126,
	 *           0.63167214, -0.54136532, -0.97976597,  1.07007279]]), array([ 0.09030682]))
	 */
	public static double weights[] = {
		 0.17904531,  0.        ,  0.56477368,  0.        ,  0.017477  ,
		-0.17086632,  0.26117313, -0.17086632, -0.19024857,  0.28055539,
		-0.00306143,  0.09336825, -0.39608299,  0.48638981,  0.37138681,
		-0.28107999, -0.24272425,  0.33303106, -1.17445066, -0.07558267,
		 0.36867132, -0.27836451,  0.02165901,  0.0686478 ,  1.14720362,
		 0.21294205, -0.17833509,  0.21294205,  0.42061173, -0.33030491,
		 0.02972627,  0.        ,  0.59033077, -0.50002396,  0.63492322,
		-0.5446164 , -0.06628046, -1.56662507,  0.81008808, -0.71978126,
		 0.63167214, -0.54136532, -0.97976597,  1.07007279,
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
