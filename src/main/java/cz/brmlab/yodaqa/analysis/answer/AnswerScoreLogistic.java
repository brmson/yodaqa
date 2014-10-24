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
	 * (test) PERANS acc/prec/rcl/F2 = 0.765/0.057/0.586/0.206, @70 prec/rcl/F2 = 0.106/0.351/0.239, PERQ avail 0.702, any good = [0.540], simple 0.512
	 * (test) PERANS acc/prec/rcl/F2 = 0.754/0.059/0.580/0.210, @70 prec/rcl/F2 = 0.106/0.374/0.248, PERQ avail 0.758, any good = [0.519], simple 0.482
	 * (test) PERANS acc/prec/rcl/F2 = 0.755/0.058/0.606/0.209, @70 prec/rcl/F2 = 0.100/0.341/0.230, PERQ avail 0.702, any good = [0.551], simple 0.486
	 * (test) PERANS acc/prec/rcl/F2 = 0.758/0.062/0.586/0.217, @70 prec/rcl/F2 = 0.121/0.386/0.269, PERQ avail 0.707, any good = [0.568], simple 0.470
	 * (test) PERANS acc/prec/rcl/F2 = 0.723/0.052/0.617/0.193, @70 prec/rcl/F2 = 0.097/0.382/0.241, PERQ avail 0.716, any good = [0.548], simple 0.498
	 * (test) PERANS acc/prec/rcl/F2 = 0.738/0.063/0.599/0.222, @70 prec/rcl/F2 = 0.127/0.356/0.262, PERQ avail 0.730, any good = [0.579], simple 0.497
	 * (test) PERANS acc/prec/rcl/F2 = 0.751/0.057/0.619/0.208, @70 prec/rcl/F2 = 0.102/0.393/0.251, PERQ avail 0.688, any good = [0.515], simple 0.444
	 * (test) PERANS acc/prec/rcl/F2 = 0.795/0.055/0.543/0.197, @70 prec/rcl/F2 = 0.095/0.301/0.210, PERQ avail 0.707, any good = [0.481], simple 0.430
	 * (test) PERANS acc/prec/rcl/F2 = 0.776/0.057/0.560/0.203, @70 prec/rcl/F2 = 0.107/0.326/0.231, PERQ avail 0.702, any good = [0.480], simple 0.398
	 * (test) PERANS acc/prec/rcl/F2 = 0.790/0.058/0.538/0.204, @70 prec/rcl/F2 = 0.110/0.316/0.230, PERQ avail 0.684, any good = [0.531], simple 0.452
	 * Cross-validation score mean 53.113% S.D. 3.162%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.759/1.000/0.247/0.291, @70 prec/rcl/F2 = 1.000/0.083/0.101, PERQ avail 0.714, any good = [0.547], simple 0.472
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.105404, -0.002554,  0.000000, /*                  occurences d01:  0.102850 */
		/*              resultLogScore @,%,! */  0.525305,  0.083095,  0.000000, /*              resultLogScore d01:  0.608400 */
		/*             passageLogScore @,%,! */ -0.377531,  0.643871,  0.170482, /*             passageLogScore d01:  0.095858 */
		/*                   originPsg @,%,! */ -0.218657, -0.257209,  0.170482, /*                   originPsg d01: -0.646347 */
		/*              originPsgFirst @,%,! */  0.038583, -0.183857, -0.086759, /*              originPsgFirst d01: -0.058515 */
		/*                 originPsgNP @,%,! */  0.782943, -0.056151, -0.831118, /*                 originPsgNP d01:  1.557910 */
		/*                 originPsgNE @,%,! */  0.063871, -0.017350, -0.112046, /*                 originPsgNE d01:  0.158567 */
		/*        originPsgNPByLATSubj @,%,! */  0.186276, -0.012896, -0.234451, /*        originPsgNPByLATSubj d01:  0.407831 */
		/*              originDocTitle @,%,! */  0.626338,  0.271822, -0.674514, /*              originDocTitle d01:  1.572674 */
		/*               originConcept @,%,! */  0.063294, -0.404934, -0.111470, /*               originConcept d01: -0.230169 */
		/*      originConceptBySubject @,%,! */  0.181716, -0.010783, -0.229891, /*      originConceptBySubject d01:  0.400824 */
		/*          originConceptByLAT @,%,! */  0.280744, -0.682220, -0.328920, /*          originConceptByLAT d01: -0.072556 */
		/*           originConceptByNE @,%,! */  0.232858, -0.333711, -0.281033, /*           originConceptByNE d01:  0.180180 */
		/*              originMultiple @,%,! */ -0.391914, -0.175778,  0.343739, /*              originMultiple d01: -0.911431 */
		/*                   spWordNet @,%,! */  0.622552,  0.237998,  0.741653, /*                   spWordNet d01:  0.118897 */
		/*               LATQNoWordNet @,%,! */ -0.580860,  0.000000,  0.532685, /*               LATQNoWordNet d01: -1.113545 */
		/*               LATANoWordNet @,%,! */  0.378252, -0.467189, -0.426427, /*               LATANoWordNet d01:  0.337490 */
		/*              tyCorPassageSp @,%,! */  1.857920, -0.162537,  0.163041, /*              tyCorPassageSp d01:  1.532342 */
		/*            tyCorPassageDist @,%,! */ -0.103669,  0.097764,  0.163041, /*            tyCorPassageDist d01: -0.168946 */
		/*          tyCorPassageInside @,%,! */  0.155347, -0.038212, -0.203522, /*          tyCorPassageInside d01:  0.320657 */
		/*                 simpleScore @,%,! */  0.010836,  0.110284,  0.000000, /*                 simpleScore d01:  0.121120 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000, -0.048175, /*                    LATFocus d01:  0.048175 */
		/*               LATFocusProxy @,%,! */ -0.551992, -0.009338,  0.503816, /*               LATFocusProxy d01: -1.065146 */
		/*                       LATNE @,%,! */  0.240093, -0.360051, -0.569827, /*                       LATNE d01:  0.449869 */
		/*                  LATDBpType @,%,! */  0.117090, -0.381298, -0.287168, /*                  LATDBpType d01:  0.022960 */
		/*                 tyCorSpQHit @,%,! */  0.606105, -0.226126, -0.654280, /*                 tyCorSpQHit d01:  1.034259 */
		/*                 tyCorSpAHit @,%,! */  0.610208, -0.143730, -0.658384, /*                 tyCorSpAHit d01:  1.124863 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000, -0.048175, /*             tyCorXHitAFocus d01:  0.048175 */
		/*                 tyCorAFocus @,%,! */ -0.988359,  0.302154,  0.940184, /*                 tyCorAFocus d01: -1.626389 */
		/*                    tyCorANE @,%,! */  0.145010,  0.018534, -0.193185, /*                    tyCorANE d01:  0.356729 */
		/*                   tyCorADBp @,%,! */  0.185275, -0.026774, -0.233451, /*                   tyCorADBp d01:  0.391952 */
	};
	public static double intercept = -0.048175;

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
