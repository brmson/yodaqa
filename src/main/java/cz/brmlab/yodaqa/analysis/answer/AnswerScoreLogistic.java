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
	 * 430 answersets, 83196 answers
	 * + Cross-validation:
	 * (test) PERANS acc/prec/rcl/F2 = 0.756/0.061/0.627/0.220, @70 prec/rcl/F2 = 0.113/0.368/0.253, PERQ avail 0.702, any good = [0.535], simple 0.482
	 * (test) PERANS acc/prec/rcl/F2 = 0.734/0.054/0.631/0.202, @70 prec/rcl/F2 = 0.101/0.373/0.242, PERQ avail 0.693, any good = [0.607], simple 0.485
	 * (test) PERANS acc/prec/rcl/F2 = 0.713/0.060/0.677/0.220, @70 prec/rcl/F2 = 0.111/0.425/0.272, PERQ avail 0.712, any good = [0.601], simple 0.484
	 * (test) PERANS acc/prec/rcl/F2 = 0.776/0.062/0.605/0.221, @70 prec/rcl/F2 = 0.118/0.337/0.246, PERQ avail 0.712, any good = [0.590], simple 0.475
	 * (test) PERANS acc/prec/rcl/F2 = 0.748/0.054/0.626/0.201, @70 prec/rcl/F2 = 0.102/0.353/0.236, PERQ avail 0.693, any good = [0.510], simple 0.495
	 * (test) PERANS acc/prec/rcl/F2 = 0.733/0.056/0.603/0.205, @70 prec/rcl/F2 = 0.104/0.384/0.250, PERQ avail 0.693, any good = [0.567], simple 0.501
	 * (test) PERANS acc/prec/rcl/F2 = 0.772/0.065/0.593/0.225, @70 prec/rcl/F2 = 0.107/0.337/0.235, PERQ avail 0.730, any good = [0.537], simple 0.493
	 * (test) PERANS acc/prec/rcl/F2 = 0.755/0.055/0.599/0.202, @70 prec/rcl/F2 = 0.101/0.340/0.231, PERQ avail 0.726, any good = [0.508], simple 0.450
	 * (test) PERANS acc/prec/rcl/F2 = 0.763/0.059/0.607/0.212, @70 prec/rcl/F2 = 0.099/0.378/0.241, PERQ avail 0.693, any good = [0.493], simple 0.482
	 * (test) PERANS acc/prec/rcl/F2 = 0.739/0.057/0.611/0.207, @70 prec/rcl/F2 = 0.104/0.352/0.238, PERQ avail 0.721, any good = [0.532], simple 0.493
	 * Cross-validation score mean 54.798% S.D. 3.873%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.756/1.000/0.251/0.295, @70 prec/rcl/F2 = 1.000/0.079/0.097, PERQ avail 0.705, any good = [0.560], simple 0.481
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.149846, -0.068276,  0.000000, /*                  occurences d01:  0.081569 */
		/*              resultLogScore @,%,! */  0.662312,  0.062269,  0.000000, /*              resultLogScore d01:  0.724581 */
		/*             passageLogScore @,%,! */ -0.338594,  0.680702,  0.111959, /*             passageLogScore d01:  0.230149 */
		/*                   originPsg @,%,! */ -0.130691, -0.294498,  0.111959, /*                   originPsg d01: -0.537149 */
		/*              originPsgFirst @,%,! */  0.184777, -0.321410, -0.203509, /*              originPsgFirst d01:  0.066876 */
		/*                 originPsgNP @,%,! */  1.212706, -0.380544, -1.231438, /*                 originPsgNP d01:  2.063601 */
		/*                 originPsgNE @,%,! */  0.106719, -0.070982, -0.125451, /*                 originPsgNE d01:  0.161188 */
		/*        originPsgNPByLATSubj @,%,! */  0.429551, -0.062475, -0.448283, /*        originPsgNPByLATSubj d01:  0.815359 */
		/*              originDocTitle @,%,! */  0.733032,  0.233109, -0.751764, /*              originDocTitle d01:  1.717905 */
		/*               originConcept @,%,! */ -0.082997, -0.372091,  0.064265, /*               originConcept d01: -0.519352 */
		/*      originConceptBySubject @,%,! */  0.050298,  0.080622, -0.069030, /*      originConceptBySubject d01:  0.199951 */
		/*          originConceptByLAT @,%,! */  0.365972, -0.674379, -0.384704, /*          originConceptByLAT d01:  0.076298 */
		/*           originConceptByNE @,%,! */  0.236927, -0.353802, -0.255659, /*           originConceptByNE d01:  0.138784 */
		/*              originMultiple @,%,! */ -0.272010, -0.209210,  0.253278, /*              originMultiple d01: -0.734498 */
		/*                   spWordNet @,%,! */ -0.256616,  0.276320,  0.353055, /*                   spWordNet d01: -0.333351 */
		/*               LATQNoWordNet @,%,! */ -0.540436,  0.000000,  0.521704, /*               LATQNoWordNet d01: -1.062140 */
		/*               LATANoWordNet @,%,! */  0.193372, -0.273040, -0.212104, /*               LATANoWordNet d01:  0.132435 */
		/*              tyCorPassageSp @,%,! */  1.375345, -0.082545,  0.233154, /*              tyCorPassageSp d01:  1.059645 */
		/*            tyCorPassageDist @,%,! */  0.102293, -0.004664,  0.233154, /*            tyCorPassageDist d01: -0.135525 */
		/*          tyCorPassageInside @,%,! */  0.454699, -0.111175, -0.473431, /*          tyCorPassageInside d01:  0.816955 */
		/*                 simpleScore @,%,! */  0.002448,  0.142758,  0.000000, /*                 simpleScore d01:  0.145205 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000, -0.018732, /*                    LATFocus d01:  0.018732 */
		/*               LATFocusProxy @,%,! */ -0.415918, -0.029919,  0.397187, /*               LATFocusProxy d01: -0.843024 */
		/*                       LATNE @,%,! */  0.313832, -0.237171, -0.274570, /*                       LATNE d01:  0.351231 */
		/*                  LATDBpType @,%,! */  0.135917, -0.469750, -0.367823, /*                  LATDBpType d01:  0.033990 */
		/*                 tyCorSpQHit @,%,! */  0.446943, -0.023425, -0.465675, /*                 tyCorSpQHit d01:  0.889193 */
		/*                 tyCorSpAHit @,%,! */  0.562664, -0.153553, -0.581396, /*                 tyCorSpAHit d01:  0.990508 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000, -0.018732, /*             tyCorXHitAFocus d01:  0.018732 */
		/*                 tyCorAFocus @,%,! */ -0.891365,  0.275197,  0.872633, /*                 tyCorAFocus d01: -1.488801 */
		/*                    tyCorANE @,%,! */  0.071281, -0.046000, -0.090013, /*                    tyCorANE d01:  0.115294 */
		/*                   tyCorADBp @,%,! */  0.490151, -0.245349, -0.508883, /*                   tyCorADBp d01:  0.753684 */
	};
	public static double intercept = -0.018732;

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
