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
	 * (test) PERANS acc/prec/rcl/F2 = 0.687/0.053/0.662/0.202, @70 prec/rcl/F2 = 0.093/0.394/0.240, PERQ avail 0.712, any good = [0.551], simple 0.448
	 * (test) PERANS acc/prec/rcl/F2 = 0.760/0.047/0.514/0.171, @70 prec/rcl/F2 = 0.086/0.323/0.208, PERQ avail 0.702, any good = [0.455], simple 0.403
	 * (test) PERANS acc/prec/rcl/F2 = 0.715/0.052/0.605/0.193, @70 prec/rcl/F2 = 0.099/0.367/0.238, PERQ avail 0.702, any good = [0.542], simple 0.450
	 * (test) PERANS acc/prec/rcl/F2 = 0.735/0.057/0.597/0.206, @70 prec/rcl/F2 = 0.101/0.363/0.239, PERQ avail 0.712, any good = [0.497], simple 0.482
	 * (test) PERANS acc/prec/rcl/F2 = 0.733/0.055/0.603/0.201, @70 prec/rcl/F2 = 0.106/0.374/0.249, PERQ avail 0.707, any good = [0.550], simple 0.504
	 * (test) PERANS acc/prec/rcl/F2 = 0.749/0.057/0.567/0.204, @70 prec/rcl/F2 = 0.092/0.350/0.224, PERQ avail 0.740, any good = [0.462], simple 0.467
	 * (test) PERANS acc/prec/rcl/F2 = 0.738/0.059/0.600/0.212, @70 prec/rcl/F2 = 0.098/0.352/0.232, PERQ avail 0.670, any good = [0.488], simple 0.503
	 * (test) PERANS acc/prec/rcl/F2 = 0.719/0.054/0.637/0.203, @70 prec/rcl/F2 = 0.103/0.356/0.239, PERQ avail 0.730, any good = [0.497], simple 0.468
	 * (test) PERANS acc/prec/rcl/F2 = 0.775/0.056/0.548/0.198, @70 prec/rcl/F2 = 0.099/0.325/0.223, PERQ avail 0.670, any good = [0.516], simple 0.481
	 * (test) PERANS acc/prec/rcl/F2 = 0.737/0.055/0.586/0.199, @70 prec/rcl/F2 = 0.095/0.354/0.230, PERQ avail 0.665, any good = [0.526], simple 0.441
	 * Cross-validation score mean 50.836% S.D. 3.274%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.743/1.000/0.265/0.311, @70 prec/rcl/F2 = 1.000/0.080/0.099, PERQ avail 0.702, any good = [0.514], simple 0.457
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.118211, -0.028881,  0.000000, /*                  occurences d01:  0.089331 */
		/*              resultLogScore @,%,! */  0.711904,  0.079234,  0.000000, /*              resultLogScore d01:  0.791138 */
		/*             passageLogScore @,%,! */ -0.583423,  0.716945,  0.051718, /*             passageLogScore d01:  0.081804 */
		/*                   originPsg @,%,! */ -0.015894, -0.385145,  0.051718, /*                   originPsg d01: -0.452757 */
		/*              originPsgFirst @,%,! */  0.115464, -0.188322, -0.079641, /*              originPsgFirst d01:  0.006783 */
		/*           originPsgByClueSV @,%,! */  0.097868,  0.063784, -0.062045, /*           originPsgByClueSV d01:  0.223697 */
		/*           originPsgByClueNE @,%,! */  0.014046, -0.117908,  0.021778, /*           originPsgByClueNE d01: -0.125640 */
		/*        originPsgByClueFocus @,%,! */ -0.022464, -0.024980,  0.058287, /*        originPsgByClueFocus d01: -0.105731 */
		/*      originPsgByClueSubject @,%,! */  0.528787, -0.544058, -0.492964, /*      originPsgByClueSubject d01:  0.477693 */
		/*      originPsgByClueConcept @,%,! */  0.161910,  0.081642, -0.126086, /*      originPsgByClueConcept d01:  0.369638 */
		/*                 originPsgNP @,%,! */  0.457844, -0.178949, -0.422021, /*                 originPsgNP d01:  0.700916 */
		/*                 originPsgNE @,%,! */ -0.332383, -0.106157,  0.368206, /*                 originPsgNE d01: -0.806747 */
		/*              originDocTitle @,%,! */  0.604028,  0.140879, -0.568205, /*              originDocTitle d01:  1.313112 */
		/*               originConcept @,%,! */ -0.056748, -0.375407,  0.092571, /*               originConcept d01: -0.524726 */
		/*      originConceptBySubject @,%,! */ -0.137662,  0.196577,  0.173485, /*      originConceptBySubject d01: -0.114569 */
		/*        originConceptByFocus @,%,! */  0.712795, -0.860991, -0.676972, /*        originConceptByFocus d01:  0.528776 */
		/*           originConceptByNE @,%,! */  0.346595, -0.368885, -0.310771, /*           originConceptByNE d01:  0.288481 */
		/*              originMultiple @,%,! */  0.118653, -0.082428, -0.082829, /*              originMultiple d01:  0.119055 */
		/*                   spWordNet @,%,! */ -0.729425,  0.309086,  0.377232, /*                   spWordNet d01: -0.797571 */
		/*               LATQNoWordNet @,%,! */ -0.479161,  0.000000,  0.514984, /*               LATQNoWordNet d01: -0.994144 */
		/*               LATANoWordNet @,%,! */  0.274106, -0.281815, -0.238283, /*               LATANoWordNet d01:  0.230574 */
		/*              tyCorPassageSp @,%,! */  1.684719, -0.119338,  0.090105, /*              tyCorPassageSp d01:  1.475277 */
		/*            tyCorPassageDist @,%,! */  0.171505,  0.039688,  0.090105, /*            tyCorPassageDist d01:  0.121088 */
		/*          tyCorPassageInside @,%,! */  0.109837, -0.044731, -0.074014, /*          tyCorPassageInside d01:  0.139120 */
		/*                 simpleScore @,%,! */  0.002915,  0.095628,  0.000000, /*                 simpleScore d01:  0.098543 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000,  0.035823, /*                    LATFocus d01: -0.035823 */
		/*               LATFocusProxy @,%,! */ -1.027910,  0.165981,  1.063733, /*               LATFocusProxy d01: -1.925661 */
		/*                       LATNE @,%,! */ -0.245501, -0.156649, -0.649028, /*                       LATNE d01:  0.246878 */
		/*                 tyCorSpQHit @,%,! */  0.038508,  0.321753, -0.002684, /*                 tyCorSpQHit d01:  0.362946 */
		/*                 tyCorSpAHit @,%,! */  1.160369, -0.481209, -1.124546, /*                 tyCorSpAHit d01:  1.803706 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000,  0.035823, /*             tyCorXHitAFocus d01: -0.035823 */
	};

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
