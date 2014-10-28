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
	 * 430 answersets, 87245 answers
	 * + Cross-validation:
	 * (test) PERANS acc/prec/rcl/F2 = 0.773/0.066/0.617/0.231, @70 prec/rcl/F2 = 0.115/0.362/0.254, PERQ avail 0.744, any good = [0.484], simple 0.484
	 * (test) PERANS acc/prec/rcl/F2 = 0.780/0.064/0.594/0.223, @70 prec/rcl/F2 = 0.104/0.332/0.231, PERQ avail 0.688, any good = [0.493], simple 0.527
	 * (test) PERANS acc/prec/rcl/F2 = 0.781/0.069/0.623/0.240, @70 prec/rcl/F2 = 0.110/0.350/0.243, PERQ avail 0.698, any good = [0.501], simple 0.481
	 * (test) PERANS acc/prec/rcl/F2 = 0.765/0.061/0.617/0.220, @70 prec/rcl/F2 = 0.106/0.394/0.256, PERQ avail 0.740, any good = [0.490], simple 0.481
	 * (test) PERANS acc/prec/rcl/F2 = 0.751/0.060/0.640/0.219, @70 prec/rcl/F2 = 0.120/0.406/0.275, PERQ avail 0.744, any good = [0.471], simple 0.483
	 * (test) PERANS acc/prec/rcl/F2 = 0.745/0.058/0.637/0.213, @70 prec/rcl/F2 = 0.101/0.400/0.251, PERQ avail 0.684, any good = [0.498], simple 0.523
	 * (test) PERANS acc/prec/rcl/F2 = 0.712/0.053/0.676/0.201, @70 prec/rcl/F2 = 0.085/0.429/0.237, PERQ avail 0.707, any good = [0.520], simple 0.522
	 * (test) PERANS acc/prec/rcl/F2 = 0.752/0.064/0.594/0.223, @70 prec/rcl/F2 = 0.099/0.395/0.247, PERQ avail 0.716, any good = [0.516], simple 0.554
	 * (test) PERANS acc/prec/rcl/F2 = 0.765/0.065/0.602/0.226, @70 prec/rcl/F2 = 0.118/0.364/0.256, PERQ avail 0.726, any good = [0.477], simple 0.507
	 * (test) PERANS acc/prec/rcl/F2 = 0.774/0.062/0.620/0.220, @70 prec/rcl/F2 = 0.107/0.395/0.257, PERQ avail 0.698, any good = [0.486], simple 0.484
	 * Cross-validation score mean 49.363% S.D. 1.489%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.774/1.000/0.233/0.276, @70 prec/rcl/F2 = 1.000/0.087/0.106, PERQ avail 0.714, any good = [0.533], simple 0.516
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */ -0.006302, -0.047561,  0.000000, /*                  occurences d01: -0.053863 */
		/*              resultLogScore @,%,! */  0.586236,  0.074511,  0.000000, /*              resultLogScore d01:  0.660747 */
		/*             passageLogScore @,%,! */ -0.237667,  0.591349,  0.191988, /*             passageLogScore d01:  0.161694 */
		/*                   originPsg @,%,! */ -0.026075, -0.380310,  0.191988, /*                   originPsg d01: -0.598373 */
		/*              originPsgFirst @,%,! */  0.172293, -0.185627, -0.006380, /*              originPsgFirst d01: -0.006954 */
		/*                 originPsgNP @,%,! */  0.717175,  0.085125, -0.551262, /*                 originPsgNP d01:  1.353562 */
		/*                 originPsgNE @,%,! */  0.007849,  0.082384,  0.158064, /*                 originPsgNE d01: -0.067830 */
		/*        originPsgNPByLATSubj @,%,! */  0.328982, -0.002282, -0.163068, /*        originPsgNPByLATSubj d01:  0.489768 */
		/*           originPsgSurprise @,%,! */ -0.095733,  0.122942,  0.261647, /*           originPsgSurprise d01: -0.234438 */
		/*              originDocTitle @,%,! */  0.718302,  0.135373, -0.552389, /*              originDocTitle d01:  1.406064 */
		/*               originConcept @,%,! */  0.095458, -0.400243,  0.070456, /*               originConcept d01: -0.375241 */
		/*      originConceptBySubject @,%,! */  0.337182, -0.035535, -0.171268, /*      originConceptBySubject d01:  0.472915 */
		/*          originConceptByLAT @,%,! */  0.360186, -0.546792, -0.194273, /*          originConceptByLAT d01:  0.007667 */
		/*           originConceptByNE @,%,! */  0.368428, -0.350630, -0.202515, /*           originConceptByNE d01:  0.220312 */
		/*              originMultiple @,%,! */ -0.107773, -0.217713,  0.273686, /*              originMultiple d01: -0.599172 */
		/*                   spWordNet @,%,! */  0.931492,  0.274861, -0.732259, /*                   spWordNet d01:  1.938611 */
		/*               LATQNoWordNet @,%,! */ -0.254432,  0.000000,  0.420345, /*               LATQNoWordNet d01: -0.674777 */
		/*               LATANoWordNet @,%,! */  0.334697, -0.017955, -0.168784, /*               LATANoWordNet d01:  0.485525 */
		/*              tyCorPassageSp @,%,! */  1.969913, -0.115755,  0.164543, /*              tyCorPassageSp d01:  1.689615 */
		/*            tyCorPassageDist @,%,! */ -0.038580, -0.003676,  0.164543, /*            tyCorPassageDist d01: -0.206799 */
		/*          tyCorPassageInside @,%,! */ -0.113791,  0.127458,  0.279705, /*          tyCorPassageInside d01: -0.266039 */
		/*                 simpleScore @,%,! */  0.007875,  0.129618,  0.000000, /*                 simpleScore d01:  0.137493 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000,  0.165913, /*                    LATFocus d01: -0.165913 */
		/*               LATFocusProxy @,%,! */  0.000000,  0.000000,  0.165913, /*               LATFocusProxy d01: -0.165913 */
		/*                       LATNE @,%,! */ -0.887441,  0.222116, -0.468480, /*                       LATNE d01: -0.196844 */
		/*                  LATDBpType @,%,! */  0.020559, -0.324690, -0.048467, /*                  LATDBpType d01: -0.255664 */
		/*                 LATQuantity @,%,! */  0.225032, -0.221271,  0.216196, /*                 LATQuantity d01: -0.212435 */
		/*               LATQuantityCD @,%,! */  0.537017,  0.094374, -0.020455, /*               LATQuantityCD d01:  0.651846 */
		/*               LATWnInstance @,%,! */ -0.056357, -0.005322, -0.831138, /*               LATWnInstance d01:  0.769459 */
		/*                 tyCorSpQHit @,%,! */  0.296861, -0.023301, -0.130948, /*                 tyCorSpQHit d01:  0.404508 */
		/*                 tyCorSpAHit @,%,! */ -0.117862, -0.413696,  0.283775, /*                 tyCorSpAHit d01: -0.815332 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000,  0.165913, /*             tyCorXHitAFocus d01: -0.165913 */
		/*                 tyCorAFocus @,%,! */  0.000000,  0.000000,  0.165913, /*                 tyCorAFocus d01: -0.165913 */
		/*                    tyCorANE @,%,! */  1.090403, -0.097050, -0.924490, /*                    tyCorANE d01:  1.917842 */
		/*                   tyCorADBp @,%,! */  0.859066, -0.201268, -0.693152, /*                   tyCorADBp d01:  1.350950 */
		/*              tyCorAQuantity @,%,! */ -0.823374,  0.818574,  0.989287, /*              tyCorAQuantity d01: -0.994087 */
		/*            tyCorAWnInstance @,%,! */  0.745744, -0.249081, -0.579831, /*            tyCorAWnInstance d01:  1.076494 */
	};
	public static double intercept = 0.165913;

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
