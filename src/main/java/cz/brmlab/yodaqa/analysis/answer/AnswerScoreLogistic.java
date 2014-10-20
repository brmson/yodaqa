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
	 * (test) PERANS acc/prec/rcl/F2 = 0.754/0.048/0.525/0.175, @70 prec/rcl/F2 = 0.082/0.308/0.199, PERQ avail 0.688, any good = [0.403], simple 0.431
	 * (test) PERANS acc/prec/rcl/F2 = 0.762/0.061/0.594/0.216, @70 prec/rcl/F2 = 0.117/0.371/0.259, PERQ avail 0.698, any good = [0.553], simple 0.454
	 * (test) PERANS acc/prec/rcl/F2 = 0.747/0.052/0.594/0.193, @70 prec/rcl/F2 = 0.087/0.348/0.217, PERQ avail 0.684, any good = [0.504], simple 0.416
	 * (test) PERANS acc/prec/rcl/F2 = 0.741/0.045/0.553/0.171, @70 prec/rcl/F2 = 0.078/0.317/0.196, PERQ avail 0.702, any good = [0.483], simple 0.463
	 * (test) PERANS acc/prec/rcl/F2 = 0.747/0.055/0.617/0.203, @70 prec/rcl/F2 = 0.101/0.390/0.248, PERQ avail 0.730, any good = [0.485], simple 0.445
	 * (test) PERANS acc/prec/rcl/F2 = 0.745/0.055/0.578/0.200, @70 prec/rcl/F2 = 0.086/0.338/0.213, PERQ avail 0.693, any good = [0.522], simple 0.471
	 * (test) PERANS acc/prec/rcl/F2 = 0.744/0.055/0.628/0.202, @70 prec/rcl/F2 = 0.094/0.374/0.235, PERQ avail 0.688, any good = [0.525], simple 0.422
	 * (test) PERANS acc/prec/rcl/F2 = 0.744/0.058/0.559/0.204, @70 prec/rcl/F2 = 0.103/0.344/0.234, PERQ avail 0.702, any good = [0.469], simple 0.481
	 * (test) PERANS acc/prec/rcl/F2 = 0.746/0.054/0.577/0.197, @70 prec/rcl/F2 = 0.098/0.350/0.231, PERQ avail 0.735, any good = [0.489], simple 0.437
	 * (test) PERANS acc/prec/rcl/F2 = 0.758/0.062/0.595/0.218, @70 prec/rcl/F2 = 0.106/0.326/0.230, PERQ avail 0.702, any good = [0.496], simple 0.434
	 * Cross-validation score mean 49.279% S.D. 3.791%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.750/1.000/0.257/0.301, @70 prec/rcl/F2 = 1.000/0.084/0.103, PERQ avail 0.702, any good = [0.531], simple 0.457
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.137577, -0.025075,  0.000000, /*                  occurences d01:  0.112502 */
		/*              resultLogScore @,%,! */  0.630746,  0.100662,  0.000000, /*              resultLogScore d01:  0.731408 */
		/*             passageLogScore @,%,! */ -0.192502,  0.551551, -0.118563, /*             passageLogScore d01:  0.477611 */
		/*                   originPsg @,%,! */  0.073977, -0.351117, -0.118563, /*                   originPsg d01: -0.158577 */
		/*              originPsgFirst @,%,! */ -0.156891, -0.014469,  0.112304, /*              originPsgFirst d01: -0.283663 */
		/*                 originPsgNP @,%,! */  0.426229, -0.177009, -0.470815, /*                 originPsgNP d01:  0.720036 */
		/*                 originPsgNE @,%,! */ -0.303383, -0.154356,  0.258796, /*                 originPsgNE d01: -0.716535 */
		/*              originDocTitle @,%,! */  0.451927,  0.113544, -0.496513, /*              originDocTitle d01:  1.061983 */
		/*               originConcept @,%,! */ -0.028300, -0.426822, -0.016286, /*               originConcept d01: -0.438835 */
		/*      originConceptBySubject @,%,! */ -0.042480,  0.132147, -0.002106, /*      originConceptBySubject d01:  0.091774 */
		/*        originConceptByFocus @,%,! */  0.618565, -0.821149, -0.663151, /*        originConceptByFocus d01:  0.460567 */
		/*           originConceptByNE @,%,! */  0.332094, -0.373328, -0.376680, /*           originConceptByNE d01:  0.335446 */
		/*              originMultiple @,%,! */  0.089083, -0.106209, -0.133670, /*              originMultiple d01:  0.116544 */
		/*                   spWordNet @,%,! */ -0.633115,  0.307645,  0.405882, /*                   spWordNet d01: -0.731352 */
		/*               LATQNoWordNet @,%,! */ -0.543808,  0.000000,  0.499222, /*               LATQNoWordNet d01: -1.043030 */
		/*               LATANoWordNet @,%,! */  0.238807, -0.275779, -0.283393, /*               LATANoWordNet d01:  0.246421 */
		/*              tyCorPassageSp @,%,! */  1.718258, -0.131371,  0.127879, /*              tyCorPassageSp d01:  1.459008 */
		/*            tyCorPassageDist @,%,! */  0.258897,  0.019745,  0.127879, /*            tyCorPassageDist d01:  0.150763 */
		/*          tyCorPassageInside @,%,! */  0.071429, -0.034067, -0.116016, /*          tyCorPassageInside d01:  0.153378 */
		/*                 simpleScore @,%,! */  0.001429,  0.101896,  0.000000, /*                 simpleScore d01:  0.103325 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000, -0.044586, /*                    LATFocus d01:  0.044586 */
		/*               LATFocusProxy @,%,! */ -1.030728,  0.153526,  0.986142, /*               LATFocusProxy d01: -1.863343 */
		/*                       LATNE @,%,! */ -0.284744, -0.142261, -0.699669, /*                       LATNE d01:  0.272665 */
		/*                 tyCorSpQHit @,%,! */ -0.167126,  0.385969,  0.122540, /*                 tyCorSpQHit d01:  0.096303 */
		/*                 tyCorSpAHit @,%,! */  1.224037, -0.546455, -1.268623, /*                 tyCorSpAHit d01:  1.946205 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000, -0.044586, /*             tyCorXHitAFocus d01:  0.044586 */
	};
	public static double intercept = -0.044586;

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
