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
	 * (test) PERANS acc/prec/rcl/F2 = 0.781/0.064/0.618/0.228, @70 prec/rcl/F2 = 0.105/0.403/0.257, PERQ avail 0.712, any good = [0.533], simple 0.533
	 * (test) PERANS acc/prec/rcl/F2 = 0.790/0.066/0.563/0.225, @70 prec/rcl/F2 = 0.106/0.325/0.230, PERQ avail 0.726, any good = [0.531], simple 0.506
	 * (test) PERANS acc/prec/rcl/F2 = 0.754/0.058/0.590/0.208, @70 prec/rcl/F2 = 0.094/0.375/0.234, PERQ avail 0.753, any good = [0.542], simple 0.473
	 * (test) PERANS acc/prec/rcl/F2 = 0.738/0.062/0.638/0.224, @70 prec/rcl/F2 = 0.109/0.376/0.252, PERQ avail 0.740, any good = [0.477], simple 0.556
	 * (test) PERANS acc/prec/rcl/F2 = 0.760/0.060/0.632/0.216, @70 prec/rcl/F2 = 0.105/0.391/0.253, PERQ avail 0.740, any good = [0.499], simple 0.454
	 * (test) PERANS acc/prec/rcl/F2 = 0.757/0.053/0.587/0.196, @70 prec/rcl/F2 = 0.083/0.360/0.215, PERQ avail 0.740, any good = [0.479], simple 0.453
	 * (test) PERANS acc/prec/rcl/F2 = 0.760/0.060/0.594/0.214, @70 prec/rcl/F2 = 0.102/0.353/0.237, PERQ avail 0.726, any good = [0.493], simple 0.525
	 * (test) PERANS acc/prec/rcl/F2 = 0.728/0.054/0.648/0.203, @70 prec/rcl/F2 = 0.087/0.399/0.232, PERQ avail 0.730, any good = [0.506], simple 0.501
	 * (test) PERANS acc/prec/rcl/F2 = 0.760/0.055/0.589/0.201, @70 prec/rcl/F2 = 0.084/0.326/0.207, PERQ avail 0.763, any good = [0.490], simple 0.512
	 * (test) PERANS acc/prec/rcl/F2 = 0.725/0.052/0.662/0.197, @70 prec/rcl/F2 = 0.082/0.415/0.229, PERQ avail 0.698, any good = [0.510], simple 0.496
	 * Cross-validation score mean 50.590% S.D. 2.173%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.768/1.000/0.239/0.282, @70 prec/rcl/F2 = 1.000/0.086/0.105, PERQ avail 0.730, any good = [0.537], simple 0.507
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */ -0.005834, -0.044761,  0.000000, /*                  occurences d01: -0.050595 */
		/*              resultLogScore @,%,! */  0.581086,  0.050365,  0.000000, /*              resultLogScore d01:  0.631450 */
		/*             passageLogScore @,%,! */ -0.270680,  0.663811,  0.138971, /*             passageLogScore d01:  0.254160 */
		/*                   originPsg @,%,! */ -0.078966, -0.476769,  0.138971, /*                   originPsg d01: -0.694705 */
		/*              originPsgFirst @,%,! */  0.164062, -0.212593, -0.104057, /*              originPsgFirst d01:  0.055526 */
		/*                 originPsgNP @,%,! */  0.355688,  0.239074, -0.295683, /*                 originPsgNP d01:  0.890445 */
		/*                 originPsgNE @,%,! */ -0.271789,  0.168260,  0.331794, /*                 originPsgNE d01: -0.435323 */
		/*        originPsgNPByLATSubj @,%,! */  0.324518, -0.022104, -0.264513, /*        originPsgNPByLATSubj d01:  0.566927 */
		/*           originPsgSurprise @,%,! */  0.076559, -0.039036, -0.016554, /*           originPsgSurprise d01:  0.054077 */
		/*              originDocTitle @,%,! */  0.544625,  0.137308, -0.484619, /*              originDocTitle d01:  1.166552 */
		/*           originDBpRelation @,%,! */ -0.001913,  0.032164,  0.061919, /*           originDBpRelation d01: -0.031668 */
		/*               originConcept @,%,! */  0.011656, -0.341900,  0.048350, /*               originConcept d01: -0.378595 */
		/*      originConceptBySubject @,%,! */  0.409593, -0.134526, -0.349588, /*      originConceptBySubject d01:  0.624656 */
		/*          originConceptByLAT @,%,! */  0.428073, -0.652422, -0.368067, /*          originConceptByLAT d01:  0.143718 */
		/*           originConceptByNE @,%,! */  0.359925, -0.371964, -0.299920, /*           originConceptByNE d01:  0.287881 */
		/*              originMultiple @,%,! */ -0.041117, -0.208503,  0.101123, /*              originMultiple d01: -0.350743 */
		/*                   spWordNet @,%,! */  0.778054,  0.224121, -0.634467, /*                   spWordNet d01:  1.636642 */
		/*               LATQNoWordNet @,%,! */ -0.349179,  0.000000,  0.409184, /*               LATQNoWordNet d01: -0.758363 */
		/*               LATANoWordNet @,%,! */  0.305472, -0.163322, -0.245467, /*               LATANoWordNet d01:  0.387616 */
		/*              tyCorPassageSp @,%,! */  1.141412,  0.100099,  0.144927, /*              tyCorPassageSp d01:  1.096584 */
		/*            tyCorPassageDist @,%,! */  0.279489, -0.126611,  0.144927, /*            tyCorPassageDist d01:  0.007952 */
		/*          tyCorPassageInside @,%,! */ -0.068764,  0.132943,  0.128769, /*          tyCorPassageInside d01: -0.064591 */
		/*                 simpleScore @,%,! */  0.004457,  0.140011,  0.000000, /*                 simpleScore d01:  0.144468 */
		/*                       LATNE @,%,! */ -0.273137,  0.270370,  0.333143, /*                       LATNE d01: -0.335909 */
		/*                  LATDBpType @,%,! */  0.755053, -0.748230, -0.695048, /*                  LATDBpType d01:  0.701871 */
		/*                 LATQuantity @,%,! */ -0.194094, -0.080593,  0.254099, /*                 LATQuantity d01: -0.528786 */
		/*               LATQuantityCD @,%,! */  0.666993, -0.246150, -0.606987, /*               LATQuantityCD d01:  1.027829 */
		/*               LATWnInstance @,%,! */  0.483584, -0.184357, -0.423578, /*               LATWnInstance d01:  0.722805 */
		/*              LATDBpRelation @,%,! */ -0.001913,  0.032164,  0.061919, /*              LATDBpRelation d01: -0.031668 */
		/*                 tyCorSpQHit @,%,! */  0.355222, -0.046836, -0.295216, /*                 tyCorSpQHit d01:  0.603602 */
		/*                 tyCorSpAHit @,%,! */ -0.236629, -0.354358,  0.296635, /*                 tyCorSpAHit d01: -0.887622 */
		/*                    tyCorANE @,%,! */  1.113358, -0.080440, -1.053353, /*                    tyCorANE d01:  2.086272 */
		/*                   tyCorADBp @,%,! */  0.814672, -0.158721, -0.754666, /*                   tyCorADBp d01:  1.410617 */
		/*              tyCorAQuantity @,%,! */ -0.042166,  0.045706,  0.102172, /*              tyCorAQuantity d01: -0.098631 */
		/*            tyCorAQuantityCD @,%,! */ -0.901426,  0.835997,  0.961431, /*            tyCorAQuantityCD d01: -1.026860 */
		/*            tyCorAWnInstance @,%,! */ -0.630471,  0.243413,  0.690476, /*            tyCorAWnInstance d01: -1.077534 */
		/*           tyCorADBpRelation @,%,! */ -0.261035,  0.189828,  0.321040, /*           tyCorADBpRelation d01: -0.392247 */
	};
	public static double intercept = 0.060005;

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
