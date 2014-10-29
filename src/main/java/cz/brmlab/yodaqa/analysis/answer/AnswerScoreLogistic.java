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
	 * (test) PERANS acc/prec/rcl/F2 = 0.776/0.062/0.583/0.217, @70 prec/rcl/F2 = 0.100/0.351/0.234, PERQ avail 0.679, any good = [0.518], simple 0.505
	 * (test) PERANS acc/prec/rcl/F2 = 0.773/0.063/0.621/0.223, @70 prec/rcl/F2 = 0.106/0.370/0.248, PERQ avail 0.702, any good = [0.513], simple 0.466
	 * (test) PERANS acc/prec/rcl/F2 = 0.764/0.063/0.640/0.225, @70 prec/rcl/F2 = 0.112/0.405/0.266, PERQ avail 0.684, any good = [0.580], simple 0.521
	 * (test) PERANS acc/prec/rcl/F2 = 0.777/0.061/0.588/0.216, @70 prec/rcl/F2 = 0.104/0.357/0.240, PERQ avail 0.698, any good = [0.466], simple 0.548
	 * (test) PERANS acc/prec/rcl/F2 = 0.754/0.062/0.643/0.223, @70 prec/rcl/F2 = 0.105/0.388/0.251, PERQ avail 0.735, any good = [0.468], simple 0.496
	 * (test) PERANS acc/prec/rcl/F2 = 0.769/0.068/0.605/0.235, @70 prec/rcl/F2 = 0.107/0.354/0.242, PERQ avail 0.744, any good = [0.500], simple 0.500
	 * (test) PERANS acc/prec/rcl/F2 = 0.775/0.064/0.588/0.222, @70 prec/rcl/F2 = 0.114/0.382/0.259, PERQ avail 0.721, any good = [0.526], simple 0.512
	 * (test) PERANS acc/prec/rcl/F2 = 0.752/0.062/0.621/0.221, @70 prec/rcl/F2 = 0.115/0.400/0.268, PERQ avail 0.767, any good = [0.512], simple 0.520
	 * (test) PERANS acc/prec/rcl/F2 = 0.796/0.069/0.564/0.231, @70 prec/rcl/F2 = 0.112/0.319/0.233, PERQ avail 0.688, any good = [0.564], simple 0.532
	 * (test) PERANS acc/prec/rcl/F2 = 0.773/0.059/0.592/0.210, @70 prec/rcl/F2 = 0.105/0.386/0.252, PERQ avail 0.688, any good = [0.525], simple 0.490
	 * Cross-validation score mean 51.727% S.D. 3.415%
	 * + Full training set:
	 * (full) PERANS acc/prec/rcl/F2 = 0.768/1.000/0.240/0.283, @70 prec/rcl/F2 = 1.000/0.085/0.104, PERQ avail 0.714, any good = [0.528], simple 0.516
	 * Full model is LogisticRegression(C=1.0, class_weight=auto, dual=False, fit_intercept=True,
		  intercept_scaling=1, penalty=l2, random_state=None, tol=0.0001)
	 */
	public static double weights[] = {
		/*                  occurences @,%,! */ -0.023283, -0.061358,  0.000000, /*                  occurences d01: -0.084641 */
		/*              resultLogScore @,%,! */  0.638492,  0.072135,  0.000000, /*              resultLogScore d01:  0.710627 */
		/*             passageLogScore @,%,! */ -0.671001,  0.732957,  0.353562, /*             passageLogScore d01: -0.291606 */
		/*                   originPsg @,%,! */ -0.149358, -0.415656,  0.353562, /*                   originPsg d01: -0.918576 */
		/*              originPsgFirst @,%,! */  0.415207, -0.329346, -0.211004, /*              originPsgFirst d01:  0.296866 */
		/*           originPsgByClueSV @,%,! */  0.280701, -0.020741, -0.076498, /*           originPsgByClueSV d01:  0.336457 */
		/*           originPsgByClueNE @,%,! */  0.190334, -0.170254,  0.013869, /*           originPsgByClueNE d01:  0.006212 */
		/*          originPsgByClueLAT @,%,! */ -0.091978,  0.194152,  0.296182, /*          originPsgByClueLAT d01: -0.194008 */
		/*      originPsgByClueSubject @,%,! */  0.633577, -0.521748, -0.429374, /*      originPsgByClueSubject d01:  0.541202 */
		/*      originPsgByClueConcept @,%,! */  0.242159,  0.079629, -0.037956, /*      originPsgByClueConcept d01:  0.359744 */
		/*                 originPsgNP @,%,! */  0.776793,  0.060154, -0.572590, /*                 originPsgNP d01:  1.409537 */
		/*                 originPsgNE @,%,! */ -0.088796,  0.170910,  0.292999, /*                 originPsgNE d01: -0.210884 */
		/*        originPsgNPByLATSubj @,%,! */  0.250619,  0.031617, -0.046416, /*        originPsgNPByLATSubj d01:  0.328652 */
		/*           originPsgSurprise @,%,! */ -0.029984,  0.079735,  0.234187, /*           originPsgSurprise d01: -0.184436 */
		/*              originDocTitle @,%,! */  0.877930,  0.149200, -0.673727, /*              originDocTitle d01:  1.700857 */
		/*               originConcept @,%,! */  0.079304, -0.381502,  0.124899, /*               originConcept d01: -0.427097 */
		/*      originConceptBySubject @,%,! */  0.193307,  0.051409,  0.010896, /*      originConceptBySubject d01:  0.233820 */
		/*          originConceptByLAT @,%,! */  0.593132, -0.678137, -0.388929, /*          originConceptByLAT d01:  0.303924 */
		/*           originConceptByNE @,%,! */  0.260404, -0.266523, -0.056200, /*           originConceptByNE d01:  0.050081 */
		/*              originMultiple @,%,! */ -0.146275, -0.178864,  0.350478, /*              originMultiple d01: -0.675617 */
		/*                   spWordNet @,%,! */  0.820654,  0.291407, -0.785832, /*                   spWordNet d01:  1.897893 */
		/*               LATQNoWordNet @,%,! */ -0.216045,  0.000000,  0.420248, /*               LATQNoWordNet d01: -0.636293 */
		/*               LATANoWordNet @,%,! */  0.384890, -0.011867, -0.180686, /*               LATANoWordNet d01:  0.553709 */
		/*              tyCorPassageSp @,%,! */  1.960176, -0.124633,  0.147722, /*              tyCorPassageSp d01:  1.687821 */
		/*            tyCorPassageDist @,%,! */ -0.071247,  0.004227,  0.147722, /*            tyCorPassageDist d01: -0.214742 */
		/*          tyCorPassageInside @,%,! */ -0.078330,  0.122549,  0.282534, /*          tyCorPassageInside d01: -0.238315 */
		/*                 simpleScore @,%,! */  0.006041,  0.143726,  0.000000, /*                 simpleScore d01:  0.149767 */
		/*                       LATNE @,%,! */ -0.732357,  0.147525, -0.492709, /*                       LATNE d01: -0.092122 */
		/*                  LATDBpType @,%,! */  0.016676, -0.285805, -0.053686, /*                  LATDBpType d01: -0.215443 */
		/*                 LATQuantity @,%,! */ -0.182949, -0.066325,  0.387152, /*                 LATQuantity d01: -0.636426 */
		/*               LATQuantityCD @,%,! */  0.739772, -0.140536,  0.107104, /*               LATQuantityCD d01:  0.492131 */
		/*               LATWnInstance @,%,! */ -0.065160,  0.017705, -0.892191, /*               LATWnInstance d01:  0.844736 */
		/*                 tyCorSpQHit @,%,! */  0.330222, -0.018987, -0.126019, /*                 tyCorSpQHit d01:  0.437254 */
		/*                 tyCorSpAHit @,%,! */ -0.104220, -0.416551,  0.308423, /*                 tyCorSpAHit d01: -0.829194 */
		/*                    tyCorANE @,%,! */  0.968180, -0.039889, -0.763977, /*                    tyCorANE d01:  1.692267 */
		/*                   tyCorADBp @,%,! */  0.873686, -0.213606, -0.669482, /*                   tyCorADBp d01:  1.329562 */
		/*              tyCorAQuantity @,%,! */ -0.044750,  0.057901,  0.248953, /*              tyCorAQuantity d01: -0.235802 */
		/*            tyCorAQuantityCD @,%,! */ -0.749145,  0.771057,  0.953348, /*            tyCorAQuantityCD d01: -0.931435 */
		/*            tyCorAWnInstance @,%,! */  0.704743, -0.235019, -0.500540, /*            tyCorAWnInstance d01:  0.970265 */
	};
	public static double intercept = 0.204203;

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
