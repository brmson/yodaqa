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
	 * (test) PERANS acc/prec/rcl/F2 = 0.786/0.063/0.629/0.224, @70 prec/rcl/F2 = 0.113/0.420/0.272, PERQ avail 0.712, any good = [0.485], simple 0.486
	 * (test) PERANS acc/prec/rcl/F2 = 0.740/0.056/0.668/0.210, @70 prec/rcl/F2 = 0.098/0.359/0.234, PERQ avail 0.707, any good = [0.542], simple 0.514
	 * (test) PERANS acc/prec/rcl/F2 = 0.756/0.054/0.600/0.198, @70 prec/rcl/F2 = 0.091/0.372/0.230, PERQ avail 0.698, any good = [0.521], simple 0.461
	 * (test) PERANS acc/prec/rcl/F2 = 0.780/0.059/0.621/0.212, @70 prec/rcl/F2 = 0.098/0.374/0.239, PERQ avail 0.665, any good = [0.444], simple 0.419
	 * (test) PERANS acc/prec/rcl/F2 = 0.797/0.056/0.549/0.198, @70 prec/rcl/F2 = 0.094/0.341/0.223, PERQ avail 0.740, any good = [0.445], simple 0.443
	 * (test) PERANS acc/prec/rcl/F2 = 0.770/0.060/0.584/0.214, @70 prec/rcl/F2 = 0.107/0.352/0.242, PERQ avail 0.707, any good = [0.531], simple 0.515
	 * (test) PERANS acc/prec/rcl/F2 = 0.763/0.058/0.600/0.210, @70 prec/rcl/F2 = 0.109/0.388/0.257, PERQ avail 0.698, any good = [0.542], simple 0.528
	 * (test) PERANS acc/prec/rcl/F2 = 0.785/0.056/0.596/0.205, @70 prec/rcl/F2 = 0.091/0.386/0.234, PERQ avail 0.679, any good = [0.524], simple 0.426
	 * (test) PERANS acc/prec/rcl/F2 = 0.737/0.059/0.657/0.217, @70 prec/rcl/F2 = 0.103/0.435/0.264, PERQ avail 0.753, any good = [0.469], simple 0.520
	 * (test) PERANS acc/prec/rcl/F2 = 0.786/0.065/0.544/0.220, @70 prec/rcl/F2 = 0.112/0.381/0.257, PERQ avail 0.679, any good = [0.596], simple 0.491
	 * Cross-validation score mean 50.979% S.D. 4.594%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.777/1.000/0.229/0.271, @70 prec/rcl/F2 = 1.000/0.084/0.103, PERQ avail 0.714, any good = [0.554], simple 0.494
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.061630,  0.025142,  0.000000, /*                  occurences d01:  0.086772 */
		/*              resultLogScore @,%,! */  0.529417,  0.081099,  0.000000, /*              resultLogScore d01:  0.610516 */
		/*             passageLogScore @,%,! */ -0.342173,  0.636296,  0.182548, /*             passageLogScore d01:  0.111575 */
		/*                   originPsg @,%,! */ -0.178557, -0.267331,  0.182548, /*                   originPsg d01: -0.628436 */
		/*              originPsgFirst @,%,! */  0.029344, -0.146830, -0.025353, /*              originPsgFirst d01: -0.092134 */
		/*                 originPsgNP @,%,! */  0.772426, -0.039318, -0.768435, /*                 originPsgNP d01:  1.501543 */
		/*                 originPsgNE @,%,! */ -0.092525,  0.110693,  0.096516, /*                 originPsgNE d01: -0.078349 */
		/*        originPsgNPByLATSubj @,%,! */  0.156463, -0.011099, -0.152472, /*        originPsgNPByLATSubj d01:  0.297836 */
		/*              originDocTitle @,%,! */  0.594599,  0.291727, -0.590609, /*              originDocTitle d01:  1.476935 */
		/*               originConcept @,%,! */ -0.028420, -0.319271,  0.032411, /*               originConcept d01: -0.380102 */
		/*      originConceptBySubject @,%,! */  0.278045, -0.072001, -0.274054, /*      originConceptBySubject d01:  0.480098 */
		/*          originConceptByLAT @,%,! */  0.258950, -0.618751, -0.254959, /*          originConceptByLAT d01: -0.104841 */
		/*           originConceptByNE @,%,! */  0.282650, -0.323995, -0.278659, /*           originConceptByNE d01:  0.237314 */
		/*              originMultiple @,%,! */ -0.399688, -0.167519,  0.403678, /*              originMultiple d01: -0.970885 */
		/*                   spWordNet @,%,! */  1.065146,  0.185538, -0.244331, /*                   spWordNet d01:  1.495015 */
		/*               LATQNoWordNet @,%,! */ -0.475603,  0.000000,  0.479594, /*               LATQNoWordNet d01: -0.955197 */
		/*               LATANoWordNet @,%,! */  0.003991,  0.000000,  0.000000, /*               LATANoWordNet d01:  0.003991 */
		/*              tyCorPassageSp @,%,! */  1.929554, -0.059803,  0.151236, /*              tyCorPassageSp d01:  1.718516 */
		/*            tyCorPassageDist @,%,! */ -0.179470, -0.016187,  0.151236, /*            tyCorPassageDist d01: -0.346893 */
		/*          tyCorPassageInside @,%,! */  0.117232,  0.007544, -0.113241, /*          tyCorPassageInside d01:  0.238018 */
		/*                 simpleScore @,%,! */  0.017622,  0.108644,  0.000000, /*                 simpleScore d01:  0.126267 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000,  0.003991, /*                    LATFocus d01: -0.003991 */
		/*               LATFocusProxy @,%,! */  0.000000,  0.000000,  0.003991, /*               LATFocusProxy d01: -0.003991 */
		/*                       LATNE @,%,! */ -0.707330,  0.178624, -0.442648, /*                       LATNE d01: -0.086058 */
		/*                  LATDBpType @,%,! */  0.066729, -0.276636, -0.308281, /*                  LATDBpType d01:  0.098374 */
		/*                 LATQuantity @,%,! */  0.336066, -0.215412,  0.279283, /*                 LATQuantity d01: -0.158629 */
		/*               LATQuantityCD @,%,! */  0.632644,  0.064981,  0.051200, /*               LATQuantityCD d01:  0.646425 */
		/*                 tyCorSpQHit @,%,! */ -0.125676, -0.024876,  0.129667, /*                 tyCorSpQHit d01: -0.280219 */
		/*                 tyCorSpAHit @,%,! */  0.248322, -0.280669, -0.244331, /*                 tyCorSpAHit d01:  0.211984 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000,  0.003991, /*             tyCorXHitAFocus d01: -0.003991 */
		/*                 tyCorAFocus @,%,! */  0.000000,  0.000000,  0.003991, /*                 tyCorAFocus d01: -0.003991 */
		/*                    tyCorANE @,%,! */  1.005156, -0.317599, -1.001165, /*                    tyCorANE d01:  1.688722 */
		/*                   tyCorADBp @,%,! */  0.663221, -0.127145, -0.659230, /*                   tyCorADBp d01:  1.195305 */
		/*              tyCorAQuantity @,%,! */ -1.534029,  0.917959,  1.538020, /*              tyCorAQuantity d01: -2.154090 */
	};
	public static double intercept = 0.003991;

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
