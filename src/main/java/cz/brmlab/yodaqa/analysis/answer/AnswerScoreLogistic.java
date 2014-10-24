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
	 * (test) PERANS acc/prec/rcl/F2 = 0.754/0.058/0.615/0.211, @70 prec/rcl/F2 = 0.109/0.411/0.265, PERQ avail 0.684, any good = [0.512], simple 0.497
	 * (test) PERANS acc/prec/rcl/F2 = 0.741/0.060/0.635/0.218, @70 prec/rcl/F2 = 0.114/0.417/0.272, PERQ avail 0.726, any good = [0.527], simple 0.492
	 * (test) PERANS acc/prec/rcl/F2 = 0.737/0.061/0.638/0.220, @70 prec/rcl/F2 = 0.119/0.418/0.278, PERQ avail 0.721, any good = [0.560], simple 0.499
	 * (test) PERANS acc/prec/rcl/F2 = 0.789/0.066/0.569/0.225, @70 prec/rcl/F2 = 0.115/0.372/0.258, PERQ avail 0.670, any good = [0.565], simple 0.493
	 * (test) PERANS acc/prec/rcl/F2 = 0.773/0.062/0.557/0.215, @70 prec/rcl/F2 = 0.109/0.344/0.240, PERQ avail 0.698, any good = [0.504], simple 0.512
	 * (test) PERANS acc/prec/rcl/F2 = 0.789/0.063/0.586/0.220, @70 prec/rcl/F2 = 0.109/0.338/0.238, PERQ avail 0.707, any good = [0.507], simple 0.457
	 * (test) PERANS acc/prec/rcl/F2 = 0.760/0.061/0.596/0.216, @70 prec/rcl/F2 = 0.119/0.364/0.258, PERQ avail 0.726, any good = [0.516], simple 0.475
	 * (test) PERANS acc/prec/rcl/F2 = 0.741/0.049/0.605/0.185, @70 prec/rcl/F2 = 0.086/0.383/0.227, PERQ avail 0.679, any good = [0.480], simple 0.392
	 * (test) PERANS acc/prec/rcl/F2 = 0.728/0.050/0.600/0.188, @70 prec/rcl/F2 = 0.082/0.351/0.212, PERQ avail 0.721, any good = [0.496], simple 0.448
	 * (test) PERANS acc/prec/rcl/F2 = 0.751/0.051/0.600/0.191, @70 prec/rcl/F2 = 0.095/0.366/0.234, PERQ avail 0.702, any good = [0.504], simple 0.478
	 * Cross-validation score mean 51.717% S.D. 2.538%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.766/1.000/0.241/0.284, @70 prec/rcl/F2 = 1.000/0.084/0.103, PERQ avail 0.714, any good = [0.541], simple 0.482
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.071306,  0.031232,  0.000000, /*                  occurences d01:  0.102538 */
		/*              resultLogScore @,%,! */  0.518855,  0.089016,  0.000000, /*              resultLogScore d01:  0.607870 */
		/*             passageLogScore @,%,! */ -0.327584,  0.620646,  0.183675, /*             passageLogScore d01:  0.109387 */
		/*                   originPsg @,%,! */ -0.171329, -0.254220,  0.183675, /*                   originPsg d01: -0.609223 */
		/*              originPsgFirst @,%,! */  0.042413, -0.156218, -0.030067, /*              originPsgFirst d01: -0.083738 */
		/*                 originPsgNP @,%,! */  0.832136, -0.066692, -0.819790, /*                 originPsgNP d01:  1.585234 */
		/*                 originPsgNE @,%,! */  0.019021,  0.047498, -0.006675, /*                 originPsgNE d01:  0.073194 */
		/*        originPsgNPByLATSubj @,%,! */  0.200804, -0.018571, -0.188458, /*        originPsgNPByLATSubj d01:  0.370692 */
		/*              originDocTitle @,%,! */  0.625396,  0.273281, -0.613050, /*              originDocTitle d01:  1.511726 */
		/*               originConcept @,%,! */  0.062889, -0.371562, -0.050543, /*               originConcept d01: -0.258129 */
		/*      originConceptBySubject @,%,! */  0.193460, -0.003765, -0.181114, /*      originConceptBySubject d01:  0.370809 */
		/*          originConceptByLAT @,%,! */  0.380956, -0.730882, -0.368610, /*          originConceptByLAT d01:  0.018685 */
		/*           originConceptByNE @,%,! */  0.256810, -0.314652, -0.244464, /*           originConceptByNE d01:  0.186623 */
		/*              originMultiple @,%,! */ -0.354452, -0.187688,  0.366798, /*              originMultiple d01: -0.908938 */
		/*                   spWordNet @,%,! */  1.075365,  0.226398, -0.134175, /*                   spWordNet d01:  1.435938 */
		/*               LATQNoWordNet @,%,! */ -0.486569,  0.000000,  0.498915, /*               LATQNoWordNet d01: -0.985484 */
		/*               LATANoWordNet @,%,! */  0.012346,  0.000000,  0.000000, /*               LATANoWordNet d01:  0.012346 */
		/*              tyCorPassageSp @,%,! */  1.936183, -0.171452,  0.158482, /*              tyCorPassageSp d01:  1.606249 */
		/*            tyCorPassageDist @,%,! */ -0.142245,  0.107355,  0.158482, /*            tyCorPassageDist d01: -0.193372 */
		/*          tyCorPassageInside @,%,! */  0.159507, -0.033015, -0.147161, /*          tyCorPassageInside d01:  0.273652 */
		/*                 simpleScore @,%,! */  0.014461,  0.088290,  0.000000, /*                 simpleScore d01:  0.102751 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000,  0.012346, /*                    LATFocus d01: -0.012346 */
		/*               LATFocusProxy @,%,! */ -0.561408, -0.007760,  0.573754, /*               LATFocusProxy d01: -1.142921 */
		/*                       LATNE @,%,! */ -0.578766,  0.142454, -0.396739, /*                       LATNE d01: -0.039574 */
		/*                  LATDBpType @,%,! */  0.081929, -0.313219, -0.296828, /*                  LATDBpType d01:  0.065538 */
		/*                 tyCorSpQHit @,%,! */  0.246155, -0.173434, -0.233809, /*                 tyCorSpQHit d01:  0.306531 */
		/*                 tyCorSpAHit @,%,! */  0.146521, -0.250986, -0.134175, /*                 tyCorSpAHit d01:  0.029711 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000,  0.012346, /*             tyCorXHitAFocus d01: -0.012346 */
		/*                 tyCorAFocus @,%,! */ -0.904807,  0.461139,  0.917153, /*                 tyCorAFocus d01: -1.360820 */
		/*                    tyCorANE @,%,! */  0.651672, -0.126066, -0.639327, /*                    tyCorANE d01:  1.164933 */
		/*                   tyCorADBp @,%,! */  0.396030,  0.007724, -0.383684, /*                   tyCorADBp d01:  0.787437 */
	};
	public static double intercept = 0.012346;

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
