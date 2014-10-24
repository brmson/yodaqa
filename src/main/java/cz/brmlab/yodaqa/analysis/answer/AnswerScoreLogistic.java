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
	 * (test) PERANS acc/prec/rcl/F2 = 0.798/0.058/0.533/0.202, @70 prec/rcl/F2 = 0.112/0.306/0.227, PERQ avail 0.707, any good = [0.477], simple 0.439
	 * (test) PERANS acc/prec/rcl/F2 = 0.758/0.056/0.623/0.205, @70 prec/rcl/F2 = 0.096/0.351/0.229, PERQ avail 0.702, any good = [0.520], simple 0.417
	 * (test) PERANS acc/prec/rcl/F2 = 0.771/0.060/0.545/0.208, @70 prec/rcl/F2 = 0.112/0.341/0.242, PERQ avail 0.730, any good = [0.524], simple 0.482
	 * (test) PERANS acc/prec/rcl/F2 = 0.751/0.058/0.610/0.209, @70 prec/rcl/F2 = 0.117/0.348/0.249, PERQ avail 0.707, any good = [0.500], simple 0.419
	 * (test) PERANS acc/prec/rcl/F2 = 0.744/0.046/0.577/0.174, @70 prec/rcl/F2 = 0.085/0.355/0.217, PERQ avail 0.693, any good = [0.481], simple 0.470
	 * (test) PERANS acc/prec/rcl/F2 = 0.770/0.064/0.621/0.227, @70 prec/rcl/F2 = 0.122/0.382/0.268, PERQ avail 0.698, any good = [0.530], simple 0.501
	 * (test) PERANS acc/prec/rcl/F2 = 0.744/0.054/0.577/0.196, @70 prec/rcl/F2 = 0.096/0.337/0.224, PERQ avail 0.730, any good = [0.513], simple 0.472
	 * (test) PERANS acc/prec/rcl/F2 = 0.796/0.060/0.503/0.203, @70 prec/rcl/F2 = 0.108/0.304/0.223, PERQ avail 0.702, any good = [0.475], simple 0.440
	 * (test) PERANS acc/prec/rcl/F2 = 0.786/0.062/0.585/0.217, @70 prec/rcl/F2 = 0.107/0.344/0.238, PERQ avail 0.674, any good = [0.527], simple 0.507
	 * (test) PERANS acc/prec/rcl/F2 = 0.756/0.059/0.580/0.210, @70 prec/rcl/F2 = 0.111/0.370/0.252, PERQ avail 0.716, any good = [0.518], simple 0.480
	 * Cross-validation score mean 50.639% S.D. 2.041%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.764/1.000/0.243/0.286, @70 prec/rcl/F2 = 1.000/0.079/0.097, PERQ avail 0.702, any good = [0.529], simple 0.459
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.105404, -0.002554,  0.000000, /*                  occurences d01:  0.102850 */
		/*              resultLogScore @,%,! */  0.525305,  0.083095,  0.000000, /*              resultLogScore d01:  0.608400 */
		/*             passageLogScore @,%,! */ -0.377531,  0.643871,  0.170482, /*             passageLogScore d01:  0.095858 */
		/*                   originPsg @,%,! */ -0.218657, -0.257209,  0.170482, /*                   originPsg d01: -0.646347 */
		/*              originPsgFirst @,%,! */  0.038583, -0.183857, -0.086759, /*              originPsgFirst d01: -0.058515 */
		/*                 originPsgNP @,%,! */  0.782943, -0.056151, -0.831118, /*                 originPsgNP d01:  1.557910 */
		/*                 originPsgNE @,%,! */  0.063871, -0.017350, -0.112046, /*                 originPsgNE d01:  0.158567 */
		/*        originPsgNPByLATSubj @,%,! */  0.186276, -0.012896, -0.234451, /*        originPsgNPByLATSubj d01:  0.407831 */
		/*              originDocTitle @,%,! */  0.626338,  0.271822, -0.674514, /*              originDocTitle d01:  1.572674 */
		/*               originConcept @,%,! */  0.063294, -0.404934, -0.111470, /*               originConcept d01: -0.230169 */
		/*      originConceptBySubject @,%,! */  0.181716, -0.010783, -0.229891, /*      originConceptBySubject d01:  0.400824 */
		/*          originConceptByLAT @,%,! */  0.280744, -0.682220, -0.328920, /*          originConceptByLAT d01: -0.072556 */
		/*           originConceptByNE @,%,! */  0.232858, -0.333711, -0.281033, /*           originConceptByNE d01:  0.180180 */
		/*              originMultiple @,%,! */ -0.391914, -0.175778,  0.343739, /*              originMultiple d01: -0.911431 */
		/*                   spWordNet @,%,! */  0.622552,  0.237998,  0.741653, /*                   spWordNet d01:  0.118897 */
		/*               LATQNoWordNet @,%,! */ -0.580860,  0.000000,  0.532685, /*               LATQNoWordNet d01: -1.113545 */
		/*               LATANoWordNet @,%,! */  0.378252, -0.467189, -0.426427, /*               LATANoWordNet d01:  0.337490 */
		/*              tyCorPassageSp @,%,! */  1.857920, -0.162537,  0.163041, /*              tyCorPassageSp d01:  1.532342 */
		/*            tyCorPassageDist @,%,! */ -0.103669,  0.097764,  0.163041, /*            tyCorPassageDist d01: -0.168946 */
		/*          tyCorPassageInside @,%,! */  0.155347, -0.038212, -0.203522, /*          tyCorPassageInside d01:  0.320657 */
		/*                 simpleScore @,%,! */  0.010836,  0.110284,  0.000000, /*                 simpleScore d01:  0.121120 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000, -0.048175, /*                    LATFocus d01:  0.048175 */
		/*               LATFocusProxy @,%,! */ -0.551992, -0.009338,  0.503816, /*               LATFocusProxy d01: -1.065146 */
		/*                       LATNE @,%,! */  0.240093, -0.360051, -0.569827, /*                       LATNE d01:  0.449869 */
		/*                  LATDBpType @,%,! */  0.117090, -0.381298, -0.287168, /*                  LATDBpType d01:  0.022960 */
		/*                 LATQuantity @,%,! */  0.397255, -0.249536,  0.171060, /*                 LATQuantity d01: -0.023340 */
		/*               LATQuantityCD @,%,! */  0.683713,  0.058369, -0.025969, /*               LATQuantityCD d01:  0.768051 */
		/*                 tyCorSpQHit @,%,! */  0.606105, -0.226126, -0.654280, /*                 tyCorSpQHit d01:  1.034259 */
		/*                 tyCorSpAHit @,%,! */  0.610208, -0.143730, -0.658384, /*                 tyCorSpAHit d01:  1.124863 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000, -0.048175, /*             tyCorXHitAFocus d01:  0.048175 */
		/*                 tyCorAFocus @,%,! */ -0.988359,  0.302154,  0.940184, /*                 tyCorAFocus d01: -1.626389 */
		/*                    tyCorANE @,%,! */  0.145010,  0.018534, -0.193185, /*                    tyCorANE d01:  0.356729 */
		/*                   tyCorADBp @,%,! */  0.185275, -0.026774, -0.233451, /*                   tyCorADBp d01:  0.391952 */
		/*              tyCorAQuantity @,%,! */ -1.913921,  1.414321,  1.828028, /*              tyCorAQuantity d01: -2.327629 */
	};
	public static double intercept = -0.048175;

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
