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
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.092916,  0.010245,  0.000000, /*                  occurences d01:  0.103161 */
		/*              resultLogScore @,%,! */  0.511832,  0.082484,  0.000000, /*              resultLogScore d01:  0.594316 */
		/*             passageLogScore @,%,! */ -0.401060,  0.654388,  0.176693, /*             passageLogScore d01:  0.076635 */
		/*                   originPsg @,%,! */ -0.247078, -0.250126,  0.176693, /*                   originPsg d01: -0.673898 */
		/*              originPsgFirst @,%,! */  0.039955, -0.196601, -0.110341, /*              originPsgFirst d01: -0.046305 */
		/*                 originPsgNP @,%,! */  0.797349, -0.107073, -0.867734, /*                 originPsgNP d01:  1.558010 */
		/*                 originPsgNE @,%,! */ -0.044086,  0.028508, -0.026299, /*                 originPsgNE d01:  0.010720 */
		/*        originPsgNPByLATSubj @,%,! */  0.090474, -0.006358, -0.160859, /*        originPsgNPByLATSubj d01:  0.244975 */
		/*              originDocTitle @,%,! */  0.593601,  0.274701, -0.663986, /*              originDocTitle d01:  1.532288 */
		/*               originConcept @,%,! */  0.014210, -0.363304, -0.084596, /*               originConcept d01: -0.264498 */
		/*      originConceptBySubject @,%,! */  0.193466, -0.045844, -0.263851, /*      originConceptBySubject d01:  0.411473 */
		/*          originConceptByLAT @,%,! */  0.315116, -0.695383, -0.385501, /*          originConceptByLAT d01:  0.005235 */
		/*           originConceptByNE @,%,! */  0.216196, -0.326277, -0.286581, /*           originConceptByNE d01:  0.176501 */
		/*              originMultiple @,%,! */ -0.392637, -0.170336,  0.322251, /*              originMultiple d01: -0.885224 */
		/*                   spWordNet @,%,! */  0.623198,  0.287339,  1.102437, /*                   spWordNet d01: -0.191900 */
		/*               LATQNoWordNet @,%,! */ -0.593599,  0.000000,  0.523213, /*               LATQNoWordNet d01: -1.116812 */
		/*               LATANoWordNet @,%,! */ -0.197345,  0.031990,  0.126959, /*               LATANoWordNet d01: -0.292314 */
		/*              tyCorPassageSp @,%,! */  1.848078, -0.145333,  0.157574, /*              tyCorPassageSp d01:  1.545170 */
		/*            tyCorPassageDist @,%,! */ -0.043652,  0.075182,  0.157574, /*            tyCorPassageDist d01: -0.126044 */
		/*          tyCorPassageInside @,%,! */  0.120954, -0.033657, -0.191339, /*          tyCorPassageInside d01:  0.278636 */
		/*                 simpleScore @,%,! */  0.008635,  0.112193,  0.000000, /*                 simpleScore d01:  0.120828 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000, -0.070385, /*                    LATFocus d01:  0.070385 */
		/*               LATFocusProxy @,%,! */  0.000000,  0.000000, -0.070385, /*               LATFocusProxy d01:  0.070385 */
		/*                       LATNE @,%,! */ -0.519300,  0.085068, -0.353659, /*                       LATNE d01: -0.080573 */
		/*                  LATDBpType @,%,! */  0.103577, -0.329335, -0.268676, /*                  LATDBpType d01:  0.042918 */
		/*                 LATQuantity @,%,! */  0.181380, -0.219494,  0.143735, /*                 LATQuantity d01: -0.181849 */
		/*               LATQuantityCD @,%,! */  0.483532,  0.088723, -0.085272, /*               LATQuantityCD d01:  0.657527 */
		/*                 tyCorSpQHit @,%,! */  0.554928, -0.251968, -0.625313, /*                 tyCorSpQHit d01:  0.928273 */
		/*                 tyCorSpAHit @,%,! */  0.502453, -0.124428, -0.572838, /*                 tyCorSpAHit d01:  0.950863 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000, -0.070385, /*             tyCorXHitAFocus d01:  0.070385 */
		/*                 tyCorAFocus @,%,! */  0.000000,  0.000000, -0.070385, /*                 tyCorAFocus d01:  0.070385 */
		/*                    tyCorANE @,%,! */  0.426455, -0.081790, -0.496840, /*                    tyCorANE d01:  0.841504 */
		/*                   tyCorADBp @,%,! */  0.433293, -0.058735, -0.503678, /*                   tyCorADBp d01:  0.878236 */
		/*              tyCorAQuantity @,%,! */ -2.088286,  1.085869,  2.017901, /*              tyCorAQuantity d01: -3.020317 */
	};
	public static double intercept = -0.070385;

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
