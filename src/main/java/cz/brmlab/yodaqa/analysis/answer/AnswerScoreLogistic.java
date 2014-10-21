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
	 * 430 answersets, 82153 answers
	 * + Cross-validation:
	 * (test) PERANS acc/prec/rcl/F2 = 0.766/0.059/0.572/0.210, @70 prec/rcl/F2 = 0.108/0.340/0.238, PERQ avail 0.707, any good = [0.548], simple 0.514
	 * (test) PERANS acc/prec/rcl/F2 = 0.725/0.055/0.675/0.207, @70 prec/rcl/F2 = 0.099/0.415/0.253, PERQ avail 0.693, any good = [0.547], simple 0.517
	 * (test) PERANS acc/prec/rcl/F2 = 0.803/0.060/0.532/0.207, @70 prec/rcl/F2 = 0.099/0.325/0.223, PERQ avail 0.674, any good = [0.515], simple 0.470
	 * (test) PERANS acc/prec/rcl/F2 = 0.766/0.061/0.600/0.217, @70 prec/rcl/F2 = 0.115/0.363/0.254, PERQ avail 0.688, any good = [0.548], simple 0.518
	 * (test) PERANS acc/prec/rcl/F2 = 0.774/0.063/0.594/0.222, @70 prec/rcl/F2 = 0.097/0.343/0.227, PERQ avail 0.702, any good = [0.567], simple 0.473
	 * (test) PERANS acc/prec/rcl/F2 = 0.761/0.064/0.624/0.226, @70 prec/rcl/F2 = 0.111/0.395/0.261, PERQ avail 0.712, any good = [0.510], simple 0.496
	 * (test) PERANS acc/prec/rcl/F2 = 0.780/0.064/0.615/0.226, @70 prec/rcl/F2 = 0.109/0.351/0.243, PERQ avail 0.730, any good = [0.520], simple 0.469
	 * (test) PERANS acc/prec/rcl/F2 = 0.806/0.063/0.545/0.216, @70 prec/rcl/F2 = 0.099/0.329/0.224, PERQ avail 0.670, any good = [0.503], simple 0.494
	 * (test) PERANS acc/prec/rcl/F2 = 0.779/0.058/0.590/0.208, @70 prec/rcl/F2 = 0.102/0.365/0.240, PERQ avail 0.674, any good = [0.518], simple 0.461
	 * (test) PERANS acc/prec/rcl/F2 = 0.749/0.059/0.626/0.215, @70 prec/rcl/F2 = 0.102/0.395/0.251, PERQ avail 0.679, any good = [0.535], simple 0.527
	 * Cross-validation score mean 53.118% S.D. 1.985%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.763/1.000/0.244/0.288, @70 prec/rcl/F2 = 1.000/0.084/0.103, PERQ avail 0.702, any good = [0.562], simple 0.499
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.090899, -0.014431,  0.000000, /*                  occurences d01:  0.076468 */
		/*              resultLogScore @,%,! */  0.670401,  0.075664,  0.000000, /*              resultLogScore d01:  0.746065 */
		/*             passageLogScore @,%,! */ -0.163066,  0.566335, -0.054982, /*             passageLogScore d01:  0.458251 */
		/*                   originPsg @,%,! */  0.000619, -0.300744, -0.054982, /*                   originPsg d01: -0.245143 */
		/*              originPsgFirst @,%,! */ -0.061751, -0.082315,  0.007388, /*              originPsgFirst d01: -0.151453 */
		/*                 originPsgNP @,%,! */  0.372614, -0.167114, -0.426977, /*                 originPsgNP d01:  0.632476 */
		/*                 originPsgNE @,%,! */ -0.162700, -0.238690,  0.108338, /*                 originPsgNE d01: -0.509727 */
		/*              originDocTitle @,%,! */  0.329788,  0.170573, -0.384150, /*              originDocTitle d01:  0.884511 */
		/*               originConcept @,%,! */  0.111979, -0.572049, -0.166342, /*               originConcept d01: -0.293728 */
		/*              originMultiple @,%,! */  0.080712, -0.131427, -0.135074, /*              originMultiple d01:  0.084358 */
		/*                   spWordNet @,%,! */ -0.869562,  0.259699,  0.970358, /*                   spWordNet d01: -1.580222 */
		/*               LATQNoWordNet @,%,! */ -0.527587,  0.000000,  0.473224, /*               LATQNoWordNet d01: -1.000811 */
		/*               LATANoWordNet @,%,! */  0.312897, -0.398850, -0.367260, /*               LATANoWordNet d01:  0.281306 */
		/*              tyCorPassageSp @,%,! */  1.626552, -0.125964,  0.126188, /*              tyCorPassageSp d01:  1.374400 */
		/*            tyCorPassageDist @,%,! */  0.553834, -0.020199,  0.126188, /*            tyCorPassageDist d01:  0.407447 */
		/*          tyCorPassageInside @,%,! */  0.033548, -0.030848, -0.087910, /*          tyCorPassageInside d01:  0.090611 */
		/*                 simpleScore @,%,! */  0.007602,  0.103532,  0.000000, /*                 simpleScore d01:  0.111134 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000, -0.054362, /*                    LATFocus d01:  0.054362 */
		/*               LATFocusProxy @,%,! */ -0.824343,  0.084281,  0.769980, /*               LATFocusProxy d01: -1.510042 */
		/*                       LATNE @,%,! */ -0.209224,  0.002132, -0.225147, /*                       LATNE d01:  0.018055 */
		/*                  LATDBpType @,%,! */  0.113116, -0.406521, -0.382246, /*                  LATDBpType d01:  0.088841 */
		/*                 LATQuantity @,%,! */ -0.025702, -0.180815, -0.064827, /*                 LATQuantity d01: -0.141690 */
		/*               LATQuantityCD @,%,! */  0.256889,  0.160286, -0.266469, /*               LATQuantityCD d01:  0.683644 */
		/*                 tyCorSpQHit @,%,! */  0.788256, -0.099150, -0.842618, /*                 tyCorSpQHit d01:  1.531724 */
		/*                 tyCorSpAHit @,%,! */ -0.027076,  0.007908, -0.027286, /*                 tyCorSpAHit d01:  0.008118 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000, -0.054362, /*             tyCorXHitAFocus d01:  0.054362 */
		/*                 tyCorAFocus @,%,! */ -1.079913,  0.657742,  1.025551, /*                 tyCorAFocus d01: -1.447722 */
		/*                    tyCorANE @,%,! */  1.159122, -0.330412, -1.213484, /*                    tyCorANE d01:  2.042193 */
		/*                   tyCorADBp @,%,! */  1.160926, -0.200918, -1.215288, /*                   tyCorADBp d01:  2.175296 */
	};
	public static double intercept = -0.054362;

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
