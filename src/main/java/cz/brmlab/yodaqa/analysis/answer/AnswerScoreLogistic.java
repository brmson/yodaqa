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
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */ -0.012339, -0.031856,  0.000000, /*                  occurences d01: -0.044196 */
		/*              resultLogScore @,%,! */  0.559956,  0.063586,  0.000000, /*              resultLogScore d01:  0.623542 */
		/*             passageLogScore @,%,! */ -0.197483,  0.626866,  0.092419, /*             passageLogScore d01:  0.336964 */
		/*                   originPsg @,%,! */  0.019368, -0.515246,  0.092419, /*                   originPsg d01: -0.588297 */
		/*              originPsgFirst @,%,! */  0.147003, -0.178337, -0.035216, /*              originPsgFirst d01:  0.003882 */
		/*                 originPsgNP @,%,! */  0.342501,  0.301692, -0.230713, /*                 originPsgNP d01:  0.874906 */
		/*                 originPsgNE @,%,! */ -0.103938,  0.083992,  0.215725, /*                 originPsgNE d01: -0.235671 */
		/*        originPsgNPByLATSubj @,%,! */  0.326020, -0.015500, -0.214232, /*        originPsgNPByLATSubj d01:  0.524752 */
		/*           originPsgSurprise @,%,! */  0.058143, -0.008212,  0.053645, /*           originPsgSurprise d01: -0.003714 */
		/*              originDocTitle @,%,! */  0.597087,  0.126226, -0.485299, /*              originDocTitle d01:  1.208612 */
		/*           originDBpRelation @,%,! */  0.047665,  0.035590,  0.064122, /*           originDBpRelation d01:  0.019133 */
		/*               originConcept @,%,! */  0.046417, -0.336377,  0.065371, /*               originConcept d01: -0.355331 */
		/*      originConceptBySubject @,%,! */  0.401110, -0.116914, -0.289322, /*      originConceptBySubject d01:  0.573518 */
		/*          originConceptByLAT @,%,! */  0.442482, -0.625352, -0.330694, /*          originConceptByLAT d01:  0.147824 */
		/*           originConceptByNE @,%,! */  0.398493, -0.383440, -0.286706, /*           originConceptByNE d01:  0.301759 */
		/*              originMultiple @,%,! */ -0.105707, -0.174342,  0.217495, /*              originMultiple d01: -0.497544 */
		/*                   spWordNet @,%,! */ -0.063480,  0.239917, -0.405324, /*                   spWordNet d01:  0.581761 */
		/*               LATQNoWordNet @,%,! */ -0.294036,  0.000000,  0.405824, /*               LATQNoWordNet d01: -0.699860 */
		/*               LATANoWordNet @,%,! */  0.162929, -0.028695, -0.051141, /*               LATANoWordNet d01:  0.185375 */
		/*              tyCorPassageSp @,%,! */  1.263577,  0.059882,  0.144735, /*              tyCorPassageSp d01:  1.178724 */
		/*            tyCorPassageDist @,%,! */  0.276526, -0.116755,  0.144735, /*            tyCorPassageDist d01:  0.015036 */
		/*          tyCorPassageInside @,%,! */ -0.049752,  0.123525,  0.161539, /*          tyCorPassageInside d01: -0.087766 */
		/*                 simpleScore @,%,! */  0.005399,  0.120694,  0.000000, /*                 simpleScore d01:  0.126092 */
		/*                       LATNE @,%,! */ -0.273137,  0.270370,  0.333143, /*                       LATNE d01: -0.335909 */
		/*                  LATDBpType @,%,! */  0.755053, -0.748230, -0.695048, /*                  LATDBpType d01:  0.701871 */
		/*                 LATQuantity @,%,! */ -0.194094, -0.080593,  0.254099, /*                 LATQuantity d01: -0.528786 */
		/*               LATQuantityCD @,%,! */  0.666993, -0.246150, -0.606987, /*               LATQuantityCD d01:  1.027829 */
		/*               LATWnInstance @,%,! */  0.483584, -0.184357, -0.423578, /*               LATWnInstance d01:  0.722805 */
		/*              LATDBpRelation @,%,! */ -0.001913,  0.032164,  0.061919, /*              LATDBpRelation d01: -0.031668 */
		/*                 tyCorSpQHit @,%,! */  0.652977, -0.029996, -0.541189, /*                 tyCorSpQHit d01:  1.164169 */
		/*                 tyCorSpAHit @,%,! */  0.025592, -0.409377,  0.086196, /*                 tyCorSpAHit d01: -0.469980 */
		/*                    tyCorANE @,%,! */  1.063217, -0.118016, -0.951429, /*                    tyCorANE d01:  1.896630 */
		/*                   tyCorADBp @,%,! */  0.896886, -0.181593, -0.785098, /*                   tyCorADBp d01:  1.500390 */
		/*              tyCorAQuantity @,%,! */ -0.043941,  0.049687,  0.155728, /*              tyCorAQuantity d01: -0.149982 */
		/*            tyCorAQuantityCD @,%,! */ -0.858078,  0.848658,  0.969866, /*            tyCorAQuantityCD d01: -0.979286 */
		/*            tyCorAWnInstance @,%,! */ -0.548228,  0.241369,  0.660016, /*            tyCorAWnInstance d01: -0.966875 */
		/*           tyCorADBpRelation @,%,! */ -0.269009,  0.190748,  0.380797, /*           tyCorADBpRelation d01: -0.459058 */
	};
	public static double intercept = 0.111788;

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
