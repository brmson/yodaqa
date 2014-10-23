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
	 * (test) PERANS acc/prec/rcl/F2 = 0.749/0.052/0.618/0.193, @70 prec/rcl/F2 = 0.098/0.374/0.239, PERQ avail 0.716, any good = [0.531], simple 0.456
	 * (test) PERANS acc/prec/rcl/F2 = 0.778/0.062/0.542/0.213, @70 prec/rcl/F2 = 0.101/0.336/0.229, PERQ avail 0.702, any good = [0.482], simple 0.444
	 * (test) PERANS acc/prec/rcl/F2 = 0.777/0.063/0.571/0.218, @70 prec/rcl/F2 = 0.116/0.348/0.249, PERQ avail 0.702, any good = [0.531], simple 0.472
	 * (test) PERANS acc/prec/rcl/F2 = 0.733/0.060/0.612/0.215, @70 prec/rcl/F2 = 0.108/0.343/0.239, PERQ avail 0.735, any good = [0.537], simple 0.520
	 * (test) PERANS acc/prec/rcl/F2 = 0.736/0.051/0.609/0.190, @70 prec/rcl/F2 = 0.088/0.389/0.230, PERQ avail 0.674, any good = [0.529], simple 0.447
	 * (test) PERANS acc/prec/rcl/F2 = 0.758/0.057/0.555/0.203, @70 prec/rcl/F2 = 0.101/0.360/0.238, PERQ avail 0.744, any good = [0.501], simple 0.463
	 * (test) PERANS acc/prec/rcl/F2 = 0.764/0.057/0.617/0.209, @70 prec/rcl/F2 = 0.100/0.366/0.239, PERQ avail 0.707, any good = [0.515], simple 0.467
	 * (test) PERANS acc/prec/rcl/F2 = 0.753/0.055/0.591/0.200, @70 prec/rcl/F2 = 0.098/0.354/0.232, PERQ avail 0.693, any good = [0.512], simple 0.452
	 * (test) PERANS acc/prec/rcl/F2 = 0.751/0.055/0.614/0.202, @70 prec/rcl/F2 = 0.100/0.387/0.246, PERQ avail 0.707, any good = [0.560], simple 0.476
	 * (test) PERANS acc/prec/rcl/F2 = 0.719/0.051/0.675/0.197, @70 prec/rcl/F2 = 0.092/0.428/0.248, PERQ avail 0.665, any good = [0.488], simple 0.471
	 * Cross-validation score mean 51.855% S.D. 2.270%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.760/1.000/0.247/0.290, @70 prec/rcl/F2 = 1.000/0.081/0.100, PERQ avail 0.702, any good = [0.563], simple 0.482
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.105133, -0.012774,  0.000000, /*                  occurences d01:  0.092358 */
		/*              resultLogScore @,%,! */  0.620297,  0.088577,  0.000000, /*              resultLogScore d01:  0.708875 */
		/*             passageLogScore @,%,! */ -0.133084,  0.548386,  0.072616, /*             passageLogScore d01:  0.342687 */
		/*                   originPsg @,%,! */ -0.095418, -0.274958,  0.072616, /*                   originPsg d01: -0.442991 */
		/*              originPsgFirst @,%,! */ -0.045848, -0.089045,  0.023046, /*              originPsgFirst d01: -0.157938 */
		/*                 originPsgNP @,%,! */  0.866323, -0.061058, -0.889125, /*                 originPsgNP d01:  1.694390 */
		/*                 originPsgNE @,%,! */  0.265324, -0.128953, -0.288126, /*                 originPsgNE d01:  0.424497 */
		/*        originPsgNPByLATSubj @,%,! */  0.149160,  0.032836, -0.171962, /*        originPsgNPByLATSubj d01:  0.353958 */
		/*              originDocTitle @,%,! */  0.640708,  0.251671, -0.663510, /*              originDocTitle d01:  1.555889 */
		/*               originConcept @,%,! */ -0.027057, -0.404721,  0.004255, /*               originConcept d01: -0.436033 */
		/*      originConceptBySubject @,%,! */  0.089048,  0.053394, -0.111850, /*      originConceptBySubject d01:  0.254293 */
		/*        originConceptByFocus @,%,! */  0.692647, -0.856279, -0.715449, /*        originConceptByFocus d01:  0.551817 */
		/*           originConceptByNE @,%,! */  0.282434, -0.349949, -0.305236, /*           originConceptByNE d01:  0.237720 */
		/*              originMultiple @,%,! */ -0.389051, -0.187027,  0.366249, /*              originMultiple d01: -0.942328 */
		/*                   spWordNet @,%,! */  0.104616,  0.173804,  0.560956, /*                   spWordNet d01: -0.282536 */
		/*               LATQNoWordNet @,%,! */ -0.518739,  0.000000,  0.495937, /*               LATQNoWordNet d01: -1.014675 */
		/*               LATANoWordNet @,%,! */  0.439393, -0.550340, -0.462195, /*               LATANoWordNet d01:  0.351249 */
		/*              tyCorPassageSp @,%,! */  1.559457, -0.098658,  0.149233, /*              tyCorPassageSp d01:  1.311565 */
		/*            tyCorPassageDist @,%,! */  0.310751, -0.019308,  0.149233, /*            tyCorPassageDist d01:  0.142210 */
		/*          tyCorPassageInside @,%,! */  0.177020, -0.044309, -0.199822, /*          tyCorPassageInside d01:  0.332532 */
		/*                 simpleScore @,%,! */  0.007372,  0.111191,  0.000000, /*                 simpleScore d01:  0.118563 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000, -0.022802, /*                    LATFocus d01:  0.022802 */
		/*               LATFocusProxy @,%,! */ -0.246662, -0.122955,  0.223860, /*               LATFocusProxy d01: -0.593477 */
		/*                       LATNE @,%,! */ -0.046515, -0.240323, -0.515208, /*                       LATNE d01:  0.228370 */
		/*                  LATDBpType @,%,! */  0.119586, -0.395831, -0.346089, /*                  LATDBpType d01:  0.069844 */
		/*                 tyCorSpQHit @,%,! */  0.566405, -0.128078, -0.589207, /*                 tyCorSpQHit d01:  1.027533 */
		/*                 tyCorSpAHit @,%,! */  0.444097, -0.128379, -0.466899, /*                 tyCorSpAHit d01:  0.782617 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000, -0.022802, /*             tyCorXHitAFocus d01:  0.022802 */
		/*                 tyCorAFocus @,%,! */ -0.653534,  0.188927,  0.630732, /*                 tyCorAFocus d01: -1.095339 */
		/*                    tyCorANE @,%,! */  0.187038, -0.277576, -0.209840, /*                    tyCorANE d01:  0.119303 */
		/*                   tyCorADBp @,%,! */  0.246196, -0.042479, -0.268998, /*                   tyCorADBp d01:  0.472716 */
		/*               tyCorAFocusSp @,%,! */  0.387237,  0.188927,  0.630732, /*               tyCorAFocusSp d01: -0.054568 */
		/*                  tyCorANESp @,%,! */ -0.513483,  0.276602, -0.209840, /*                  tyCorANESp d01: -0.027041 */
		/*                 tyCorADBpSp @,%,! */  0.270973, -0.028778, -0.268998, /*                 tyCorADBpSp d01:  0.511193 */
	};
	public static double intercept = -0.022802;

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
