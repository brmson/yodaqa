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
	 * (test) PERANS acc/prec/rcl/F2 = 0.777/0.052/0.578/0.192, @70 prec/rcl/F2 = 0.087/0.365/0.223, PERQ avail 0.684, any good = [0.505], simple 0.501
	 * (test) PERANS acc/prec/rcl/F2 = 0.791/0.069/0.598/0.236, @70 prec/rcl/F2 = 0.115/0.388/0.263, PERQ avail 0.721, any good = [0.583], simple 0.536
	 * (test) PERANS acc/prec/rcl/F2 = 0.816/0.069/0.562/0.232, @70 prec/rcl/F2 = 0.111/0.356/0.247, PERQ avail 0.702, any good = [0.556], simple 0.481
	 * (test) PERANS acc/prec/rcl/F2 = 0.789/0.067/0.596/0.231, @70 prec/rcl/F2 = 0.119/0.399/0.271, PERQ avail 0.698, any good = [0.499], simple 0.534
	 * (test) PERANS acc/prec/rcl/F2 = 0.769/0.070/0.641/0.243, @70 prec/rcl/F2 = 0.126/0.434/0.292, PERQ avail 0.730, any good = [0.523], simple 0.528
	 * (test) PERANS acc/prec/rcl/F2 = 0.774/0.062/0.619/0.221, @70 prec/rcl/F2 = 0.107/0.370/0.248, PERQ avail 0.730, any good = [0.571], simple 0.526
	 * (test) PERANS acc/prec/rcl/F2 = 0.784/0.064/0.589/0.224, @70 prec/rcl/F2 = 0.113/0.377/0.257, PERQ avail 0.712, any good = [0.461], simple 0.489
	 * (test) PERANS acc/prec/rcl/F2 = 0.790/0.065/0.601/0.226, @70 prec/rcl/F2 = 0.112/0.369/0.254, PERQ avail 0.721, any good = [0.547], simple 0.496
	 * (test) PERANS acc/prec/rcl/F2 = 0.814/0.066/0.567/0.224, @70 prec/rcl/F2 = 0.108/0.338/0.237, PERQ avail 0.693, any good = [0.461], simple 0.475
	 * (test) PERANS acc/prec/rcl/F2 = 0.777/0.064/0.612/0.226, @70 prec/rcl/F2 = 0.104/0.411/0.258, PERQ avail 0.702, any good = [0.469], simple 0.494
	 * Cross-validation score mean 51.749% S.D. 4.340%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.784/1.000/0.223/0.265, @70 prec/rcl/F2 = 1.000/0.086/0.106, PERQ avail 0.714, any good = [0.555], simple 0.521
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */  0.029877,  0.030009,  0.000000, /*                  occurences d01:  0.059887 */
		/*              resultLogScore @,%,! */  0.558494,  0.079677,  0.000000, /*              resultLogScore d01:  0.638171 */
		/*             passageLogScore @,%,! */ -0.365127,  0.656421,  0.316126, /*             passageLogScore d01: -0.024831 */
		/*                   originPsg @,%,! */ -0.166157, -0.307921,  0.316126, /*                   originPsg d01: -0.790203 */
		/*              originPsgFirst @,%,! */  0.134003, -0.176990,  0.015966, /*              originPsgFirst d01: -0.058954 */
		/*                 originPsgNP @,%,! */  0.835853,  0.039292, -0.685884, /*                 originPsgNP d01:  1.561029 */
		/*                 originPsgNE @,%,! */ -0.026276,  0.171436,  0.176245, /*                 originPsgNE d01: -0.031085 */
		/*        originPsgNPByLATSubj @,%,! */  0.275223, -0.002363, -0.125254, /*        originPsgNPByLATSubj d01:  0.398114 */
		/*              originDocTitle @,%,! */  0.701672,  0.243747, -0.551703, /*              originDocTitle d01:  1.497121 */
		/*               originConcept @,%,! */  0.056832, -0.348861,  0.093137, /*               originConcept d01: -0.385165 */
		/*      originConceptBySubject @,%,! */  0.274874, -0.003476, -0.124905, /*      originConceptBySubject d01:  0.396302 */
		/*          originConceptByLAT @,%,! */  0.250376, -0.504549, -0.100407, /*          originConceptByLAT d01: -0.153766 */
		/*           originConceptByNE @,%,! */  0.331868, -0.340112, -0.181899, /*           originConceptByNE d01:  0.173655 */
		/*              originMultiple @,%,! */ -0.274329, -0.231580,  0.424297, /*              originMultiple d01: -0.930206 */
		/*                   spWordNet @,%,! */  0.868400,  0.303885, -0.653756, /*                   spWordNet d01:  1.826042 */
		/*               LATQNoWordNet @,%,! */ -0.334033,  0.000000,  0.484002, /*               LATQNoWordNet d01: -0.818035 */
		/*               LATANoWordNet @,%,! */  0.278469,  0.038692, -0.128500, /*               LATANoWordNet d01:  0.445662 */
		/*              tyCorPassageSp @,%,! */  1.940806, -0.074143,  0.168221, /*              tyCorPassageSp d01:  1.698442 */
		/*            tyCorPassageDist @,%,! */ -0.112481, -0.023171,  0.168221, /*            tyCorPassageDist d01: -0.303872 */
		/*          tyCorPassageInside @,%,! */  0.141366,  0.046169,  0.008603, /*          tyCorPassageInside d01:  0.178932 */
		/*                 simpleScore @,%,! */  0.012704,  0.117876,  0.000000, /*                 simpleScore d01:  0.130581 */
		/*                    LATFocus @,%,! */  0.000000,  0.000000,  0.149969, /*                    LATFocus d01: -0.149969 */
		/*               LATFocusProxy @,%,! */  0.000000,  0.000000,  0.149969, /*               LATFocusProxy d01: -0.149969 */
		/*                       LATNE @,%,! */ -0.861127,  0.240673, -0.382644, /*                       LATNE d01: -0.237809 */
		/*                  LATDBpType @,%,! */  0.022514, -0.348458, -0.032189, /*                  LATDBpType d01: -0.293755 */
		/*                 LATQuantity @,%,! */  0.299842, -0.221148,  0.274479, /*                 LATQuantity d01: -0.195785 */
		/*               LATQuantityCD @,%,! */  0.588674,  0.097815,  0.062341, /*               LATQuantityCD d01:  0.624148 */
		/*               LATWnInstance @,%,! */ -0.034749, -0.025626, -0.915493, /*               LATWnInstance d01:  0.855118 */
		/*                 tyCorSpQHit @,%,! */  0.390303, -0.080578, -0.240334, /*                 tyCorSpQHit d01:  0.550059 */
		/*                 tyCorSpAHit @,%,! */ -0.030015, -0.482728,  0.179984, /*                 tyCorSpAHit d01: -0.692728 */
		/*             tyCorXHitAFocus @,%,! */  0.000000,  0.000000,  0.149969, /*             tyCorXHitAFocus d01: -0.149969 */
		/*                 tyCorAFocus @,%,! */  0.000000,  0.000000,  0.149969, /*                 tyCorAFocus d01: -0.149969 */
		/*                    tyCorANE @,%,! */  1.061030, -0.083277, -0.911061, /*                    tyCorANE d01:  1.888814 */
		/*                   tyCorADBp @,%,! */  0.861231, -0.197142, -0.711262, /*                   tyCorADBp d01:  1.375350 */
		/*              tyCorAQuantity @,%,! */ -1.040891,  0.888691,  1.190860, /*              tyCorAQuantity d01: -1.343060 */
		0, 0, 0,
		/*            tyCorAWnInstance @,%,! */  0.894690, -0.297929, -0.744721, /*            tyCorAWnInstance d01:  1.341481 */
	};
	public static double intercept = 0.149969;

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
