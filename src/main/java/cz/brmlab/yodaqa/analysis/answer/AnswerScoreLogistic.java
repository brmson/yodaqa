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
	 * (test) PERANS acc/prec/rcl/F2 = 0.777/0.056/0.552/0.199, @70 prec/rcl/F2 = 0.103/0.373/0.245, PERQ avail 0.679, any good = [0.523], simple 0.480
	 * (test) PERANS acc/prec/rcl/F2 = 0.787/0.059/0.573/0.208, @70 prec/rcl/F2 = 0.096/0.325/0.220, PERQ avail 0.702, any good = [0.519], simple 0.518
	 * (test) PERANS acc/prec/rcl/F2 = 0.746/0.062/0.630/0.223, @70 prec/rcl/F2 = 0.105/0.376/0.248, PERQ avail 0.693, any good = [0.506], simple 0.446
	 * (test) PERANS acc/prec/rcl/F2 = 0.773/0.060/0.572/0.213, @70 prec/rcl/F2 = 0.101/0.343/0.232, PERQ avail 0.716, any good = [0.537], simple 0.477
	 * (test) PERANS acc/prec/rcl/F2 = 0.791/0.056/0.585/0.204, @70 prec/rcl/F2 = 0.096/0.344/0.227, PERQ avail 0.651, any good = [0.481], simple 0.474
	 * (test) PERANS acc/prec/rcl/F2 = 0.776/0.060/0.570/0.212, @70 prec/rcl/F2 = 0.105/0.362/0.243, PERQ avail 0.684, any good = [0.544], simple 0.476
	 * (test) PERANS acc/prec/rcl/F2 = 0.743/0.055/0.597/0.201, @70 prec/rcl/F2 = 0.086/0.383/0.227, PERQ avail 0.679, any good = [0.558], simple 0.518
	 * (test) PERANS acc/prec/rcl/F2 = 0.735/0.058/0.616/0.210, @70 prec/rcl/F2 = 0.103/0.395/0.253, PERQ avail 0.735, any good = [0.532], simple 0.505
	 * (test) PERANS acc/prec/rcl/F2 = 0.754/0.062/0.636/0.224, @70 prec/rcl/F2 = 0.117/0.387/0.265, PERQ avail 0.712, any good = [0.543], simple 0.508
	 * (test) PERANS acc/prec/rcl/F2 = 0.724/0.055/0.656/0.205, @70 prec/rcl/F2 = 0.105/0.414/0.260, PERQ avail 0.684, any good = [0.504], simple 0.480
	 * Cross-validation score mean 52.456% S.D. 2.178%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.760/1.000/0.246/0.290, @70 prec/rcl/F2 = 1.000/0.080/0.098, PERQ avail 0.702, any good = [0.552], simple 0.482
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.100352, -0.012678,  0.000000, /*                  occurences d01:  0.087674 */
		/*              resultLogScore @,%,! */  0.615729,  0.085443,  0.000000, /*              resultLogScore d01:  0.701173 */
		/*             passageLogScore @,%,! */ -0.112547,  0.537037,  0.059178, /*             passageLogScore d01:  0.365311 */
		/*                   originPsg @,%,! */ -0.085529, -0.266150,  0.059178, /*                   originPsg d01: -0.410857 */
		/*              originPsgFirst @,%,! */ -0.046335, -0.089456,  0.019985, /*              originPsgFirst d01: -0.155777 */
		/*                 originPsgNP @,%,! */  0.881812, -0.080968, -0.908163, /*                 originPsgNP d01:  1.709007 */
		/*                 originPsgNE @,%,! */  0.262866, -0.119885, -0.289217, /*                 originPsgNE d01:  0.432198 */
		/*        originPsgNPByLATSubj @,%,! */  0.146330,  0.030999, -0.172681, /*        originPsgNPByLATSubj d01:  0.350009 */
		/*              originDocTitle @,%,! */  0.647560,  0.246438, -0.673911, /*              originDocTitle d01:  1.567909 */
		/*               originConcept @,%,! */ -0.037110, -0.392325,  0.010759, /*               originConcept d01: -0.440194 */
		/*      originConceptBySubject @,%,! */  0.092005,  0.049552, -0.118355, /*      originConceptBySubject d01:  0.259911 */
		/*        originConceptByFocus @,%,! */  0.704052, -0.868384, -0.730403, /*        originConceptByFocus d01:  0.566072 */
		/*           originConceptByNE @,%,! */  0.273198, -0.347387, -0.299548, /*           originConceptByNE d01:  0.225359 */
		/*              originMultiple @,%,! */ -0.436353, -0.169141,  0.410003, /*              originMultiple d01: -1.015496 */
		/*                   spWordNet @,%,! */ -0.242350,  0.276525,  0.617367, /*                   spWordNet d01: -0.583192 */
		/*               LATQNoWordNet @,%,! */ -0.529512,  0.000000,  0.503161, /*               LATQNoWordNet d01: -1.032673 */
		/*               LATANoWordNet @,%,! */  0.310466, -0.425046, -0.336816, /*               LATANoWordNet d01:  0.222236 */
		/*              tyCorPassageSp @,%,! */  1.520565, -0.097244,  0.146293, /*              tyCorPassageSp d01:  1.277028 */
		/*            tyCorPassageDist @,%,! */  0.348945, -0.021501,  0.146293, /*            tyCorPassageDist d01:  0.181151 */
		/*          tyCorPassageInside @,%,! */  0.177384, -0.046111, -0.203734, /*          tyCorPassageInside d01:  0.335006 */
		/*                 simpleScore @,%,! */  0.006092,  0.121233,  0.000000, /*                 simpleScore d01:  0.127325 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000, -0.026350, /*                    LATFocus d01:  0.026350 */
		/*               LATFocusProxy @,%,! */ -0.320951, -0.099161,  0.294601, /*               LATFocusProxy d01: -0.714713 */
		/*                       LATNE @,%,! */  0.011071, -0.192783, -0.357389, /*                       LATNE d01:  0.175677 */
		/*                  LATDBpType @,%,! */  0.123158, -0.407189, -0.335251, /*                  LATDBpType d01:  0.051220 */
		/*                 tyCorSpQHit @,%,! */  0.673789, -0.148703, -0.700139, /*                 tyCorSpQHit d01:  1.225224 */
		/*                 tyCorSpAHit @,%,! */  0.461338, -0.110680, -0.487688, /*                 tyCorSpAHit d01:  0.838346 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000, -0.026350, /*             tyCorXHitAFocus d01:  0.026350 */
		/*                 tyCorAFocus @,%,! */ -0.993113,  0.350703,  0.966763, /*                 tyCorAFocus d01: -1.609172 */
		/*                    tyCorANE @,%,! */  0.260702, -0.082499, -0.287052, /*                    tyCorANE d01:  0.465255 */
		/*                   tyCorADBp @,%,! */  0.435001, -0.114869, -0.461352, /*                   tyCorADBp d01:  0.781485 */
		0, 0, 0,
		0, 0, 0,
		0, 0, 0,
	};
	public static double intercept = -0.026350;

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
