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
		/*                  occurences @,%,! */  0.143288, -0.064453,  0.000000, /*                  occurences d01:  0.078834 */
		/*              resultLogScore @,%,! */  0.635494,  0.081029,  0.000000, /*              resultLogScore d01:  0.716523 */
		/*             passageLogScore @,%,! */ -0.225612,  0.600068,  0.146815, /*             passageLogScore d01:  0.227641 */
		/*                   originPsg @,%,! */ -0.232709, -0.266322,  0.146815, /*                   originPsg d01: -0.645846 */
		/*              originPsgFirst @,%,! */ -0.055157, -0.099963, -0.030737, /*              originPsgFirst d01: -0.124384 */
		/*                 originPsgNP @,%,! */  0.831027, -0.122551, -0.916920, /*                 originPsgNP d01:  1.625397 */
		/*                 originPsgNE @,%,! */  0.294677, -0.043252, -0.380570, /*                 originPsgNE d01:  0.631994 */
		/*        originPsgNPByLATSubj @,%,! */  0.009206,  0.047929, -0.095100, /*        originPsgNPByLATSubj d01:  0.152236 */
		/*              originDocTitle @,%,! */  0.530366,  0.249842, -0.616260, /*              originDocTitle d01:  1.396468 */
		/*               originConcept @,%,! */ -0.172953, -0.350020,  0.087060, /*               originConcept d01: -0.610034 */
		/*      originConceptBySubject @,%,! */  0.308982, -0.125687, -0.394875, /*      originConceptBySubject d01:  0.578170 */
		/*        originConceptByFocus @,%,! */  0.557941, -0.776372, -0.643834, /*        originConceptByFocus d01:  0.425403 */
		/*           originConceptByNE @,%,! */  0.212148, -0.272092, -0.298041, /*           originConceptByNE d01:  0.238098 */
		/*              originMultiple @,%,! */ -0.265159, -0.229306,  0.179266, /*              originMultiple d01: -0.673731 */
		/*                   spWordNet @,%,! */  0.280565,  0.117861,  0.554346, /*                   spWordNet d01: -0.155920 */
		/*               LATQNoWordNet @,%,! */ -0.647027,  0.000000,  0.561134, /*               LATQNoWordNet d01: -1.208160 */
		/*               LATANoWordNet @,%,! */ -0.085893,  0.000000,  0.000000, /*               LATANoWordNet d01: -0.085893 */
		/*              tyCorPassageSp @,%,! */  1.264089, -0.030214,  0.157645, /*              tyCorPassageSp d01:  1.076230 */
		/*            tyCorPassageDist @,%,! */  0.673377, -0.106201,  0.157645, /*            tyCorPassageDist d01:  0.409531 */
		/*          tyCorPassageInside @,%,! */  0.079919, -0.011092, -0.165812, /*          tyCorPassageInside d01:  0.234640 */
		/*                 simpleScore @,%,! */ -0.002622,  0.135762,  0.000000, /*                 simpleScore d01:  0.133140 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000, -0.085893, /*                    LATFocus d01:  0.085893 */
		/*               LATFocusProxy @,%,! */  0.000000,  0.000000, -0.085893, /*               LATFocusProxy d01:  0.085893 */
		/*                       LATNE @,%,! */  0.000000,  0.000000, -0.085893, /*                       LATNE d01:  0.085893 */
		/*                  LATDBpType @,%,! */  0.086605, -0.311357, -0.328221, /*                  LATDBpType d01:  0.103469 */
		/*                 LATQuantity @,%,! */  0.397255, -0.249536,  0.171060, /*                 LATQuantity d01: -0.023340 */
		/*               LATQuantityCD @,%,! */  0.683713,  0.058369, -0.025969, /*               LATQuantityCD d01:  0.768051 */
		/*                 tyCorSpQHit @,%,! */  0.661398, -0.196922, -0.747291, /*                 tyCorSpQHit d01:  1.211767 */
		/*                 tyCorSpAHit @,%,! */ -0.640239,  0.296954,  0.554346, /*                 tyCorSpAHit d01: -0.897631 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000, -0.085893, /*             tyCorXHitAFocus d01:  0.085893 */
		/*                 tyCorAFocus @,%,! */  0.000000,  0.000000, -0.085893, /*                 tyCorAFocus d01:  0.085893 */
		/*                    tyCorANE @,%,! */  0.000000,  0.000000, -0.085893, /*                    tyCorANE d01:  0.085893 */
		/*                   tyCorADBp @,%,! */  1.273682, -0.338640, -1.359575, /*                   tyCorADBp d01:  2.294617 */
		/*              tyCorAQuantity @,%,! */ -1.913921,  1.414321,  1.828028, /*              tyCorAQuantity d01: -2.327629 */
	};
	public static double intercept = -0.085893;

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
