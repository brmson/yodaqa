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
	 * (test) PERANS acc/prec/rcl/F2 = 0.787/0.067/0.594/0.231, @70 prec/rcl/F2 = 0.110/0.369/0.251, PERQ avail 0.726, any good = [0.591], simple 0.591
	 * (test) PERANS acc/prec/rcl/F2 = 0.745/0.062/0.618/0.221, @70 prec/rcl/F2 = 0.106/0.393/0.255, PERQ avail 0.702, any good = [0.582], simple 0.631
	 * (test) PERANS acc/prec/rcl/F2 = 0.772/0.063/0.596/0.222, @70 prec/rcl/F2 = 0.109/0.369/0.250, PERQ avail 0.735, any good = [0.519], simple 0.512
	 * (test) PERANS acc/prec/rcl/F2 = 0.768/0.063/0.593/0.221, @70 prec/rcl/F2 = 0.101/0.391/0.248, PERQ avail 0.740, any good = [0.499], simple 0.461
	 * (test) PERANS acc/prec/rcl/F2 = 0.758/0.068/0.655/0.240, @70 prec/rcl/F2 = 0.120/0.393/0.270, PERQ avail 0.730, any good = [0.523], simple 0.527
	 * (test) PERANS acc/prec/rcl/F2 = 0.792/0.068/0.594/0.233, @70 prec/rcl/F2 = 0.113/0.421/0.273, PERQ avail 0.721, any good = [0.537], simple 0.461
	 * (test) PERANS acc/prec/rcl/F2 = 0.781/0.065/0.581/0.224, @70 prec/rcl/F2 = 0.103/0.371/0.244, PERQ avail 0.767, any good = [0.489], simple 0.482
	 * (test) PERANS acc/prec/rcl/F2 = 0.767/0.063/0.628/0.225, @70 prec/rcl/F2 = 0.114/0.420/0.273, PERQ avail 0.749, any good = [0.585], simple 0.538
	 * (test) PERANS acc/prec/rcl/F2 = 0.774/0.055/0.584/0.199, @70 prec/rcl/F2 = 0.097/0.394/0.244, PERQ avail 0.688, any good = [0.521], simple 0.488
	 * (test) PERANS acc/prec/rcl/F2 = 0.788/0.057/0.592/0.207, @70 prec/rcl/F2 = 0.097/0.379/0.239, PERQ avail 0.726, any good = [0.499], simple 0.488
	 * Cross-validation score mean 53.451% S.D. 3.619%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.780/1.000/0.226/0.268, @70 prec/rcl/F2 = 1.000/0.084/0.103, PERQ avail 0.714, any good = [0.545], simple 0.518
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.049860,  0.028534,  0.000000, /*                  occurences d01:  0.078394 */
		/*              resultLogScore @,%,! */  0.560581,  0.068684,  0.000000, /*              resultLogScore d01:  0.629265 */
		/*             passageLogScore @,%,! */ -0.361112,  0.646740,  0.230933, /*             passageLogScore d01:  0.054695 */
		/*                   originPsg @,%,! */ -0.196146, -0.252987,  0.230933, /*                   originPsg d01: -0.680066 */
		/*              originPsgFirst @,%,! */  0.068759, -0.178123, -0.033972, /*              originPsgFirst d01: -0.075392 */
		/*                 originPsgNP @,%,! */  0.791278,  0.002021, -0.756491, /*                 originPsgNP d01:  1.549789 */
		/*                 originPsgNE @,%,! */ -0.073781,  0.139643,  0.108567, /*                 originPsgNE d01: -0.042705 */
		/*        originPsgNPByLATSubj @,%,! */  0.176918, -0.001320, -0.142131, /*        originPsgNPByLATSubj d01:  0.317729 */
		/*              originDocTitle @,%,! */  0.657240,  0.288621, -0.622454, /*              originDocTitle d01:  1.568314 */
		/*               originConcept @,%,! */  0.017650, -0.357708,  0.017137, /*               originConcept d01: -0.357195 */
		/*      originConceptBySubject @,%,! */  0.207827, -0.011393, -0.173040, /*      originConceptBySubject d01:  0.369473 */
		/*          originConceptByLAT @,%,! */  0.247091, -0.564324, -0.212304, /*          originConceptByLAT d01: -0.104930 */
		/*           originConceptByNE @,%,! */  0.274086, -0.326689, -0.239299, /*           originConceptByNE d01:  0.186696 */
		/*              originMultiple @,%,! */ -0.397744, -0.183998,  0.432531, /*              originMultiple d01: -1.014274 */
		/*                   spWordNet @,%,! */  0.994339,  0.265103, -0.176062, /*                   spWordNet d01:  1.435503 */
		/*               LATQNoWordNet @,%,! */ -0.413391,  0.000000,  0.448178, /*               LATQNoWordNet d01: -0.861569 */
		/*               LATANoWordNet @,%,! */  0.034787,  0.000000,  0.000000, /*               LATANoWordNet d01:  0.034787 */
		/*              tyCorPassageSp @,%,! */  1.913489, -0.061507,  0.159954, /*              tyCorPassageSp d01:  1.692028 */
		/*            tyCorPassageDist @,%,! */ -0.331344,  0.001048,  0.159954, /*            tyCorPassageDist d01: -0.490250 */
		/*          tyCorPassageInside @,%,! */  0.194513,  0.010818, -0.159727, /*          tyCorPassageInside d01:  0.365059 */
		/*                 simpleScore @,%,! */  0.010882,  0.121276,  0.000000, /*                 simpleScore d01:  0.132158 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000,  0.034787, /*                    LATFocus d01: -0.034787 */
		/*               LATFocusProxy @,%,! */  0.000000,  0.000000,  0.034787, /*               LATFocusProxy d01: -0.034787 */
		/*                       LATNE @,%,! */ -0.910421,  0.229307, -0.524380, /*                       LATNE d01: -0.156734 */
		/*                  LATDBpType @,%,! */  0.024330, -0.335690, -0.084618, /*                  LATDBpType d01: -0.226743 */
		/*                 LATQuantity @,%,! */  0.279981, -0.225196,  0.250161, /*                 LATQuantity d01: -0.195377 */
		/*               LATQuantityCD @,%,! */  0.574132,  0.091900,  0.031720, /*               LATQuantityCD d01:  0.634312 */
		0, 0, 0,
		/*                 tyCorSpQHit @,%,! */  0.034145, -0.002601,  0.000642, /*                 tyCorSpQHit d01:  0.030903 */
		/*                 tyCorSpAHit @,%,! */  0.210848, -0.535515, -0.176062, /*                 tyCorSpAHit d01: -0.148605 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000,  0.034787, /*             tyCorXHitAFocus d01: -0.034787 */
		/*                 tyCorAFocus @,%,! */  0.000000,  0.000000,  0.034787, /*                 tyCorAFocus d01: -0.034787 */
		/*                    tyCorANE @,%,! */  1.027644, -0.073339, -0.992857, /*                    tyCorANE d01:  1.947162 */
		/*                   tyCorADBp @,%,! */  0.874325, -0.199056, -0.839539, /*                   tyCorADBp d01:  1.514807 */
		/*              tyCorAQuantity @,%,! */ -1.258555,  0.986512,  1.293341, /*              tyCorAQuantity d01: -1.565384 */
		0, 0, 0,
	};
	public static double intercept = 0.034787;

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
