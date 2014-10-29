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
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */ -0.006504, -0.047509,  0.000000, /*                  occurences d01: -0.054014 */
		/*              resultLogScore @,%,! */  0.583413,  0.075090,  0.000000, /*              resultLogScore d01:  0.658503 */
		/*             passageLogScore @,%,! */ -0.256503,  0.598797,  0.200319, /*             passageLogScore d01:  0.141975 */
		/*                   originPsg @,%,! */ -0.021933, -0.383284,  0.200319, /*                   originPsg d01: -0.605536 */
		/*              originPsgFirst @,%,! */  0.179591, -0.186728, -0.001205, /*              originPsgFirst d01: -0.005932 */
		/*           originPsgByClueSV @,%,! */  0.097868,  0.063784, -0.062045, /*           originPsgByClueSV d01:  0.223697 */
		/*           originPsgByClueNE @,%,! */  0.014046, -0.117908,  0.021778, /*           originPsgByClueNE d01: -0.125640 */
		/*        originPsgByClueLAT @,%,! */ -0.022464, -0.024980,  0.058287, /*        originPsgByClueLAT d01: -0.105731 */
		/*      originPsgByClueSubject @,%,! */  0.528787, -0.544058, -0.492964, /*      originPsgByClueSubject d01:  0.477693 */
		/*      originPsgByClueConcept @,%,! */  0.161910,  0.081642, -0.126086, /*      originPsgByClueConcept d01:  0.369638 */
		/*                 originPsgNP @,%,! */  0.721589,  0.085950, -0.543203, /*                 originPsgNP d01:  1.350742 */
		/*                 originPsgNE @,%,! */  0.013648,  0.082011,  0.164737, /*                 originPsgNE d01: -0.069078 */
		/*        originPsgNPByLATSubj @,%,! */  0.333734, -0.001956, -0.155348, /*        originPsgNPByLATSubj d01:  0.487126 */
		/*           originPsgSurprise @,%,! */ -0.091159,  0.123970,  0.269545, /*           originPsgSurprise d01: -0.236734 */
		/*              originDocTitle @,%,! */  0.727194,  0.137038, -0.548808, /*              originDocTitle d01:  1.413039 */
		/*               originConcept @,%,! */  0.105103, -0.401291,  0.073282, /*               originConcept d01: -0.369470 */
		/*      originConceptBySubject @,%,! */  0.341682, -0.034264, -0.163296, /*      originConceptBySubject d01:  0.470714 */
		/*          originConceptByLAT @,%,! */  0.362755, -0.544052, -0.184370, /*          originConceptByLAT d01:  0.003073 */
		/*           originConceptByNE @,%,! */  0.374430, -0.350409, -0.196045, /*           originConceptByNE d01:  0.220066 */
		/*              originMultiple @,%,! */ -0.102151, -0.216578,  0.280537, /*              originMultiple d01: -0.599266 */
		/*                   spWordNet @,%,! */  0.923505,  0.274991, -0.727900, /*                   spWordNet d01:  1.926395 */
		/*               LATQNoWordNet @,%,! */ -0.248014,  0.000000,  0.426399, /*               LATQNoWordNet d01: -0.674413 */
		/*               LATANoWordNet @,%,! */  0.341930, -0.018393, -0.163545, /*               LATANoWordNet d01:  0.487082 */
		/*              tyCorPassageSp @,%,! */  1.970528, -0.116463,  0.163968, /*              tyCorPassageSp d01:  1.690096 */
		/*            tyCorPassageDist @,%,! */ -0.037657, -0.003241,  0.163968, /*            tyCorPassageDist d01: -0.204866 */
		/*          tyCorPassageInside @,%,! */ -0.107042,  0.127208,  0.285428, /*          tyCorPassageInside d01: -0.265262 */
		/*                 simpleScore @,%,! */  0.007960,  0.129569,  0.000000, /*                 simpleScore d01:  0.137529 */
		/*                       LATNE @,%,! */ -0.881876,  0.220448, -0.467869, /*                       LATNE d01: -0.193559 */
		/*                  LATDBpType @,%,! */  0.020573, -0.324972, -0.048632, /*                  LATDBpType d01: -0.255767 */
		/*                 LATQuantity @,%,! */ -0.188153, -0.068995,  0.366539, /*                 LATQuantity d01: -0.623686 */
		/*               LATQuantityCD @,%,! */  0.721788, -0.121756,  0.167045, /*               LATQuantityCD d01:  0.432987 */
		/*               LATWnInstance @,%,! */ -0.056652, -0.005012, -0.831737, /*               LATWnInstance d01:  0.770072 */
		/*                 tyCorSpQHit @,%,! */  0.307304, -0.023877, -0.128918, /*                 tyCorSpQHit d01:  0.412344 */
		/*                 tyCorSpAHit @,%,! */ -0.110982, -0.414044,  0.289368, /*                 tyCorSpAHit d01: -0.814395 */
		/*                    tyCorANE @,%,! */  1.097990, -0.096336, -0.919605, /*                    tyCorANE d01:  1.921259 */
		/*                   tyCorADBp @,%,! */  0.867229, -0.200945, -0.688844, /*                   tyCorADBp d01:  1.355128 */
		/*              tyCorAQuantity @,%,! */ -0.043507,  0.051718,  0.221893, /*              tyCorAQuantity d01: -0.213682 */
		/*            tyCorAQuantityCD @,%,! */ -0.809476,  0.815288,  0.987861, /*            tyCorAQuantityCD d01: -0.982049 */
		/*            tyCorAWnInstance @,%,! */  0.753231, -0.248683, -0.574845, /*            tyCorAWnInstance d01:  1.079394 */
	};
	public static double intercept = 0.178386;

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
