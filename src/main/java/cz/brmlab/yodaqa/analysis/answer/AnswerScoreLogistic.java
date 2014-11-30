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
	 * 430 answersets, 91421 answers
	 * + Cross-validation:
	 * (test) PERANS acc/prec/rcl/F2 = 0.781/0.068/0.594/0.233, @70 prec/rcl/F2 = 0.120/0.358/0.257, PERQ avail 0.730, any good = [0.506], simple 0.504
	 * (test) PERANS acc/prec/rcl/F2 = 0.761/0.059/0.635/0.215, @70 prec/rcl/F2 = 0.104/0.364/0.242, PERQ avail 0.744, any good = [0.481], simple 0.505
	 * (test) PERANS acc/prec/rcl/F2 = 0.774/0.060/0.615/0.216, @70 prec/rcl/F2 = 0.107/0.342/0.238, PERQ avail 0.730, any good = [0.475], simple 0.491
	 * (test) PERANS acc/prec/rcl/F2 = 0.765/0.062/0.587/0.219, @70 prec/rcl/F2 = 0.106/0.364/0.245, PERQ avail 0.772, any good = [0.500], simple 0.549
	 * (test) PERANS acc/prec/rcl/F2 = 0.762/0.055/0.563/0.199, @70 prec/rcl/F2 = 0.094/0.335/0.221, PERQ avail 0.749, any good = [0.534], simple 0.549
	 * (test) PERANS acc/prec/rcl/F2 = 0.774/0.058/0.579/0.207, @70 prec/rcl/F2 = 0.105/0.347/0.238, PERQ avail 0.721, any good = [0.466], simple 0.456
	 * (test) PERANS acc/prec/rcl/F2 = 0.727/0.057/0.655/0.212, @70 prec/rcl/F2 = 0.102/0.394/0.251, PERQ avail 0.740, any good = [0.493], simple 0.522
	 * (test) PERANS acc/prec/rcl/F2 = 0.755/0.056/0.582/0.203, @70 prec/rcl/F2 = 0.090/0.322/0.212, PERQ avail 0.730, any good = [0.516], simple 0.496
	 * (test) PERANS acc/prec/rcl/F2 = 0.764/0.057/0.590/0.205, @70 prec/rcl/F2 = 0.096/0.344/0.226, PERQ avail 0.716, any good = [0.463], simple 0.518
	 * (test) PERANS acc/prec/rcl/F2 = 0.720/0.059/0.704/0.222, @70 prec/rcl/F2 = 0.121/0.410/0.277, PERQ avail 0.726, any good = [0.514], simple 0.541
	 * Cross-validation score mean 49.478% S.D. 2.219%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.767/1.000/0.239/0.282, @70 prec/rcl/F2 = 1.000/0.084/0.103, PERQ avail 0.730, any good = [0.531], simple 0.510
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */ -0.011020, -0.031052,  0.000000, /*                  occurences d01: -0.042072 */
		/*              resultLogScore @,%,! */  0.565235,  0.047445,  0.000000, /*              resultLogScore d01:  0.612680 */
		/*             passageLogScore @,%,! */ -0.216939,  0.647013,  0.151152, /*             passageLogScore d01:  0.278923 */
		/*                   originPsg @,%,! */ -0.068426, -0.435525,  0.151152, /*                   originPsg d01: -0.655103 */
		/*              originPsgFirst @,%,! */  0.149380, -0.193078, -0.066654, /*              originPsgFirst d01:  0.022956 */
		/*                 originPsgNP @,%,! */  0.427402,  0.185231, -0.344676, /*                 originPsgNP d01:  0.957308 */
		/*                 originPsgNE @,%,! */ -0.196863,  0.119346,  0.279589, /*                 originPsgNE d01: -0.357106 */
		/*        originPsgNPByLATSubj @,%,! */  0.276542, -0.007062, -0.193817, /*        originPsgNPByLATSubj d01:  0.463296 */
		/*           originPsgSurprise @,%,! */  0.095243, -0.054551, -0.012517, /*           originPsgSurprise d01:  0.053209 */
		/*              originDocTitle @,%,! */  0.528086,  0.146673, -0.445361, /*              originDocTitle d01:  1.120120 */
		/*           originDBpRelation @,%,! */  0.073038,  0.017856,  0.009687, /*           originDBpRelation d01:  0.081207 */
		/*               originConcept @,%,! */  0.021799, -0.318013,  0.060927, /*               originConcept d01: -0.357140 */
		/*      originConceptBySubject @,%,! */  0.414607, -0.134188, -0.331882, /*      originConceptBySubject d01:  0.612302 */
		/*          originConceptByLAT @,%,! */  0.454554, -0.653654, -0.371829, /*          originConceptByLAT d01:  0.172729 */
		/*           originConceptByNE @,%,! */  0.400042, -0.398756, -0.317317, /*           originConceptByNE d01:  0.318603 */
		/*              originMultiple @,%,! */ -0.092733, -0.172670,  0.175458, /*              originMultiple d01: -0.440861 */
		/*                   spWordNet @,%,! */ -0.141533,  0.234833, -0.465347, /*                   spWordNet d01:  0.558647 */
		/*               LATQNoWordNet @,%,! */ -0.323917,  0.000000,  0.406642, /*               LATQNoWordNet d01: -0.730559 */
		/*               LATANoWordNet @,%,! */  0.197666, -0.131097, -0.114940, /*               LATANoWordNet d01:  0.181509 */
		/*              tyCorPassageSp @,%,! */  1.493832, -0.033635,  0.151623, /*              tyCorPassageSp d01:  1.308573 */
		/*            tyCorPassageDist @,%,! */  0.264081, -0.077265,  0.151623, /*            tyCorPassageDist d01:  0.035193 */
		/*          tyCorPassageInside @,%,! */  0.013910,  0.053769,  0.068815, /*          tyCorPassageInside d01: -0.001136 */
		/*                 simpleScore @,%,! */  0.005152,  0.124129,  0.000000, /*                 simpleScore d01:  0.129281 */
		/*                       LATNE @,%,! */ -0.254634,  0.268415,  0.337359, /*                       LATNE d01: -0.323577 */
		/*                  LATDBpType @,%,! */  0.792816, -0.777972, -0.710091, /*                  LATDBpType d01:  0.724936 */
		/*                 LATQuantity @,%,! */ -0.194370, -0.080417,  0.277095, /*                 LATQuantity d01: -0.551882 */
		/*               LATQuantityCD @,%,! */  0.656620, -0.247561, -0.573894, /*               LATQuantityCD d01:  0.982953 */
		/*               LATWnInstance @,%,! */  0.336435, -0.128505, -0.253710, /*               LATWnInstance d01:  0.461640 */
		/*              LATDBpRelation @,%,! */  0.073038,  0.017856,  0.009687, /*              LATDBpRelation d01:  0.081207 */
		/*                 tyCorSpQHit @,%,! */  0.618110, -0.013223, -0.535385, /*                 tyCorSpQHit d01:  1.140272 */
		/*                 tyCorSpAHit @,%,! */ -0.068557, -0.386522,  0.151282, /*                 tyCorSpAHit d01: -0.606362 */
		/*                    tyCorANE @,%,! */  1.069453, -0.107889, -0.986727, /*                    tyCorANE d01:  1.948291 */
		/*                   tyCorADBp @,%,! */  0.846031, -0.160854, -0.763305, /*                   tyCorADBp d01:  1.448482 */
		/*              tyCorAQuantity @,%,! */ -0.041670,  0.050137,  0.124396, /*              tyCorAQuantity d01: -0.115929 */
		/*            tyCorAQuantityCD @,%,! */ -0.825221,  0.820825,  0.907946, /*            tyCorAQuantityCD d01: -0.912342 */
		/*            tyCorAWnInstance @,%,! */ -0.513567,  0.243623,  0.596293, /*            tyCorAWnInstance d01: -0.866237 */
		/*           tyCorADBpRelation @,%,! */ -0.206801,  0.199799,  0.289526, /*           tyCorADBpRelation d01: -0.296527 */
	};
	public static double intercept = 0.082725;

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
