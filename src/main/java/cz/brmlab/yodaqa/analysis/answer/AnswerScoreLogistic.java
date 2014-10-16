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
	 * (test) PERANS acc/prec/rcl/F2 = 0.736/0.054/0.602/0.200, @70 prec/rcl/F2 = 0.094/0.354/0.228, PERQ avail 0.730, any good = [0.506], simple 0.457
	 * (test) PERANS acc/prec/rcl/F2 = 0.715/0.061/0.644/0.220, @70 prec/rcl/F2 = 0.107/0.361/0.245, PERQ avail 0.716, any good = [0.592], simple 0.489
	 * (test) PERANS acc/prec/rcl/F2 = 0.733/0.059/0.626/0.214, @70 prec/rcl/F2 = 0.113/0.348/0.246, PERQ avail 0.688, any good = [0.518], simple 0.446
	 * (test) PERANS acc/prec/rcl/F2 = 0.754/0.057/0.569/0.204, @70 prec/rcl/F2 = 0.105/0.338/0.234, PERQ avail 0.721, any good = [0.542], simple 0.499
	 * (test) PERANS acc/prec/rcl/F2 = 0.770/0.061/0.575/0.215, @70 prec/rcl/F2 = 0.113/0.333/0.239, PERQ avail 0.735, any good = [0.470], simple 0.406
	 * (test) PERANS acc/prec/rcl/F2 = 0.766/0.055/0.557/0.197, @70 prec/rcl/F2 = 0.104/0.328/0.229, PERQ avail 0.721, any good = [0.445], simple 0.421
	 * (test) PERANS acc/prec/rcl/F2 = 0.746/0.050/0.567/0.184, @70 prec/rcl/F2 = 0.085/0.322/0.207, PERQ avail 0.684, any good = [0.459], simple 0.470
	 * (test) PERANS acc/prec/rcl/F2 = 0.762/0.054/0.534/0.193, @70 prec/rcl/F2 = 0.089/0.327/0.213, PERQ avail 0.726, any good = [0.402], simple 0.453
	 * (test) PERANS acc/prec/rcl/F2 = 0.732/0.053/0.637/0.199, @70 prec/rcl/F2 = 0.095/0.364/0.232, PERQ avail 0.688, any good = [0.482], simple 0.443
	 * (test) PERANS acc/prec/rcl/F2 = 0.779/0.057/0.579/0.204, @70 prec/rcl/F2 = 0.091/0.339/0.219, PERQ avail 0.698, any good = [0.526], simple 0.461
	 * Cross-validation score mean 49.417% S.D. 5.132%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.748/1.000/0.258/0.303, @70 prec/rcl/F2 = 1.000/0.084/0.103, PERQ avail 0.702, any good = [0.538], simple 0.457
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.128231, -0.015201,  0.000000, /*                  occurences d01:  0.113031 */
		/*              resultLogScore @,%,! */  0.681929,  0.084972,  0.000000, /*              resultLogScore d01:  0.766901 */
		/*             passageLogScore @,%,! */ -0.227195,  0.573743, -0.142387, /*             passageLogScore d01:  0.488936 */
		/*                   originPsg @,%,! */  0.004744, -0.354732, -0.142387, /*                   originPsg d01: -0.207601 */
		/*              originPsgFirst @,%,! */ -0.190162, -0.022486,  0.052519, /*              originPsgFirst d01: -0.265166 */
		/*                 originPsgNP @,%,! */  0.353059, -0.157826, -0.490702, /*                 originPsgNP d01:  0.685935 */
		/*                 originPsgNE @,%,! */ -0.408738, -0.114308,  0.271095, /*                 originPsgNE d01: -0.794140 */
		/*              originDocTitle @,%,! */  0.445767,  0.102090, -0.583410, /*              originDocTitle d01:  1.131267 */
		/*               originConcept @,%,! */  0.068009, -0.581246, -0.205652, /*               originConcept d01: -0.307584 */
		  0, 0, 0,
		  0, 0, 0,
		  0, 0, 0,
		/*              originMultiple @,%,! */  0.083528, -0.128157, -0.221171, /*              originMultiple d01:  0.176543 */
		/*                   spWordNet @,%,! */ -0.628454,  0.307345,  0.407740, /*                   spWordNet d01: -0.728850 */
		/*               LATQNoWordNet @,%,! */ -0.591457,  0.000000,  0.453814, /*               LATQNoWordNet d01: -1.045271 */
		/*               LATANoWordNet @,%,! */  0.196689, -0.260574, -0.334332, /*               LATANoWordNet d01:  0.270447 */
		/*              tyCorPassageSp @,%,! */  1.739094, -0.136612,  0.131449, /*              tyCorPassageSp d01:  1.471033 */
		/*            tyCorPassageDist @,%,! */  0.298253,  0.014034,  0.131449, /*            tyCorPassageDist d01:  0.180838 */
		/*          tyCorPassageInside @,%,! */  0.022189, -0.037580, -0.159832, /*          tyCorPassageInside d01:  0.144442 */
		/*                 simpleScore @,%,! */  0.003889,  0.092601,  0.000000, /*                 simpleScore d01:  0.096491 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000, -0.137643, /*                    LATFocus d01:  0.137643 */
		/*               LATFocusProxy @,%,! */ -1.050503,  0.150483,  0.912860, /*               LATFocusProxy d01: -1.812880 */
		/*                       LATNE @,%,! */ -0.297917, -0.130084, -0.726454, /*                       LATNE d01:  0.298454 */
		/*                 tyCorSpQHit @,%,! */ -0.386865,  0.484647,  0.249222, /*                 tyCorSpQHit d01: -0.151440 */
		/*                 tyCorSpAHit @,%,! */  1.212500, -0.573632, -1.350143, /*                 tyCorSpAHit d01:  1.989012 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000, -0.137643, /*             tyCorXHitAFocus d01:  0.137643 */
	};
	public static double intercept = -0.137643;

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
