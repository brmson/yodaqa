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
	 * (array([[ 0.24821101,  0.        ,  0.8010371 ,  0.        ,  0.67408491,
	 *         -0.36122794,  0.03792085, -0.36122794, -0.46096478,  0.13765769,
	 *         -0.17255208, -0.15075501, -0.77145857,  0.44815148, -0.01402672,
	 *         -0.30928037, -0.69940524,  0.37609815, -0.7121121 ,  0.00705248,
	 *         -0.56053079,  0.2372237 , -0.18833246, -0.13497463,  2.01442722,
	 *          0.12156538, -0.31071085,  0.12156538, -0.17102169, -0.1522854 ,
	 *         -0.00989544,  0.        ,  0.68084971, -1.0041568 ,  0.30286585,
	 *         -0.62617294,  0.62930236, -1.31746502,  0.82055717, -1.14386425,
	 *          0.40351879, -0.72682587, -1.55168953,  1.22838244]]), array([-0.32330709]))
	 *
	 * (testset) PERANS acc/prec/rcl/F2 = 0.766/0.061/0.505/0.206, @70 prec/rcl/F2 = 0.116/0.330/0.241, PERQ avail 0.698, any good = [0.556], simple 0.511
	 */
	public static double weights[] = {
		 0.24821101,  0.        ,  0.8010371 ,  0.        ,  0.67408491,
		-0.36122794,  0.03792085, -0.36122794, -0.46096478,  0.13765769,
		-0.17255208, -0.15075501, -0.77145857,  0.44815148, -0.01402672,
		-0.30928037, -0.69940524,  0.37609815,
		0, 0,
		-0.7121121 ,  0.00705248,
		-0.56053079,  0.2372237 , -0.18833246, -0.13497463,  2.01442722,
		 0.12156538, -0.31071085,  0.12156538, -0.17102169, -0.1522854 ,
		-0.00989544,  0.        ,  0.68084971, -1.0041568 ,  0.30286585,
		-0.62617294,  0.62930236, -1.31746502,  0.82055717, -1.14386425,
		 0.40351879, -0.72682587, -1.55168953,  1.22838244,
	};
	public static double intercept = -0.32330709;

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
