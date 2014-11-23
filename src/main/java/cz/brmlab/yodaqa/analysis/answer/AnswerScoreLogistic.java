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
	 * 430 answersets, 91421 answers
	 * + Cross-validation:
	 * (test) PERANS acc/prec/rcl/F2 = 0.798/0.064/0.589/0.222, @70 prec/rcl/F2 = 0.103/0.329/0.228, PERQ avail 0.716, any good = [0.519], simple 0.452
	 * (test) PERANS acc/prec/rcl/F2 = 0.767/0.053/0.588/0.194, @70 prec/rcl/F2 = 0.091/0.344/0.221, PERQ avail 0.712, any good = [0.559], simple 0.517
	 * (test) PERANS acc/prec/rcl/F2 = 0.755/0.060/0.577/0.213, @70 prec/rcl/F2 = 0.105/0.368/0.245, PERQ avail 0.753, any good = [0.537], simple 0.529
	 * (test) PERANS acc/prec/rcl/F2 = 0.771/0.059/0.603/0.213, @70 prec/rcl/F2 = 0.099/0.395/0.248, PERQ avail 0.735, any good = [0.486], simple 0.459
	 * (test) PERANS acc/prec/rcl/F2 = 0.774/0.064/0.598/0.224, @70 prec/rcl/F2 = 0.106/0.395/0.256, PERQ avail 0.702, any good = [0.514], simple 0.512
	 * (test) PERANS acc/prec/rcl/F2 = 0.759/0.060/0.609/0.214, @70 prec/rcl/F2 = 0.096/0.379/0.238, PERQ avail 0.749, any good = [0.472], simple 0.503
	 * (test) PERANS acc/prec/rcl/F2 = 0.794/0.063/0.560/0.218, @70 prec/rcl/F2 = 0.109/0.349/0.242, PERQ avail 0.712, any good = [0.462], simple 0.499
	 * (test) PERANS acc/prec/rcl/F2 = 0.757/0.064/0.631/0.227, @70 prec/rcl/F2 = 0.117/0.354/0.252, PERQ avail 0.716, any good = [0.541], simple 0.537
	 * (test) PERANS acc/prec/rcl/F2 = 0.759/0.059/0.623/0.215, @70 prec/rcl/F2 = 0.098/0.371/0.238, PERQ avail 0.758, any good = [0.490], simple 0.499
	 * (test) PERANS acc/prec/rcl/F2 = 0.765/0.062/0.599/0.219, @70 prec/rcl/F2 = 0.100/0.397/0.249, PERQ avail 0.730, any good = [0.492], simple 0.515
	 * Cross-validation score mean 50.732% S.D. 3.026%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.771/1.000/0.235/0.278, @70 prec/rcl/F2 = 1.000/0.087/0.107, PERQ avail 0.730, any good = [0.514], simple 0.507
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */ -0.008879, -0.045151,  0.000000, /*                  occurences d01: -0.054030 */
		/*              resultLogScore @,%,! */  0.584779,  0.062053,  0.000000, /*              resultLogScore d01:  0.646832 */
		/*             passageLogScore @,%,! */ -0.247473,  0.646720,  0.105808, /*             passageLogScore d01:  0.293439 */
		/*                   originPsg @,%,! */  0.039864, -0.526413,  0.105808, /*                   originPsg d01: -0.592357 */
		/*              originPsgFirst @,%,! */  0.154614, -0.172186, -0.008943, /*              originPsgFirst d01: -0.008628 */
		/*                 originPsgNP @,%,! */  0.371705,  0.317313, -0.226034, /*                 originPsgNP d01:  0.915051 */
		/*                 originPsgNE @,%,! */ -0.123216,  0.134434,  0.268888, /*                 originPsgNE d01: -0.257669 */
		/*        originPsgNPByLATSubj @,%,! */  0.369403, -0.018127, -0.223731, /*        originPsgNPByLATSubj d01:  0.575008 */
		/*           originPsgSurprise @,%,! */  0.069704, -0.002013,  0.075967, /*           originPsgSurprise d01: -0.008276 */
		/*              originDocTitle @,%,! */  0.680123,  0.120797, -0.534451, /*              originDocTitle d01:  1.335371 */
		/*           originDBpRelation @,%,! */  0.152542,  0.010033, -0.006870, /*           originDBpRelation d01:  0.169445 */
		/*               originConcept @,%,! */  0.048890, -0.349603,  0.096782, /*               originConcept d01: -0.397496 */
		/*      originConceptBySubject @,%,! */  0.420418, -0.111847, -0.274746, /*      originConceptBySubject d01:  0.583317 */
		/*          originConceptByLAT @,%,! */  0.390621, -0.570519, -0.244950, /*          originConceptByLAT d01:  0.065053 */
		/*           originConceptByNE @,%,! */  0.420908, -0.387536, -0.275236, /*           originConceptByNE d01:  0.308609 */
		/*              originMultiple @,%,! */ -0.030368, -0.220983,  0.176039, /*              originMultiple d01: -0.427390 */
		/*                   spWordNet @,%,! */  0.849036,  0.229409, -0.434128, /*                   spWordNet d01:  1.512573 */
		/*               LATQNoWordNet @,%,! */ -0.290598,  0.000000,  0.436270, /*               LATQNoWordNet d01: -0.726868 */
		/*               LATANoWordNet @,%,! */  0.166730, -0.056700, -0.021059, /*               LATANoWordNet d01:  0.131088 */
		/*              tyCorPassageSp @,%,! */  1.137046,  0.083670,  0.151173, /*              tyCorPassageSp d01:  1.069544 */
		/*            tyCorPassageDist @,%,! */  0.294209, -0.105392,  0.151173, /*            tyCorPassageDist d01:  0.037644 */
		/*          tyCorPassageInside @,%,! */ -0.076801,  0.158795,  0.222473, /*          tyCorPassageInside d01: -0.140480 */
		/*                 simpleScore @,%,! */  0.005399,  0.136480,  0.000000, /*                 simpleScore d01:  0.141879 */
		/*                       LATNE @,%,! */ -1.027160,  0.257871, -0.506170, /*                       LATNE d01: -0.263119 */
		/*                  LATDBpType @,%,! */  0.018946, -0.315474, -0.017254, /*                  LATDBpType d01: -0.279274 */
		/*                 LATQuantity @,%,! */ -0.180568, -0.080758,  0.326239, /*                 LATQuantity d01: -0.587565 */
		/*               LATQuantityCD @,%,! */  0.618630, -0.146441, -0.097604, /*               LATQuantityCD d01:  0.569793 */
		/*               LATWnInstance @,%,! */ -0.028913, -0.042799, -0.450328, /*               LATWnInstance d01:  0.378617 */
		/*              LATDBpRelation @,%,! */  0.152542,  0.010033, -0.006870, /*              LATDBpRelation d01:  0.169445 */
		/*                 tyCorSpQHit @,%,! */  0.384506, -0.042944, -0.238834, /*                 tyCorSpQHit d01:  0.580396 */
		/*                 tyCorSpAHit @,%,! */ -0.094533, -0.356991,  0.240204, /*                 tyCorSpAHit d01: -0.691729 */
		/*                    tyCorANE @,%,! */  1.193199, -0.106220, -1.047527, /*                    tyCorANE d01:  2.134506 */
		/*                   tyCorADBp @,%,! */  0.922138, -0.178681, -0.776467, /*                   tyCorADBp d01:  1.519925 */
		/*              tyCorAQuantity @,%,! */ -0.043880,  0.051712,  0.189551, /*              tyCorAQuantity d01: -0.181719 */
		/*            tyCorAQuantityCD @,%,! */ -0.749711,  0.798327,  0.895383, /*            tyCorAQuantityCD d01: -0.846767 */
		/*            tyCorAWnInstance @,%,! */  0.827247, -0.215050, -0.681575, /*            tyCorAWnInstance d01:  1.293772 */
		/*           tyCorADBpRelation @,%,! */ -0.886985,  0.214050,  1.032656, /*           tyCorADBpRelation d01: -1.705591 */
	};
	public static double intercept = 0.145671;

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
