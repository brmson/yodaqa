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
	 * (test) PERANS acc/prec/rcl/F2 = 0.750/0.061/0.654/0.222, @70 prec/rcl/F2 = 0.101/0.427/0.259, PERQ avail 0.688, any good = [0.518], simple 0.513
	 * (test) PERANS acc/prec/rcl/F2 = 0.724/0.057/0.665/0.212, @70 prec/rcl/F2 = 0.097/0.413/0.249, PERQ avail 0.679, any good = [0.523], simple 0.496
	 * (test) PERANS acc/prec/rcl/F2 = 0.728/0.055/0.627/0.203, @70 prec/rcl/F2 = 0.096/0.406/0.247, PERQ avail 0.716, any good = [0.523], simple 0.482
	 * (test) PERANS acc/prec/rcl/F2 = 0.751/0.061/0.588/0.216, @70 prec/rcl/F2 = 0.104/0.384/0.249, PERQ avail 0.698, any good = [0.582], simple 0.499
	 * (test) PERANS acc/prec/rcl/F2 = 0.766/0.068/0.591/0.232, @70 prec/rcl/F2 = 0.122/0.365/0.261, PERQ avail 0.716, any good = [0.575], simple 0.511
	 * (test) PERANS acc/prec/rcl/F2 = 0.750/0.057/0.612/0.207, @70 prec/rcl/F2 = 0.109/0.400/0.261, PERQ avail 0.735, any good = [0.525], simple 0.468
	 * (test) PERANS acc/prec/rcl/F2 = 0.780/0.065/0.591/0.225, @70 prec/rcl/F2 = 0.114/0.352/0.248, PERQ avail 0.693, any good = [0.513], simple 0.460
	 * (test) PERANS acc/prec/rcl/F2 = 0.751/0.057/0.603/0.208, @70 prec/rcl/F2 = 0.109/0.408/0.264, PERQ avail 0.693, any good = [0.465], simple 0.475
	 * (test) PERANS acc/prec/rcl/F2 = 0.752/0.057/0.597/0.206, @70 prec/rcl/F2 = 0.097/0.374/0.238, PERQ avail 0.698, any good = [0.518], simple 0.485
	 * (test) PERANS acc/prec/rcl/F2 = 0.772/0.064/0.582/0.223, @70 prec/rcl/F2 = 0.120/0.370/0.262, PERQ avail 0.716, any good = [0.505], simple 0.443
	 * Cross-validation score mean 52.469% S.D. 3.166%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.761/1.000/0.246/0.289, @70 prec/rcl/F2 = 1.000/0.082/0.100, PERQ avail 0.702, any good = [0.550], simple 0.486
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.094692, -0.011328,  0.000000, /*                  occurences d01:  0.083364 */
		/*              resultLogScore @,%,! */  0.618033,  0.088573,  0.000000, /*              resultLogScore d01:  0.706606 */
		/*             passageLogScore @,%,! */ -0.105699,  0.537790,  0.131343, /*             passageLogScore d01:  0.300748 */
		/*                   originPsg @,%,! */ -0.033611, -0.260460,  0.131343, /*                   originPsg d01: -0.425414 */
		/*              originPsgFirst @,%,! */ -0.020116, -0.057780,  0.117848, /*              originPsgFirst d01: -0.195745 */
		/*                 originPsgNP @,%,! */  0.917653, -0.035211, -0.819921, /*                 originPsgNP d01:  1.702363 */
		/*                 originPsgNE @,%,! */  0.421115, -0.160128, -0.323383, /*                 originPsgNE d01:  0.584370 */
		/*        originPsgNPByLATSubj @,%,! */  0.224532,  0.034212, -0.126800, /*        originPsgNPByLATSubj d01:  0.385544 */
		/*              originDocTitle @,%,! */  0.672615,  0.270278, -0.574883, /*              originDocTitle d01:  1.517776 */
		/*               originConcept @,%,! */  0.017494, -0.380773,  0.080238, /*               originConcept d01: -0.443517 */
		/*      originConceptBySubject @,%,! */  0.152242,  0.045455, -0.054510, /*      originConceptBySubject d01:  0.252207 */
		/*        originConceptByFocus @,%,! */  0.753869, -0.848592, -0.656136, /*        originConceptByFocus d01:  0.561413 */
		/*           originConceptByNE @,%,! */  0.319187, -0.333489, -0.221455, /*           originConceptByNE d01:  0.207154 */
		/*              originMultiple @,%,! */ -0.350413, -0.184295,  0.448145, /*              originMultiple d01: -0.982854 */
		/*                   spWordNet @,%,! */ -0.984127,  0.270048,  0.964258, /*                   spWordNet d01: -1.678337 */
		/*               LATQNoWordNet @,%,! */ -0.438205,  0.000000,  0.535937, /*               LATQNoWordNet d01: -0.974142 */
		/*               LATANoWordNet @,%,! */  0.379642, -0.377921, -0.281910, /*               LATANoWordNet d01:  0.283632 */
		/*              tyCorPassageSp @,%,! */  1.582786, -0.112104,  0.140675, /*              tyCorPassageSp d01:  1.330007 */
		/*            tyCorPassageDist @,%,! */  0.345435, -0.007750,  0.140675, /*            tyCorPassageDist d01:  0.197011 */
		/*          tyCorPassageInside @,%,! */  0.197902, -0.038388, -0.100170, /*          tyCorPassageInside d01:  0.259685 */
		/*                 simpleScore @,%,! */  0.005073,  0.104157,  0.000000, /*                 simpleScore d01:  0.109230 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000,  0.097732, /*                    LATFocus d01: -0.097732 */
		/*               LATFocusProxy @,%,! */ -0.944135,  0.135189,  1.041867, /*               LATFocusProxy d01: -1.850814 */
		/*                       LATNE @,%,! */ -0.242673, -0.006968, -0.310887, /*                       LATNE d01:  0.061246 */
		/*                  LATDBpType @,%,! */  0.118883, -0.412860, -0.350475, /*                  LATDBpType d01:  0.056498 */
		/*                 tyCorSpQHit @,%,! */  0.978682, -0.088404, -0.880950, /*                 tyCorSpQHit d01:  1.771227 */
		/*                 tyCorSpAHit @,%,! */  0.221266, -0.114662, -0.123534, /*                 tyCorSpAHit d01:  0.230138 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000,  0.097732, /*             tyCorXHitAFocus d01: -0.097732 */
		/*                 tyCorAFocus @,%,! */  0.368673,  0.087835, -0.270941, /*                 tyCorAFocus d01:  0.727450 */
		/*                    tyCorANE @,%,! */  1.147002, -0.284431, -1.049270, /*                    tyCorANE d01:  1.911842 */
		/*                   tyCorADBp @,%,! */  1.014437, -0.117579, -0.916705, /*                   tyCorADBp d01:  1.813564 */
	};
	public static double intercept = 0.097732;

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
