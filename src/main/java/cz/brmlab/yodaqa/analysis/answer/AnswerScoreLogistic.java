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
	 * (test) PERANS acc/prec/rcl/F2 = 0.760/0.057/0.607/0.208, @70 prec/rcl/F2 = 0.102/0.366/0.242, PERQ avail 0.753, any good = [0.432], simple 0.509
	 * (test) PERANS acc/prec/rcl/F2 = 0.746/0.056/0.660/0.208, @70 prec/rcl/F2 = 0.091/0.367/0.229, PERQ avail 0.740, any good = [0.487], simple 0.495
	 * (test) PERANS acc/prec/rcl/F2 = 0.777/0.060/0.550/0.209, @70 prec/rcl/F2 = 0.093/0.314/0.212, PERQ avail 0.730, any good = [0.504], simple 0.544
	 * (test) PERANS acc/prec/rcl/F2 = 0.757/0.063/0.641/0.227, @70 prec/rcl/F2 = 0.115/0.384/0.261, PERQ avail 0.716, any good = [0.532], simple 0.493
	 * (test) PERANS acc/prec/rcl/F2 = 0.777/0.062/0.597/0.218, @70 prec/rcl/F2 = 0.103/0.345/0.234, PERQ avail 0.721, any good = [0.481], simple 0.519
	 * (test) PERANS acc/prec/rcl/F2 = 0.756/0.062/0.594/0.218, @70 prec/rcl/F2 = 0.099/0.376/0.241, PERQ avail 0.735, any good = [0.502], simple 0.501
	 * (test) PERANS acc/prec/rcl/F2 = 0.749/0.056/0.618/0.206, @70 prec/rcl/F2 = 0.098/0.384/0.242, PERQ avail 0.684, any good = [0.498], simple 0.515
	 * (test) PERANS acc/prec/rcl/F2 = 0.762/0.064/0.588/0.223, @70 prec/rcl/F2 = 0.111/0.391/0.260, PERQ avail 0.730, any good = [0.511], simple 0.541
	 * (test) PERANS acc/prec/rcl/F2 = 0.803/0.056/0.539/0.198, @70 prec/rcl/F2 = 0.092/0.303/0.208, PERQ avail 0.693, any good = [0.411], simple 0.474
	 * (test) PERANS acc/prec/rcl/F2 = 0.758/0.057/0.609/0.207, @70 prec/rcl/F2 = 0.095/0.377/0.237, PERQ avail 0.730, any good = [0.506], simple 0.513
	 * Cross-validation score mean 48.634% S.D. 3.537%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.771/1.000/0.235/0.278, @70 prec/rcl/F2 = 1.000/0.085/0.104, PERQ avail 0.730, any good = [0.530], simple 0.510
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
		/*                       LATNE @,%,! */ -1.023491,  0.273424, -0.490480, /*                       LATNE d01: -0.259587 */
		/*                  LATDBpType @,%,! */  0.017846, -0.307483, -0.017687, /*                  LATDBpType d01: -0.271950 */
		/*                 LATQuantity @,%,! */ -0.179648, -0.089754,  0.291436, /*                 LATQuantity d01: -0.560839 */
		/*               LATQuantityCD @,%,! */  0.537140, -0.149985, -0.185711, /*               LATQuantityCD d01:  0.572866 */
		/*               LATWnInstance @,%,! */ -0.036625, -0.037179, -0.590633, /*               LATWnInstance d01:  0.516829 */
		/*              LATDBpRelation @,%,! */  0.047665,  0.035590,  0.064122, /*              LATDBpRelation d01:  0.019133 */
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
