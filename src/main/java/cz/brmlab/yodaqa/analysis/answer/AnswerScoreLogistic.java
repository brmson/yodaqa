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
	 * (test) PERANS acc/prec/rcl/F2 = 0.777/0.063/0.588/0.222, @70 prec/rcl/F2 = 0.111/0.377/0.255, PERQ avail 0.716, any good = [0.513], simple 0.542
	 * (test) PERANS acc/prec/rcl/F2 = 0.776/0.071/0.619/0.243, @70 prec/rcl/F2 = 0.120/0.366/0.259, PERQ avail 0.730, any good = [0.485], simple 0.490
	 * (test) PERANS acc/prec/rcl/F2 = 0.758/0.058/0.573/0.205, @70 prec/rcl/F2 = 0.086/0.350/0.217, PERQ avail 0.688, any good = [0.508], simple 0.568
	 * (test) PERANS acc/prec/rcl/F2 = 0.786/0.067/0.571/0.228, @70 prec/rcl/F2 = 0.118/0.354/0.253, PERQ avail 0.707, any good = [0.509], simple 0.493
	 * (test) PERANS acc/prec/rcl/F2 = 0.783/0.069/0.605/0.237, @70 prec/rcl/F2 = 0.116/0.379/0.261, PERQ avail 0.707, any good = [0.521], simple 0.521
	 * (test) PERANS acc/prec/rcl/F2 = 0.757/0.058/0.600/0.210, @70 prec/rcl/F2 = 0.093/0.354/0.227, PERQ avail 0.688, any good = [0.476], simple 0.521
	 * (test) PERANS acc/prec/rcl/F2 = 0.779/0.057/0.578/0.204, @70 prec/rcl/F2 = 0.096/0.367/0.235, PERQ avail 0.670, any good = [0.509], simple 0.484
	 * (test) PERANS acc/prec/rcl/F2 = 0.748/0.065/0.635/0.230, @70 prec/rcl/F2 = 0.108/0.393/0.257, PERQ avail 0.763, any good = [0.544], simple 0.509
	 * (test) PERANS acc/prec/rcl/F2 = 0.792/0.065/0.553/0.222, @70 prec/rcl/F2 = 0.109/0.342/0.240, PERQ avail 0.749, any good = [0.514], simple 0.511
	 * (test) PERANS acc/prec/rcl/F2 = 0.773/0.058/0.580/0.206, @70 prec/rcl/F2 = 0.095/0.340/0.225, PERQ avail 0.693, any good = [0.440], simple 0.470
	 * Cross-validation score mean 50.189% S.D. 2.708%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.775/1.000/0.232/0.275, @70 prec/rcl/F2 = 1.000/0.086/0.106, PERQ avail 0.714, any good = [0.539], simple 0.516
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */ -0.009601, -0.045176,  0.000000, /*                  occurences d01: -0.054777 */
		/*              resultLogScore @,%,! */  0.584932,  0.074017,  0.000000, /*              resultLogScore d01:  0.658949 */
		/*             passageLogScore @,%,! */ -0.266400,  0.602284,  0.224264, /*             passageLogScore d01:  0.111620 */
		/*                   originPsg @,%,! */  0.002652, -0.386492,  0.224264, /*                   originPsg d01: -0.608105 */
		/*              originPsgFirst @,%,! */  0.210609, -0.190392,  0.016306, /*              originPsgFirst d01:  0.003910 */
		/*                 originPsgNP @,%,! */  0.750804,  0.098030, -0.523888, /*                 originPsgNP d01:  1.372722 */
		/*                 originPsgNE @,%,! */  0.013568,  0.110764,  0.213348, /*                 originPsgNE d01: -0.089016 */
		/*        originPsgNPByLATSubj @,%,! */  0.352751,  0.000159, -0.125836, /*        originPsgNPByLATSubj d01:  0.478746 */
		/*           originPsgSurprise @,%,! */ -0.072574,  0.128016,  0.299490, /*           originPsgSurprise d01: -0.244048 */
		/*              originDocTitle @,%,! */  0.755201,  0.140842, -0.528285, /*              originDocTitle d01:  1.424329 */
		/*               originConcept @,%,! */  0.122651, -0.395235,  0.104265, /*               originConcept d01: -0.376849 */
		/*      originConceptBySubject @,%,! */  0.378728, -0.045644, -0.151812, /*      originConceptBySubject d01:  0.484896 */
		/*          originConceptByLAT @,%,! */  0.381651, -0.543683, -0.154736, /*          originConceptByLAT d01: -0.007295 */
		/*           originConceptByNE @,%,! */  0.403323, -0.354079, -0.176407, /*           originConceptByNE d01:  0.225652 */
		/*              originMultiple @,%,! */ -0.061867, -0.230036,  0.288783, /*              originMultiple d01: -0.580686 */
		/*                   spWordNet @,%,! */  1.295744,  0.251853, -0.944419, /*                   spWordNet d01:  2.492016 */
		/*               LATQNoWordNet @,%,! */ -0.227583,  0.000000,  0.454499, /*               LATQNoWordNet d01: -0.682082 */
		/*               LATANoWordNet @,%,! */  0.395039, -0.027703, -0.168124, /*               LATANoWordNet d01:  0.535460 */
		/*              tyCorPassageSp @,%,! */  1.973901, -0.112870,  0.164861, /*              tyCorPassageSp d01:  1.696171 */
		/*            tyCorPassageDist @,%,! */ -0.050077, -0.004648,  0.164861, /*            tyCorPassageDist d01: -0.219585 */
		/*          tyCorPassageInside @,%,! */ -0.082212,  0.128269,  0.309128, /*          tyCorPassageInside d01: -0.263071 */
		/*                 simpleScore @,%,! */  0.008471,  0.132952,  0.000000, /*                 simpleScore d01:  0.141423 */
		/*                       LATNE @,%,! */ -0.858429,  0.215879, -0.453244, /*                       LATNE d01: -0.189306 */
		/*                  LATDBpType @,%,! */  0.020467, -0.325452, -0.053343, /*                  LATDBpType d01: -0.251642 */
		/*                 LATQuantity @,%,! */ -0.185940, -0.065520,  0.412856, /*                 LATQuantity d01: -0.664316 */
		/*               LATQuantityCD @,%,! */  0.813872, -0.136706,  0.193569, /*               LATQuantityCD d01:  0.483597 */
		/*               LATWnInstance @,%,! */ -0.061674, -0.002226, -0.864285, /*               LATWnInstance d01:  0.800384 */
		/*                 tyCorSpQHit @,%,! */  0.249032, -0.045368, -0.022116, /*                 tyCorSpQHit d01:  0.225781 */
		/*                 tyCorSpAHit @,%,! */ -0.513193, -0.378933,  0.740109, /*                 tyCorSpAHit d01: -1.632235 */
		/*                    tyCorANE @,%,! */  1.065032, -0.321826, -0.838116, /*                    tyCorANE d01:  1.581321 */
		/*                   tyCorADBp @,%,! */  0.870683, -0.228015, -0.643767, /*                   tyCorADBp d01:  1.286434 */
		/*              tyCorAQuantity @,%,! */ -0.042341,  0.052533,  0.269257, /*              tyCorAQuantity d01: -0.259065 */
		/*            tyCorAQuantityCD @,%,! */ -0.398520,  0.436391,  0.625436, /*            tyCorAQuantityCD d01: -0.587565 */
		/*            tyCorAWnInstance @,%,! */  0.538936, -0.300715, -0.312020, /*            tyCorAWnInstance d01:  0.550241 */
		/*                  tyCorANESp @,%,! */ -0.701914,  0.219159, -0.838116, /*                  tyCorANESp d01:  0.355361 */
		/*                 tyCorADBpSp @,%,! */ -0.141878,  0.023895, -0.643767, /*                 tyCorADBpSp d01:  0.525784 */
		/*            tyCorAQuantitySp @,%,! */ -0.005836,  0.052533,  0.269257, /*            tyCorAQuantitySp d01: -0.222560 */
		/*          tyCorAQuantityCDSp @,%,! */  1.021735,  0.436391,  0.625436, /*          tyCorAQuantityCDSp d01:  0.832690 */
		/*          tyCorAWnInstanceSp @,%,! */  1.111267,  0.053085, -0.312020, /*          tyCorAWnInstanceSp d01:  1.476371 */
	};
	public static double intercept = 0.226916;

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
