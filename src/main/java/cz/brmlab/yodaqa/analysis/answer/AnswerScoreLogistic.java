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
	 * (test) PERANS acc/prec/rcl/F2 = 0.796/0.060/0.542/0.207, @70 prec/rcl/F2 = 0.094/0.339/0.224, PERQ avail 0.749, any good = [0.447], simple 0.461
	 * (test) PERANS acc/prec/rcl/F2 = 0.785/0.058/0.559/0.206, @70 prec/rcl/F2 = 0.092/0.315/0.212, PERQ avail 0.698, any good = [0.514], simple 0.481
	 * (test) PERANS acc/prec/rcl/F2 = 0.785/0.070/0.568/0.233, @70 prec/rcl/F2 = 0.108/0.356/0.244, PERQ avail 0.726, any good = [0.487], simple 0.518
	 * (test) PERANS acc/prec/rcl/F2 = 0.733/0.060/0.650/0.219, @70 prec/rcl/F2 = 0.103/0.401/0.254, PERQ avail 0.767, any good = [0.435], simple 0.486
	 * (test) PERANS acc/prec/rcl/F2 = 0.748/0.059/0.612/0.213, @70 prec/rcl/F2 = 0.098/0.394/0.245, PERQ avail 0.740, any good = [0.505], simple 0.587
	 * (test) PERANS acc/prec/rcl/F2 = 0.766/0.060/0.597/0.214, @70 prec/rcl/F2 = 0.101/0.344/0.232, PERQ avail 0.716, any good = [0.504], simple 0.506
	 * (test) PERANS acc/prec/rcl/F2 = 0.740/0.060/0.645/0.219, @70 prec/rcl/F2 = 0.104/0.392/0.252, PERQ avail 0.744, any good = [0.500], simple 0.506
	 * (test) PERANS acc/prec/rcl/F2 = 0.759/0.062/0.608/0.220, @70 prec/rcl/F2 = 0.106/0.350/0.239, PERQ avail 0.735, any good = [0.440], simple 0.504
	 * (test) PERANS acc/prec/rcl/F2 = 0.761/0.058/0.585/0.207, @70 prec/rcl/F2 = 0.100/0.371/0.241, PERQ avail 0.730, any good = [0.516], simple 0.505
	 * (test) PERANS acc/prec/rcl/F2 = 0.778/0.061/0.579/0.214, @70 prec/rcl/F2 = 0.097/0.357/0.233, PERQ avail 0.721, any good = [0.457], simple 0.519
	 * Cross-validation score mean 48.055% S.D. 3.061%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.770/1.000/0.237/0.280, @70 prec/rcl/F2 = 1.000/0.085/0.104, PERQ avail 0.730, any good = [0.514], simple 0.507
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */ -0.009382, -0.043420,  0.000000, /*                  occurences d01: -0.052801 */
		/*              resultLogScore @,%,! */  0.580251,  0.053884,  0.000000, /*              resultLogScore d01:  0.634135 */
		/*             passageLogScore @,%,! */ -0.273198,  0.656342,  0.101859, /*             passageLogScore d01:  0.281285 */
		/*                   originPsg @,%,! */ -0.051811, -0.517343,  0.101859, /*                   originPsg d01: -0.671013 */
		/*              originPsgFirst @,%,! */  0.154242, -0.210786, -0.104195, /*              originPsgFirst d01:  0.047651 */
		/*                 originPsgNP @,%,! */  0.309037,  0.314983, -0.258989, /*                 originPsgNP d01:  0.883009 */
		/*                 originPsgNE @,%,! */ -0.221392,  0.152280,  0.271440, /*                 originPsgNE d01: -0.340552 */
		/*        originPsgNPByLATSubj @,%,! */  0.316617, -0.019127, -0.266569, /*        originPsgNPByLATSubj d01:  0.564058 */
		/*           originPsgSurprise @,%,! */  0.052000, -0.025623, -0.001952, /*           originPsgSurprise d01:  0.028329 */
		/*              originDocTitle @,%,! */  0.607932,  0.122423, -0.557885, /*              originDocTitle d01:  1.288240 */
		/*           originDBpRelation @,%,! */ -0.009544,  0.042795,  0.059592, /*           originDBpRelation d01: -0.026342 */
		/*               originConcept @,%,! */  0.005562, -0.344074,  0.044485, /*               originConcept d01: -0.382997 */
		/*      originConceptBySubject @,%,! */  0.394103, -0.132209, -0.344056, /*      originConceptBySubject d01:  0.605950 */
		/*          originConceptByLAT @,%,! */  0.367225, -0.595003, -0.317178, /*          originConceptByLAT d01:  0.089400 */
		/*           originConceptByNE @,%,! */  0.354360, -0.369084, -0.304312, /*           originConceptByNE d01:  0.289588 */
		/*              originMultiple @,%,! */ -0.039882, -0.226461,  0.089930, /*              originMultiple d01: -0.356272 */
		/*                   spWordNet @,%,! */  1.322339,  0.154134, -0.612967, /*                   spWordNet d01:  2.089441 */
		/*               LATQNoWordNet @,%,! */ -0.341467,  0.000000,  0.391515, /*               LATQNoWordNet d01: -0.732982 */
		/*               LATANoWordNet @,%,! */  0.130680, -0.015838, -0.080632, /*               LATANoWordNet d01:  0.195474 */
		/*              tyCorPassageSp @,%,! */  1.132340,  0.115273,  0.151415, /*              tyCorPassageSp d01:  1.096197 */
		/*            tyCorPassageDist @,%,! */  0.265469, -0.132397,  0.151415, /*            tyCorPassageDist d01: -0.018344 */
		/*          tyCorPassageInside @,%,! */ -0.082540,  0.138525,  0.132587, /*          tyCorPassageInside d01: -0.076602 */
		/*                 simpleScore @,%,! */  0.005614,  0.139253,  0.000000, /*                 simpleScore d01:  0.144867 */
		/*                       LATNE @,%,! */ -1.062540,  0.285600, -0.478071, /*                       LATNE d01: -0.298869 */
		/*                  LATDBpType @,%,! */  0.018135, -0.318547, -0.031235, /*                  LATDBpType d01: -0.269177 */
		/*                 LATQuantity @,%,! */ -0.185366, -0.085093,  0.235413, /*                 LATQuantity d01: -0.505872 */
		/*               LATQuantityCD @,%,! */  0.677908, -0.158455, -0.116266, /*               LATQuantityCD d01:  0.635719 */
		/*               LATWnInstance @,%,! */ -0.038620, -0.031568, -0.631469, /*               LATWnInstance d01:  0.561281 */
		/*              LATDBpRelation @,%,! */ -0.009544,  0.042795,  0.059592, /*              LATDBpRelation d01: -0.026342 */
		/*                 tyCorSpQHit @,%,! */  0.366915, -0.067269, -0.316867, /*                 tyCorSpQHit d01:  0.616513 */
		/*                 tyCorSpAHit @,%,! */ -0.316983, -0.284789,  0.367030, /*                 tyCorSpAHit d01: -0.968801 */
		/*                    tyCorANE @,%,! */  0.867256, -0.385665, -0.817209, /*                    tyCorANE d01:  1.298800 */
		/*                   tyCorADBp @,%,! */  0.674355, -0.285441, -0.624308, /*                   tyCorADBp d01:  1.013222 */
		/*              tyCorAQuantity @,%,! */ -0.045403,  0.029449,  0.095450, /*              tyCorAQuantity d01: -0.111404 */
		/*            tyCorAQuantityCD @,%,! */ -0.640884,  0.427574,  0.690932, /*            tyCorAQuantityCD d01: -0.904242 */
		/*            tyCorAWnInstance @,%,! */ -0.717623,  0.495491,  0.767671, /*            tyCorAWnInstance d01: -0.989803 */
		/*           tyCorADBpRelation @,%,! */ -0.244276,  0.176730,  0.294324, /*           tyCorADBpRelation d01: -0.361870 */
		/*                  tyCorANESp @,%,! */ -0.549649,  0.255381, -0.817209, /*                  tyCorANESp d01:  0.522941 */
		/*                 tyCorADBpSp @,%,! */ -0.545187,  0.114333, -0.624308, /*                 tyCorADBpSp d01:  0.193454 */
		/*            tyCorAQuantitySp @,%,! */ -0.006504,  0.029449,  0.095450, /*            tyCorAQuantitySp d01: -0.072506 */
		/*          tyCorAQuantityCDSp @,%,! */  1.140708,  0.427574,  0.690932, /*          tyCorAQuantityCDSp d01:  0.877350 */
		/*          tyCorAWnInstanceSp @,%,! */  1.405025, -0.264956,  0.767671, /*          tyCorAWnInstanceSp d01:  0.372399 */
		/*         tyCorADBpRelationSp @,%,! */ -0.549157,  0.046311,  0.294324, /*         tyCorADBpRelationSp d01: -0.797170 */
	};
	public static double intercept = 0.050047;

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
