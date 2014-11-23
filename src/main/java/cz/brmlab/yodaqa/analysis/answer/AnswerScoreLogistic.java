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
	 * (test) PERANS acc/prec/rcl/F2 = 0.751/0.060/0.632/0.217, @70 prec/rcl/F2 = 0.113/0.372/0.255, PERQ avail 0.744, any good = [0.509], simple 0.531
	 * (test) PERANS acc/prec/rcl/F2 = 0.763/0.061/0.626/0.219, @70 prec/rcl/F2 = 0.099/0.389/0.245, PERQ avail 0.740, any good = [0.482], simple 0.458
	 * (test) PERANS acc/prec/rcl/F2 = 0.765/0.061/0.609/0.218, @70 prec/rcl/F2 = 0.099/0.348/0.231, PERQ avail 0.726, any good = [0.503], simple 0.532
	 * (test) PERANS acc/prec/rcl/F2 = 0.740/0.061/0.636/0.220, @70 prec/rcl/F2 = 0.095/0.390/0.240, PERQ avail 0.726, any good = [0.530], simple 0.537
	 * (test) PERANS acc/prec/rcl/F2 = 0.754/0.058/0.591/0.209, @70 prec/rcl/F2 = 0.103/0.371/0.244, PERQ avail 0.707, any good = [0.488], simple 0.466
	 * (test) PERANS acc/prec/rcl/F2 = 0.771/0.058/0.594/0.208, @70 prec/rcl/F2 = 0.099/0.377/0.242, PERQ avail 0.716, any good = [0.502], simple 0.482
	 * (test) PERANS acc/prec/rcl/F2 = 0.737/0.055/0.613/0.201, @70 prec/rcl/F2 = 0.089/0.395/0.233, PERQ avail 0.749, any good = [0.516], simple 0.470
	 * (test) PERANS acc/prec/rcl/F2 = 0.776/0.062/0.601/0.219, @70 prec/rcl/F2 = 0.107/0.335/0.235, PERQ avail 0.744, any good = [0.509], simple 0.476
	 * (test) PERANS acc/prec/rcl/F2 = 0.745/0.058/0.640/0.214, @70 prec/rcl/F2 = 0.106/0.398/0.256, PERQ avail 0.712, any good = [0.509], simple 0.540
	 * (test) PERANS acc/prec/rcl/F2 = 0.752/0.057/0.636/0.211, @70 prec/rcl/F2 = 0.100/0.377/0.243, PERQ avail 0.702, any good = [0.523], simple 0.519
	 * Cross-validation score mean 50.710% S.D. 1.387%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.769/1.000/0.238/0.281, @70 prec/rcl/F2 = 1.000/0.085/0.104, PERQ avail 0.730, any good = [0.515], simple 0.507
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */ -0.004599, -0.047108,  0.000000, /*                  occurences d01: -0.051707 */
		/*              resultLogScore @,%,! */  0.581302,  0.053252,  0.000000, /*              resultLogScore d01:  0.634555 */
		/*             passageLogScore @,%,! */ -0.248154,  0.647278,  0.111296, /*             passageLogScore d01:  0.287828 */
		/*                   originPsg @,%,! */ -0.020718, -0.505520,  0.111296, /*                   originPsg d01: -0.637534 */
		/*              originPsgFirst @,%,! */  0.166603, -0.205066, -0.076025, /*              originPsgFirst d01:  0.037562 */
		/*                 originPsgNP @,%,! */  0.320743,  0.302804, -0.230165, /*                 originPsgNP d01:  0.853712 */
		/*                 originPsgNE @,%,! */ -0.176906,  0.121742,  0.267484, /*                 originPsgNE d01: -0.322648 */
		/*        originPsgNPByLATSubj @,%,! */  0.335259, -0.019678, -0.244681, /*        originPsgNPByLATSubj d01:  0.560262 */
		/*           originPsgSurprise @,%,! */  0.076547, -0.028019,  0.014030, /*           originPsgSurprise d01:  0.034498 */
		/*              originDocTitle @,%,! */  0.607460,  0.121888, -0.516882, /*              originDocTitle d01:  1.246230 */
		/*           originDBpRelation @,%,! */  0.014033,  0.041545,  0.076544, /*           originDBpRelation d01: -0.020966 */
		/*               originConcept @,%,! */  0.027905, -0.346090,  0.062673, /*               originConcept d01: -0.380858 */
		/*      originConceptBySubject @,%,! */  0.406271, -0.125064, -0.315694, /*      originConceptBySubject d01:  0.596901 */
		/*          originConceptByLAT @,%,! */  0.391851, -0.592539, -0.301273, /*          originConceptByLAT d01:  0.100585 */
		/*           originConceptByNE @,%,! */  0.364309, -0.360178, -0.273731, /*           originConceptByNE d01:  0.277863 */
		/*              originMultiple @,%,! */ -0.037750, -0.211218,  0.128328, /*              originMultiple d01: -0.377297 */
		/*                   spWordNet @,%,! */  0.857464,  0.222819, -0.596269, /*                   spWordNet d01:  1.676552 */
		/*               LATQNoWordNet @,%,! */ -0.321133,  0.000000,  0.411711, /*               LATQNoWordNet d01: -0.732845 */
		/*               LATANoWordNet @,%,! */  0.146089, -0.013889, -0.055511, /*               LATANoWordNet d01:  0.187710 */
		/*              tyCorPassageSp @,%,! */  1.134087,  0.105407,  0.151684, /*              tyCorPassageSp d01:  1.087810 */
		/*            tyCorPassageDist @,%,! */  0.280050, -0.124498,  0.151684, /*            tyCorPassageDist d01:  0.003868 */
		/*          tyCorPassageInside @,%,! */ -0.076447,  0.141754,  0.167025, /*          tyCorPassageInside d01: -0.101718 */
		/*                 simpleScore @,%,! */  0.004914,  0.136230,  0.000000, /*                 simpleScore d01:  0.141144 */
		/*                       LATNE @,%,! */ -1.054586,  0.280641, -0.488242, /*                       LATNE d01: -0.285704 */
		/*                  LATDBpType @,%,! */  0.017857, -0.314889, -0.026253, /*                  LATDBpType d01: -0.270778 */
		/*                 LATQuantity @,%,! */ -0.185399, -0.083260,  0.275977, /*                 LATQuantity d01: -0.544635 */
		/*               LATQuantityCD @,%,! */  0.609407, -0.149404, -0.134399, /*               LATQuantityCD d01:  0.594402 */
		/*               LATWnInstance @,%,! */ -0.025049, -0.044987, -0.627489, /*               LATWnInstance d01:  0.557453 */
		/*              LATDBpRelation @,%,! */  0.014033,  0.041545,  0.076544, /*              LATDBpRelation d01: -0.020966 */
		/*                 tyCorSpQHit @,%,! */  0.406055, -0.062613, -0.315477, /*                 tyCorSpQHit d01:  0.658918 */
		/*                 tyCorSpAHit @,%,! */ -0.167682, -0.360980,  0.258260, /*                 tyCorSpAHit d01: -0.786921 */
		/*                    tyCorANE @,%,! */  1.142776, -0.107810, -1.052198, /*                    tyCorANE d01:  2.087163 */
		/*                   tyCorADBp @,%,! */  0.856853, -0.173090, -0.766275, /*                   tyCorADBp d01:  1.450039 */
		/*              tyCorAQuantity @,%,! */ -0.045287,  0.049615,  0.135865, /*              tyCorAQuantity d01: -0.131537 */
		/*            tyCorAQuantityCD @,%,! */ -0.882488,  0.835049,  0.973066, /*            tyCorAQuantityCD d01: -1.020506 */
		/*            tyCorAWnInstance @,%,! */ -0.673903,  0.248359,  0.764481, /*            tyCorAWnInstance d01: -1.190025 */
		/*           tyCorADBpRelation @,%,! */ -0.280333,  0.191855,  0.370911, /*           tyCorADBpRelation d01: -0.459389 */
	};
	public static double intercept = 0.090578;

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
