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
	 * 430 answersets, 82157 answers
	 * + Cross-validation:
	 * (test) PERANS acc/prec/rcl/F2 = 0.761/0.066/0.621/0.231, @70 prec/rcl/F2 = 0.118/0.384/0.265, PERQ avail 0.716, any good = [0.536], simple 0.545
	 * (test) PERANS acc/prec/rcl/F2 = 0.788/0.068/0.602/0.233, @70 prec/rcl/F2 = 0.113/0.396/0.264, PERQ avail 0.730, any good = [0.514], simple 0.520
	 * (test) PERANS acc/prec/rcl/F2 = 0.777/0.064/0.610/0.226, @70 prec/rcl/F2 = 0.108/0.414/0.264, PERQ avail 0.712, any good = [0.520], simple 0.475
	 * (test) PERANS acc/prec/rcl/F2 = 0.758/0.066/0.613/0.230, @70 prec/rcl/F2 = 0.111/0.401/0.264, PERQ avail 0.749, any good = [0.497], simple 0.555
	 * (test) PERANS acc/prec/rcl/F2 = 0.771/0.064/0.627/0.228, @70 prec/rcl/F2 = 0.101/0.409/0.254, PERQ avail 0.721, any good = [0.485], simple 0.498
	 * (test) PERANS acc/prec/rcl/F2 = 0.780/0.069/0.621/0.240, @70 prec/rcl/F2 = 0.120/0.393/0.270, PERQ avail 0.772, any good = [0.528], simple 0.540
	 * (test) PERANS acc/prec/rcl/F2 = 0.773/0.059/0.628/0.216, @70 prec/rcl/F2 = 0.100/0.390/0.247, PERQ avail 0.735, any good = [0.511], simple 0.530
	 * (test) PERANS acc/prec/rcl/F2 = 0.740/0.061/0.696/0.227, @70 prec/rcl/F2 = 0.112/0.478/0.290, PERQ avail 0.735, any good = [0.531], simple 0.503
	 * (test) PERANS acc/prec/rcl/F2 = 0.794/0.059/0.542/0.206, @70 prec/rcl/F2 = 0.095/0.356/0.230, PERQ avail 0.684, any good = [0.490], simple 0.491
	 * (test) PERANS acc/prec/rcl/F2 = 0.790/0.068/0.606/0.235, @70 prec/rcl/F2 = 0.109/0.392/0.258, PERQ avail 0.679, any good = [0.568], simple 0.573
	 * Cross-validation score mean 51.798% S.D. 2.345%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.783/1.000/0.224/0.266, @70 prec/rcl/F2 = 1.000/0.087/0.106, PERQ avail 0.714, any good = [0.534], simple 0.521
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.068484,  0.063052,  0.000000, /*                  occurences d01:  0.131536 */
		/*              resultLogScore @,%,! */  0.558189,  0.105028,  0.000000, /*              resultLogScore d01:  0.663216 */
		/*             passageLogScore @,%,! */ -0.365479,  0.665416,  0.368730, /*             passageLogScore d01: -0.068792 */
		/*                   originPsg @,%,! */ -0.176909, -0.318358,  0.368730, /*                   originPsg d01: -0.863997 */
		/*              originPsgFirst @,%,! */  0.145710, -0.166546,  0.046111, /*              originPsgFirst d01: -0.066947 */
		/*                 originPsgNP @,%,! */  0.832622,  0.044965, -0.640801, /*                 originPsgNP d01:  1.518388 */
		/*                 originPsgNE @,%,! */ -0.027364,  0.190030,  0.219186, /*                 originPsgNE d01: -0.056520 */
		/*        originPsgNPByLATSubj @,%,! */  0.263181, -0.000718, -0.071359, /*        originPsgNPByLATSubj d01:  0.333823 */
		/*              originDocTitle @,%,! */  0.663952,  0.238519, -0.472131, /*              originDocTitle d01:  1.374602 */
		/*               originConcept @,%,! */  0.069722, -0.335337,  0.122099, /*               originConcept d01: -0.387714 */
		/*      originConceptBySubject @,%,! */  0.282469,  0.001216, -0.090647, /*      originConceptBySubject d01:  0.374332 */
		/*          originConceptByLAT @,%,! */  0.282441, -0.513327, -0.090619, /*          originConceptByLAT d01: -0.140268 */
		/*           originConceptByNE @,%,! */  0.363112, -0.332985, -0.171291, /*           originConceptByNE d01:  0.201418 */
		/*              originMultiple @,%,! */ -0.274444, -0.196497,  0.466265, /*              originMultiple d01: -0.937206 */
		/*                   spWordNet @,%,! */  0.973418,  0.348325, -0.647965, /*                   spWordNet d01:  1.969708 */
		/*               LATQNoWordNet @,%,! */ -0.316251,  0.000000,  0.508072, /*               LATQNoWordNet d01: -0.824323 */
		/*               LATANoWordNet @,%,! */  0.304966,  0.044726, -0.113145, /*               LATANoWordNet d01:  0.462836 */
		/*              tyCorPassageSp @,%,! */  1.951216, -0.075691,  0.168209, /*              tyCorPassageSp d01:  1.707317 */
		/*            tyCorPassageDist @,%,! */ -0.127870, -0.012452,  0.168209, /*            tyCorPassageDist d01: -0.308531 */
		/*          tyCorPassageInside @,%,! */  0.169031,  0.035795,  0.022791, /*          tyCorPassageInside d01:  0.182035 */
		/*                 simpleScore @,%,! */  0.012704,  0.117876,  0.000000, /*                 simpleScore d01:  0.130581 */
		/*                       LATNE @,%,! */ -0.847592,  0.221038, -0.389412, /*                       LATNE d01: -0.237143 */
		/*                  LATDBpType @,%,! */  0.021856, -0.338505, -0.025800, /*                  LATDBpType d01: -0.290850 */
		/*                 LATQuantity @,%,! */  0.301338, -0.223847,  0.269772, /*                 LATQuantity d01: -0.192280 */
		/*               LATQuantityCD @,%,! */  0.593977,  0.097090,  0.056738, /*               LATQuantityCD d01:  0.634329 */
		/*               LATWnInstance @,%,! */ -0.039641, -0.022333, -0.939943, /*               LATWnInstance d01:  0.877968 */
		/*                 tyCorSpQHit @,%,! */  0.357493, -0.050686, -0.165672, /*                 tyCorSpQHit d01:  0.472479 */
		/*                 tyCorSpAHit @,%,! */ -0.048427, -0.476564,  0.240248, /*                 tyCorSpAHit d01: -0.765239 */
		/*                    tyCorANE @,%,! */  1.107546, -0.075256, -0.915725, /*                    tyCorANE d01:  1.948015 */
		/*                   tyCorADBp @,%,! */  0.897335, -0.199430, -0.705513, /*                   tyCorADBp d01:  1.403418 */
		/*              tyCorAQuantity @,%,! */ -0.998539,  0.886697,  1.190361, /*              tyCorAQuantity d01: -1.302204 */
		/*            tyCorAWnInstance @,%,! */  0.895269, -0.293162, -0.703448, /*            tyCorAWnInstance d01:  1.305554 */
	};
	public static double intercept = 0.191821;

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
