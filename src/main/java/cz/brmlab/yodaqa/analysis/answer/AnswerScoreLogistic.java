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
	 * (test) PERANS acc/prec/rcl/F2 = 0.793/0.064/0.570/0.221, @70 prec/rcl/F2 = 0.129/0.388/0.277, PERQ avail 0.730, any good = [0.528], simple 0.491
	 * (test) PERANS acc/prec/rcl/F2 = 0.777/0.062/0.550/0.213, @70 prec/rcl/F2 = 0.105/0.358/0.241, PERQ avail 0.698, any good = [0.586], simple 0.508
	 * (test) PERANS acc/prec/rcl/F2 = 0.780/0.060/0.595/0.214, @70 prec/rcl/F2 = 0.098/0.366/0.237, PERQ avail 0.688, any good = [0.505], simple 0.434
	 * (test) PERANS acc/prec/rcl/F2 = 0.752/0.061/0.604/0.218, @70 prec/rcl/F2 = 0.096/0.416/0.250, PERQ avail 0.730, any good = [0.553], simple 0.464
	 * (test) PERANS acc/prec/rcl/F2 = 0.765/0.061/0.643/0.220, @70 prec/rcl/F2 = 0.119/0.401/0.271, PERQ avail 0.679, any good = [0.574], simple 0.488
	 * (test) PERANS acc/prec/rcl/F2 = 0.767/0.057/0.608/0.208, @70 prec/rcl/F2 = 0.097/0.394/0.244, PERQ avail 0.674, any good = [0.484], simple 0.463
	 * (test) PERANS acc/prec/rcl/F2 = 0.769/0.055/0.571/0.198, @70 prec/rcl/F2 = 0.099/0.352/0.233, PERQ avail 0.716, any good = [0.540], simple 0.451
	 * (test) PERANS acc/prec/rcl/F2 = 0.757/0.059/0.609/0.212, @70 prec/rcl/F2 = 0.097/0.367/0.236, PERQ avail 0.693, any good = [0.446], simple 0.493
	 * (test) PERANS acc/prec/rcl/F2 = 0.770/0.063/0.618/0.223, @70 prec/rcl/F2 = 0.101/0.373/0.242, PERQ avail 0.730, any good = [0.485], simple 0.482
	 * (test) PERANS acc/prec/rcl/F2 = 0.780/0.068/0.553/0.227, @70 prec/rcl/F2 = 0.114/0.351/0.248, PERQ avail 0.735, any good = [0.514], simple 0.469
	 * Cross-validation score mean 52.148% S.D. 4.128%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.772/1.000/0.235/0.277, @70 prec/rcl/F2 = 1.000/0.084/0.102, PERQ avail 0.714, any good = [0.538], simple 0.486
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.062829,  0.026179,  0.000000, /*                  occurences d01:  0.089007 */
		/*              resultLogScore @,%,! */  0.558446,  0.079826,  0.000000, /*              resultLogScore d01:  0.638272 */
		/*             passageLogScore @,%,! */ -0.325973,  0.632620,  0.174476, /*             passageLogScore d01:  0.132171 */
		/*                   originPsg @,%,! */ -0.228173, -0.278313,  0.174476, /*                   originPsg d01: -0.680963 */
		/*              originPsgFirst @,%,! */  0.005867, -0.147638, -0.059564, /*              originPsgFirst d01: -0.082207 */
		/*                 originPsgNP @,%,! */  0.663335,  0.054349, -0.717031, /*                 originPsgNP d01:  1.434715 */
		/*                 originPsgNE @,%,! */ -0.014601,  0.045809, -0.039096, /*                 originPsgNE d01:  0.070304 */
		/*        originPsgNPByLATSubj @,%,! */  0.167821, -0.014886, -0.221517, /*        originPsgNPByLATSubj d01:  0.374452 */
		/*              originDocTitle @,%,! */  0.566538,  0.286170, -0.620234, /*              originDocTitle d01:  1.472942 */
		/*               originConcept @,%,! */ -0.022439, -0.356604, -0.031257, /*               originConcept d01: -0.347786 */
		/*      originConceptBySubject @,%,! */  0.256573, -0.060813, -0.310270, /*      originConceptBySubject d01:  0.506030 */
		/*          originConceptByLAT @,%,! */  0.151673, -0.576090, -0.205370, /*          originConceptByLAT d01: -0.219047 */
		/*           originConceptByNE @,%,! */  0.259838, -0.340924, -0.313534, /*           originConceptByNE d01:  0.232448 */
		/*              originMultiple @,%,! */ -0.395674, -0.174096,  0.341978, /*              originMultiple d01: -0.911749 */
		/*                   spWordNet @,%,! */  1.350420,  0.153554,  0.124640, /*                   spWordNet d01:  1.379333 */
		/*               LATQNoWordNet @,%,! */ -0.492385,  0.000000,  0.438689, /*               LATQNoWordNet d01: -0.931075 */
		/*               LATANoWordNet @,%,! */  0.454178, -0.570100, -0.507875, /*               LATANoWordNet d01:  0.391953 */
		/*              tyCorPassageSp @,%,! */  1.901064, -0.055126,  0.154535, /*              tyCorPassageSp d01:  1.691402 */
		/*            tyCorPassageDist @,%,! */ -0.227661, -0.003298,  0.154535, /*            tyCorPassageDist d01: -0.385494 */
		/*          tyCorPassageInside @,%,! */  0.122520, -0.009020, -0.176217, /*          tyCorPassageInside d01:  0.289717 */
		/*                 simpleScore @,%,! */  0.019144,  0.093343,  0.000000, /*                 simpleScore d01:  0.112487 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000, -0.053696, /*                    LATFocus d01:  0.053696 */
		/*               LATFocusProxy @,%,! */ -0.236728, -0.219343,  0.183031, /*               LATFocusProxy d01: -0.639102 */
		/*                       LATNE @,%,! */ -0.102310, -0.251707, -0.371999, /*                       LATNE d01:  0.017983 */
		/*                  LATDBpType @,%,! */  0.076716, -0.317345, -0.316744, /*                  LATDBpType d01:  0.076115 */
		/*                 tyCorSpQHit @,%,! */ -0.451329,  0.118979,  0.397632, /*                 tyCorSpQHit d01: -0.729982 */
		/*                 tyCorSpAHit @,%,! */  0.828576, -0.444111, -0.882273, /*                 tyCorSpAHit d01:  1.266738 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000, -0.053696, /*             tyCorXHitAFocus d01:  0.053696 */
		/*                 tyCorAFocus @,%,! */ -1.336509,  0.650666,  1.282813, /*                 tyCorAFocus d01: -1.968656 */
		/*                    tyCorANE @,%,! */  0.482118, -0.057971, -0.535815, /*                    tyCorANE d01:  0.959962 */
		/*                   tyCorADBp @,%,! */  0.337946, -0.061163, -0.391643, /*                   tyCorADBp d01:  0.668426 */
	};
	public static double intercept = -0.053696;

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
