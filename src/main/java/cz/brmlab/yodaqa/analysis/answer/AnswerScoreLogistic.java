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
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.029000,  0.030688,  0.000000, /*                  occurences d01:  0.059687 */
		/*              resultLogScore @,%,! */  0.555602,  0.079269,  0.000000, /*              resultLogScore d01:  0.634871 */
		/*             passageLogScore @,%,! */ -0.384039,  0.663962,  0.312358, /*             passageLogScore d01: -0.032436 */
		/*                   originPsg @,%,! */ -0.184134, -0.311660,  0.312358, /*                   originPsg d01: -0.808153 */
		/*              originPsgFirst @,%,! */  0.124740, -0.178502,  0.003484, /*              originPsgFirst d01: -0.057246 */
		/*                 originPsgNP @,%,! */  0.823080,  0.039985, -0.694856, /*                 originPsgNP d01:  1.557921 */
		/*                 originPsgNE @,%,! */ -0.036002,  0.169491,  0.164227, /*                 originPsgNE d01: -0.030739 */
		/*        originPsgNPByLATSubj @,%,! */  0.265876, -0.002740, -0.137652, /*        originPsgNPByLATSubj d01:  0.400788 */
		/*           originPsgSurprise @,%,! */ -0.095733,  0.122942,  0.261647, /*           originPsgSurprise d01: -0.234438 */
		/*              originDocTitle @,%,! */  0.696857,  0.243512, -0.568633, /*              originDocTitle d01:  1.509002 */
		/*               originConcept @,%,! */  0.048662, -0.348362,  0.079563, /*               originConcept d01: -0.379263 */
		/*      originConceptBySubject @,%,! */  0.262297, -0.002150, -0.134072, /*      originConceptBySubject d01:  0.394219 */
		/*          originConceptByLAT @,%,! */  0.236985, -0.502695, -0.108761, /*          originConceptByLAT d01: -0.156950 */
		/*           originConceptByNE @,%,! */  0.322048, -0.340898, -0.193823, /*           originConceptByNE d01:  0.174974 */
		/*              originMultiple @,%,! */ -0.284426, -0.231023,  0.412651, /*              originMultiple d01: -0.928100 */
		/*                   spWordNet @,%,! */  0.863428,  0.303681, -0.646811, /*                   spWordNet d01:  1.813921 */
		/*               LATQNoWordNet @,%,! */ -0.344645,  0.000000,  0.472870, /*               LATQNoWordNet d01: -0.817515 */
		/*               LATANoWordNet @,%,! */  0.270178,  0.038443, -0.141954, /*               LATANoWordNet d01:  0.450574 */
		/*              tyCorPassageSp @,%,! */  1.940677, -0.075130,  0.167512, /*              tyCorPassageSp d01:  1.698034 */
		/*            tyCorPassageDist @,%,! */ -0.116251, -0.021708,  0.167512, /*            tyCorPassageDist d01: -0.305472 */
		/*          tyCorPassageInside @,%,! */  0.133193,  0.045513, -0.004969, /*          tyCorPassageInside d01:  0.183675 */
		/*                 simpleScore @,%,! */  0.012814,  0.117872,  0.000000, /*                 simpleScore d01:  0.130686 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000,  0.128224, /*                    LATFocus d01: -0.128224 */
		/*               LATFocusProxy @,%,! */  0.000000,  0.000000,  0.128224, /*               LATFocusProxy d01: -0.128224 */
		/*                       LATNE @,%,! */ -0.863316,  0.240302, -0.387837, /*                       LATNE d01: -0.235177 */
		/*                  LATDBpType @,%,! */  0.022513, -0.348648, -0.032212, /*                  LATDBpType d01: -0.293924 */
		/*                 LATQuantity @,%,! */ -0.161850, -0.087218,  0.290074, /*                 LATQuantity d01: -0.539142 */
		/*               LATQuantityCD @,%,! */  0.833124, -0.117832,  0.290301, /*               LATQuantityCD d01:  0.424991 */
		/*               LATWnInstance @,%,! */ -0.035107, -0.025238, -0.919403, /*               LATWnInstance d01:  0.859058 */
		/*                 tyCorSpQHit @,%,! */  0.383703, -0.081459, -0.255478, /*                 tyCorSpQHit d01:  0.557722 */
		/*                 tyCorSpAHit @,%,! */ -0.041348, -0.482847,  0.169572, /*                 tyCorSpAHit d01: -0.693767 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000,  0.128224, /*             tyCorXHitAFocus d01: -0.128224 */
		/*                 tyCorAFocus @,%,! */  0.000000,  0.000000,  0.128224, /*                 tyCorAFocus d01: -0.128224 */
		/*                    tyCorANE @,%,! */  1.053988, -0.082759, -0.925763, /*                    tyCorANE d01:  1.896993 */
		/*                   tyCorADBp @,%,! */  0.854817, -0.197026, -0.726593, /*                   tyCorADBp d01:  1.384384 */
		/*              tyCorAQuantity @,%,! */ -0.028897,  0.039272,  0.157122, /*              tyCorAQuantity d01: -0.146748 */
		/*            tyCorAQuantityCD @,%,! */ -1.039153,  0.883763,  1.167377, /*            tyCorAQuantityCD d01: -1.322767 */
		/*            tyCorAWnInstance @,%,! */  0.888578, -0.298268, -0.760353, /*            tyCorAWnInstance d01:  1.350664 */
	};
	public static double intercept = 0.128224;

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
