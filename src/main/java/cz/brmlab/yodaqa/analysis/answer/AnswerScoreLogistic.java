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
	 *
	 * 430 answersets, 82153 answers
	 * + Cross-validation:
	 * (test) PERANS acc/prec/rcl/F2 = 0.774/0.060/0.595/0.213, @70 prec/rcl/F2 = 0.106/0.367/0.246, PERQ avail 0.674, any good = [0.530], simple 0.543
	 * (test) PERANS acc/prec/rcl/F2 = 0.767/0.060/0.609/0.214, @70 prec/rcl/F2 = 0.105/0.355/0.240, PERQ avail 0.698, any good = [0.540], simple 0.469
	 * (test) PERANS acc/prec/rcl/F2 = 0.761/0.061/0.618/0.218, @70 prec/rcl/F2 = 0.114/0.389/0.263, PERQ avail 0.730, any good = [0.559], simple 0.469
	 * (test) PERANS acc/prec/rcl/F2 = 0.742/0.059/0.614/0.213, @70 prec/rcl/F2 = 0.099/0.375/0.240, PERQ avail 0.735, any good = [0.545], simple 0.509
	 * (test) PERANS acc/prec/rcl/F2 = 0.751/0.061/0.650/0.222, @70 prec/rcl/F2 = 0.115/0.406/0.269, PERQ avail 0.688, any good = [0.542], simple 0.511
	 * (test) PERANS acc/prec/rcl/F2 = 0.785/0.056/0.584/0.203, @70 prec/rcl/F2 = 0.099/0.311/0.217, PERQ avail 0.670, any good = [0.506], simple 0.430
	 * (test) PERANS acc/prec/rcl/F2 = 0.777/0.059/0.588/0.211, @70 prec/rcl/F2 = 0.109/0.362/0.247, PERQ avail 0.670, any good = [0.550], simple 0.525
	 * (test) PERANS acc/prec/rcl/F2 = 0.782/0.063/0.570/0.219, @70 prec/rcl/F2 = 0.115/0.378/0.259, PERQ avail 0.726, any good = [0.486], simple 0.449
	 * (test) PERANS acc/prec/rcl/F2 = 0.756/0.061/0.668/0.223, @70 prec/rcl/F2 = 0.115/0.395/0.266, PERQ avail 0.693, any good = [0.545], simple 0.434
	 * (test) PERANS acc/prec/rcl/F2 = 0.765/0.061/0.580/0.216, @70 prec/rcl/F2 = 0.108/0.361/0.246, PERQ avail 0.712, any good = [0.552], simple 0.455
	 * Cross-validation score mean 53.547% S.D. 2.168%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.761/1.000/0.246/0.290, @70 prec/rcl/F2 = 1.000/0.082/0.101, PERQ avail 0.702, any good = [0.558], simple 0.486
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.095230, -0.017494,  0.000000, /*                  occurences d01:  0.077736 */
		/*              resultLogScore @,%,! */  0.673473,  0.073534,  0.000000, /*              resultLogScore d01:  0.747006 */
		/*             passageLogScore @,%,! */ -0.153093,  0.561897, -0.020067, /*             passageLogScore d01:  0.428870 */
		/*                   originPsg @,%,! */  0.043824, -0.298870, -0.020067, /*                   originPsg d01: -0.234979 */
		/*              originPsgFirst @,%,! */ -0.052279, -0.054106,  0.076036, /*              originPsgFirst d01: -0.182422 */
		/*                 originPsgNP @,%,! */  0.417513, -0.155735, -0.393756, /*                 originPsgNP d01:  0.655534 */
		/*                 originPsgNE @,%,! */ -0.121248, -0.234971,  0.145005, /*                 originPsgNE d01: -0.501224 */
		/*              originDocTitle @,%,! */  0.359594,  0.171276, -0.335837, /*              originDocTitle d01:  0.866707 */
		/*               originConcept @,%,! */  0.157185, -0.568839, -0.133428, /*               originConcept d01: -0.278226 */
		/*              originMultiple @,%,! */  0.111693, -0.126041, -0.087936, /*              originMultiple d01:  0.073588 */
		/*                   spWordNet @,%,! */ -1.072416,  0.259126,  0.943165, /*                   spWordNet d01: -1.756455 */
		/*               LATQNoWordNet @,%,! */ -0.464669,  0.000000,  0.488426, /*               LATQNoWordNet d01: -0.953094 */
		/*               LATANoWordNet @,%,! */  0.348486, -0.369097, -0.324729, /*               LATANoWordNet d01:  0.304118 */
		/*              tyCorPassageSp @,%,! */  1.658120, -0.123740,  0.133011, /*              tyCorPassageSp d01:  1.401369 */
		/*            tyCorPassageDist @,%,! */  0.530460, -0.019715,  0.133011, /*            tyCorPassageDist d01:  0.377733 */
		/*          tyCorPassageInside @,%,! */  0.055857, -0.028630, -0.032100, /*          tyCorPassageInside d01:  0.059327 */
		/*                 simpleScore @,%,! */  0.007800,  0.095871,  0.000000, /*                 simpleScore d01:  0.103671 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000,  0.023757, /*                    LATFocus d01: -0.023757 */
		/*               LATFocusProxy @,%,! */ -0.930511,  0.129602,  0.954268, /*               LATFocusProxy d01: -1.755177 */
		/*                       LATNE @,%,! */ -0.227755,  0.017171, -0.252843, /*                       LATNE d01:  0.042259 */
		/*                  LATDBpType @,%,! */  0.124479, -0.432472, -0.357009, /*                  LATDBpType d01:  0.049017 */
		0, 0, 0,
		0, 0, 0,
		/*                 tyCorSpQHit @,%,! */  0.835041, -0.009297, -0.811284, /*                 tyCorSpQHit d01:  1.637028 */
		/*                 tyCorSpAHit @,%,! */  0.207209, -0.122028, -0.183452, /*                 tyCorSpAHit d01:  0.268634 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000,  0.023757, /*             tyCorXHitAFocus d01: -0.023757 */
		/*                 tyCorAFocus @,%,! */  0.353771,  0.065256, -0.330014, /*                 tyCorAFocus d01:  0.749040 */
		/*                    tyCorANE @,%,! */  1.101375, -0.279005, -1.077618, /*                    tyCorANE d01:  1.899987 */
		/*                   tyCorADBp @,%,! */  1.063003, -0.159892, -1.039246, /*                   tyCorADBp d01:  1.942358 */
	};
	public static double intercept = 0.023757;

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
