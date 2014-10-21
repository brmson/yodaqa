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
	 * 430 answersets, 82510 answers
	 * + Cross-validation:
	 * (test) PERANS acc/prec/rcl/F2 = 0.740/0.054/0.622/0.200, @70 prec/rcl/F2 = 0.089/0.369/0.226, PERQ avail 0.660, any good = [0.495], simple 0.463
	 * (test) PERANS acc/prec/rcl/F2 = 0.761/0.052/0.571/0.191, @70 prec/rcl/F2 = 0.089/0.307/0.206, PERQ avail 0.712, any good = [0.428], simple 0.445
	 * (test) PERANS acc/prec/rcl/F2 = 0.777/0.062/0.579/0.217, @70 prec/rcl/F2 = 0.120/0.332/0.245, PERQ avail 0.702, any good = [0.483], simple 0.424
	 * (test) PERANS acc/prec/rcl/F2 = 0.768/0.062/0.577/0.216, @70 prec/rcl/F2 = 0.107/0.356/0.243, PERQ avail 0.721, any good = [0.557], simple 0.441
	 * (test) PERANS acc/prec/rcl/F2 = 0.722/0.050/0.614/0.188, @70 prec/rcl/F2 = 0.096/0.359/0.232, PERQ avail 0.702, any good = [0.458], simple 0.429
	 * (test) PERANS acc/prec/rcl/F2 = 0.765/0.060/0.592/0.214, @70 prec/rcl/F2 = 0.111/0.336/0.239, PERQ avail 0.688, any good = [0.515], simple 0.419
	 * (test) PERANS acc/prec/rcl/F2 = 0.756/0.065/0.571/0.223, @70 prec/rcl/F2 = 0.107/0.338/0.236, PERQ avail 0.707, any good = [0.570], simple 0.465
	 * (test) PERANS acc/prec/rcl/F2 = 0.755/0.061/0.624/0.219, @70 prec/rcl/F2 = 0.110/0.352/0.244, PERQ avail 0.702, any good = [0.480], simple 0.479
	 * (test) PERANS acc/prec/rcl/F2 = 0.708/0.056/0.642/0.207, @70 prec/rcl/F2 = 0.109/0.371/0.251, PERQ avail 0.698, any good = [0.558], simple 0.507
	 * (test) PERANS acc/prec/rcl/F2 = 0.731/0.057/0.616/0.208, @70 prec/rcl/F2 = 0.100/0.348/0.233, PERQ avail 0.730, any good = [0.499], simple 0.460
	 * Cross-validation score mean 50.435% S.D. 4.387%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.749/1.000/0.258/0.303, @70 prec/rcl/F2 = 1.000/0.084/0.103, PERQ avail 0.702, any good = [0.535], simple 0.454
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.113956,  0.007683,  0.000000, /*                  occurences d01:  0.121640 */
		/*              resultLogScore @,%,! */  0.692689,  0.080762,  0.000000, /*              resultLogScore d01:  0.773451 */
		/*             passageLogScore @,%,! */ -0.188406,  0.560341,  0.005742, /*             passageLogScore d01:  0.366193 */
		/*                   originPsg @,%,! */ -0.111368, -0.315236,  0.005742, /*                   originPsg d01: -0.432346 */
		/*              originPsgFirst @,%,! */ -0.173945, -0.027609,  0.068319, /*              originPsgFirst d01: -0.269873 */
		/*                 originPsgNP @,%,! */  0.798918, -0.001841, -0.904543, /*                 originPsgNP d01:  1.701621 */
		/*                 originPsgNE @,%,! */  0.071900, -0.000993, -0.177525, /*                 originPsgNE d01:  0.248432 */
		/*        originPsgNPByLATSubj @,%,! */  0.224384,  0.011943, -0.330009, /*        originPsgNPByLATSubj d01:  0.566337 */
		/*              originDocTitle @,%,! */  0.769964,  0.189524, -0.875590, /*              originDocTitle d01:  1.835078 */
		/*               originConcept @,%,! */  0.079691, -0.579622, -0.185317, /*               originConcept d01: -0.314614 */
		/*              originMultiple @,%,! */ -0.386534, -0.201084,  0.280908, /*              originMultiple d01: -0.868526 */
		/*                   spWordNet @,%,! */ -0.627892,  0.315158,  0.419244, /*                   spWordNet d01: -0.731978 */
		/*               LATQNoWordNet @,%,! */ -0.573869,  0.000000,  0.468243, /*               LATQNoWordNet d01: -1.042112 */
		/*               LATANoWordNet @,%,! */  0.205610, -0.284362, -0.311235, /*               LATANoWordNet d01:  0.232483 */
		/*              tyCorPassageSp @,%,! */  1.675900, -0.129062,  0.144770, /*              tyCorPassageSp d01:  1.402068 */
		/*            tyCorPassageDist @,%,! */  0.160466,  0.015170,  0.144770, /*            tyCorPassageDist d01:  0.030866 */
		/*          tyCorPassageInside @,%,! */  0.142355, -0.047543, -0.247980, /*          tyCorPassageInside d01:  0.342792 */
		/*                 simpleScore @,%,! */  0.003922,  0.085339,  0.000000, /*                 simpleScore d01:  0.089261 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000, -0.105625, /*                    LATFocus d01:  0.105625 */
		/*               LATFocusProxy @,%,! */ -1.080028,  0.150711,  0.974403, /*               LATFocusProxy d01: -1.903719 */
		/*                       LATNE @,%,! */ -0.295433, -0.156198, -0.721579, /*                       LATNE d01:  0.269948 */
		/*                 tyCorSpQHit @,%,! */ -0.365788,  0.479345,  0.260163, /*                 tyCorSpQHit d01: -0.146606 */
		/*                 tyCorSpAHit @,%,! */  1.240172, -0.580188, -1.345797, /*                 tyCorSpAHit d01:  2.005781 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000, -0.105625, /*             tyCorXHitAFocus d01:  0.105625 */
	};
	public static double intercept = -0.105625;

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
