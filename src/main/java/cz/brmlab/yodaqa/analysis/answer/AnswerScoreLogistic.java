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
	 * 430 answersets, 87245 answers
	 * + Cross-validation:
	 * (test) PERANS acc/prec/rcl/F2 = 0.786/0.060/0.587/0.212, @70 prec/rcl/F2 = 0.100/0.357/0.236, PERQ avail 0.647, any good = [0.537], simple 0.530
	 * (test) PERANS acc/prec/rcl/F2 = 0.760/0.064/0.612/0.225, @70 prec/rcl/F2 = 0.105/0.364/0.244, PERQ avail 0.726, any good = [0.507], simple 0.485
	 * (test) PERANS acc/prec/rcl/F2 = 0.800/0.069/0.558/0.231, @70 prec/rcl/F2 = 0.104/0.349/0.238, PERQ avail 0.721, any good = [0.478], simple 0.471
	 * (test) PERANS acc/prec/rcl/F2 = 0.780/0.062/0.605/0.221, @70 prec/rcl/F2 = 0.111/0.376/0.254, PERQ avail 0.721, any good = [0.520], simple 0.483
	 * (test) PERANS acc/prec/rcl/F2 = 0.785/0.058/0.570/0.205, @70 prec/rcl/F2 = 0.099/0.339/0.229, PERQ avail 0.698, any good = [0.465], simple 0.489
	 * (test) PERANS acc/prec/rcl/F2 = 0.748/0.064/0.609/0.225, @70 prec/rcl/F2 = 0.107/0.369/0.248, PERQ avail 0.740, any good = [0.505], simple 0.529
	 * (test) PERANS acc/prec/rcl/F2 = 0.761/0.064/0.627/0.228, @70 prec/rcl/F2 = 0.105/0.384/0.251, PERQ avail 0.726, any good = [0.514], simple 0.547
	 * (test) PERANS acc/prec/rcl/F2 = 0.769/0.061/0.611/0.217, @70 prec/rcl/F2 = 0.113/0.387/0.260, PERQ avail 0.716, any good = [0.491], simple 0.489
	 * (test) PERANS acc/prec/rcl/F2 = 0.770/0.057/0.581/0.206, @70 prec/rcl/F2 = 0.102/0.371/0.243, PERQ avail 0.665, any good = [0.537], simple 0.496
	 * (test) PERANS acc/prec/rcl/F2 = 0.727/0.064/0.660/0.230, @70 prec/rcl/F2 = 0.105/0.403/0.257, PERQ avail 0.716, any good = [0.578], simple 0.526
	 * Cross-validation score mean 51.333% S.D. 3.086%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.773/1.000/0.234/0.276, @70 prec/rcl/F2 = 1.000/0.086/0.106, PERQ avail 0.714, any good = [0.535], simple 0.516
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */ -0.005738, -0.047737,  0.000000, /*                  occurences d01: -0.053474 */
		/*              resultLogScore @,%,! */  0.589339,  0.075201,  0.000000, /*              resultLogScore d01:  0.664540 */
		/*             passageLogScore @,%,! */ -0.246619,  0.595741,  0.232489, /*             passageLogScore d01:  0.116633 */
		/*                   originPsg @,%,! */ -0.064789, -0.351194,  0.232489, /*                   originPsg d01: -0.648472 */
		/*              originPsgFirst @,%,! */  0.167704, -0.181294, -0.000004, /*              originPsgFirst d01: -0.013586 */
		/*                 originPsgNP @,%,! */  0.696950,  0.099396, -0.529251, /*                 originPsgNP d01:  1.325596 */
		/*                 originPsgNE @,%,! */ -0.006688,  0.091862,  0.174387, /*                 originPsgNE d01: -0.089213 */
		/*        originPsgNPByLATSubj @,%,! */  0.338608, -0.004704, -0.170909, /*        originPsgNPByLATSubj d01:  0.504813 */
		/*              originDocTitle @,%,! */  0.727736,  0.133078, -0.560037, /*              originDocTitle d01:  1.420851 */
		/*               originConcept @,%,! */  0.099455, -0.407294,  0.068245, /*               originConcept d01: -0.376084 */
		/*      originConceptBySubject @,%,! */  0.325607, -0.025267, -0.157907, /*      originConceptBySubject d01:  0.458248 */
		/*          originConceptByLAT @,%,! */  0.363440, -0.547734, -0.195741, /*          originConceptByLAT d01:  0.011446 */
		/*           originConceptByNE @,%,! */  0.371518, -0.351213, -0.203818, /*           originConceptByNE d01:  0.224123 */
		/*              originMultiple @,%,! */ -0.105195, -0.217196,  0.272895, /*              originMultiple d01: -0.595285 */
		/*                   spWordNet @,%,! */  0.918877,  0.275960, -0.735351, /*                   spWordNet d01:  1.930188 */
		/*               LATQNoWordNet @,%,! */ -0.255515,  0.000000,  0.423215, /*               LATQNoWordNet d01: -0.678730 */
		/*               LATANoWordNet @,%,! */  0.331689, -0.016403, -0.163989, /*               LATANoWordNet d01:  0.479275 */
		/*              tyCorPassageSp @,%,! */  1.967978, -0.115160,  0.164167, /*              tyCorPassageSp d01:  1.688651 */
		/*            tyCorPassageDist @,%,! */ -0.034100, -0.004269,  0.164167, /*            tyCorPassageDist d01: -0.202537 */
		/*          tyCorPassageInside @,%,! */ -0.111270,  0.125926,  0.278969, /*          tyCorPassageInside d01: -0.264313 */
		/*                 simpleScore @,%,! */  0.007918,  0.129201,  0.000000, /*                 simpleScore d01:  0.137118 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000,  0.167700, /*                    LATFocus d01: -0.167700 */
		/*               LATFocusProxy @,%,! */  0.000000,  0.000000,  0.167700, /*               LATFocusProxy d01: -0.167700 */
		/*                       LATNE @,%,! */ -0.900560,  0.227139, -0.469162, /*                       LATNE d01: -0.204259 */
		/*                  LATDBpType @,%,! */  0.020447, -0.324558, -0.050400, /*                  LATDBpType d01: -0.253712 */
		/*                 LATQuantity @,%,! */  0.223446, -0.222111,  0.215571, /*                 LATQuantity d01: -0.214236 */
		/*               LATQuantityCD @,%,! */  0.535283,  0.095903, -0.020839, /*               LATQuantityCD d01:  0.652025 */
		/*               LATWnInstance @,%,! */ -0.056903, -0.005355, -0.829961, /*               LATWnInstance d01:  0.767702 */
		/*                 tyCorSpQHit @,%,! */  0.301936, -0.023940, -0.134236, /*                 tyCorSpQHit d01:  0.412232 */
		/*                 tyCorSpAHit @,%,! */ -0.119238, -0.411375,  0.286937, /*                 tyCorSpAHit d01: -0.817550 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000,  0.167700, /*             tyCorXHitAFocus d01: -0.167700 */
		/*                 tyCorAFocus @,%,! */  0.000000,  0.000000,  0.167700, /*                 tyCorAFocus d01: -0.167700 */
		/*                    tyCorANE @,%,! */  1.088286, -0.096285, -0.920587, /*                    tyCorANE d01:  1.912589 */
		/*                   tyCorADBp @,%,! */  0.860584, -0.203048, -0.692885, /*                   tyCorADBp d01:  1.350421 */
		/*              tyCorAQuantity @,%,! */ -0.808453,  0.812417,  0.976153, /*              tyCorAQuantity d01: -0.972189 */
		/*            tyCorAWnInstance @,%,! */  0.739342, -0.247146, -0.571642, /*            tyCorAWnInstance d01:  1.063839 */
	};
	public static double intercept = 0.167700;

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
