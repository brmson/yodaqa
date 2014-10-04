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
	 * (array([[ 0.17959791,  0.        ,  0.65903807,  0.        ,  0.92587873,
	 *         -0.32063631,  0.16527139, -0.32063631, -0.26774901,  0.11238409,
	 *         -0.01942871, -0.1359362 , -0.5567014 ,  0.40133648,  0.04570035,
	 *         -0.20106527, -0.46506386,  0.30969894, -1.18882323, -0.18689809,
	 *         -0.58002462,  0.4246597 ,  0.09878737, -0.25415229,  0.70628254,
	 *          0.14660766,  0.37937881,  0.14660766,  0.16460848, -0.3199734 ,
	 *          0.03837274,  0.        ,  0.84146954, -0.99683446,  0.78269291,
	 *         -0.93805783,  0.4557038 , -1.70648078,  0.76137845, -0.91674336,
	 *          0.3652391 , -0.52060402, -1.11198024,  0.95661532]]), array([-0.15536492]))
	 *
	 * (testset) PERANS acc/prec/rcl/F2 = 0.726/0.057/0.571/0.205, @70 prec/rcl/F2 = 0.114/0.339/0.243, PERQ avail 0.679, any good = [0.556], simple 0.507
	 */
	public static double weights[] = {
		 0.17959791,  0.        ,  0.65903807,  0.        ,  0.92587873,
		-0.32063631,  0.16527139, -0.32063631, -0.26774901,  0.11238409,
		-0.01942871, -0.1359362 , -0.5567014 ,  0.40133648,  0.04570035,
		-0.20106527, -0.46506386,  0.30969894, -1.18882323, -0.18689809,
		-0.58002462,  0.4246597 ,  0.09878737, -0.25415229,  0.70628254,
		 0.14660766,  0.37937881,  0.14660766,  0.16460848, -0.3199734 ,
		 0.03837274,  0.        ,  0.84146954, -0.99683446,  0.78269291,
		-0.93805783,  0.4557038 , -1.70648078,  0.76137845, -0.91674336,
		 0.3652391 , -0.52060402, -1.11198024,  0.95661532
	};
	public static double intercept = -0.15536492;

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
