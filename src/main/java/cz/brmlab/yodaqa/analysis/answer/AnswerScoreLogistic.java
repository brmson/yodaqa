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
	 * Cross-validation score mean 51.178% S.D. 3.119%
	 * (fullset) PERANS acc/prec/rcl/F2 = 0.757/1.000/0.250/0.294, @70 prec/rcl/F2 = 1.000/0.082/0.101, PERQ avail 0.702, any good = [0.513], simple 0.460
	 *
	 *          @occurences  0.1258           %occurences -0.0290           !occurences  0.0000
	 *      @resultLogScore  0.7576       %resultLogScore  0.0438       !resultLogScore  0.0000
	 *     @passageLogScore -0.2755      %passageLogScore  0.6039      !passageLogScore -0.1215
	 *           @originPsg -0.0221            %originPsg -0.3654            !originPsg -0.1215
	 *      @originPsgFirst -0.3010       %originPsgFirst  0.0557       !originPsgFirst  0.1574
	 *         @originPsgNP  0.2489          %originPsgNP -0.0700          !originPsgNP -0.3924
	 *         @originPsgNE -0.3755          %originPsgNE -0.1463          !originPsgNE  0.2320
	 *      @originDocTitle  0.3956       %originDocTitle  0.1359       !originDocTitle -0.5391
	 *       @originConcept  0.0006        %originConcept -0.5406        !originConcept -0.1441
	 *      @originMultiple  0.0374       %originMultiple -0.1025       !originMultiple -0.1810
	 *           @spWordNet -0.6022            %spWordNet  0.2564            !spWordNet  0.2293
	 *       @LATQNoWordNet -0.5699        %LATQNoWordNet  0.0000        !LATQNoWordNet  0.4263
	 *       @LATANoWordNet  0.9053        %LATANoWordNet -0.8291        !LATANoWordNet -1.0488
	 *      @tyCorPassageSp  1.9756       %tyCorPassageSp -0.2309       !tyCorPassageSp  0.1227
	 *    @tyCorPassageDist -0.1201     %tyCorPassageDist  0.1607     !tyCorPassageDist  0.1227
	 *  @tyCorPassageInside -0.0393   %tyCorPassageInside -0.0099   !tyCorPassageInside -0.1042
	 *         @simpleScore -0.0083          %simpleScore  0.1118          !simpleScore  0.0000
	 *            @LATFocus  0.0000             %LATFocus  0.0000             !LATFocus -0.1435
	 *       @LATFocusProxy -0.9240        %LATFocusProxy  0.1169        !LATFocusProxy  0.7805
	 *               @LATNE  1.0625                %LATNE -0.5515                !LATNE -0.3980
	 *         @tyCorSpQHit -0.1530          %tyCorSpQHit  0.4753          !tyCorSpQHit  0.0094
	 *         @tyCorSpAHit  1.6573          %tyCorSpAHit -0.8405          !tyCorSpAHit -1.8009
	 *     @tyCorXHitAFocus  0.0000      %tyCorXHitAFocus  0.0000      !tyCorXHitAFocus -0.1435
	 */
	public static double weights[] = {
		  1.25798789e-01,  -2.89630927e-02,   0.00000000e+00,
		  7.57565458e-01,   4.38275109e-02,   0.00000000e+00,
		 -2.75491207e-01,   6.03918991e-01,  -1.21457238e-01,
		 -2.20676272e-02,  -3.65417313e-01,  -1.21457238e-01,
		 -3.00969186e-01,   5.56562470e-02,   1.57444321e-01,
		  2.48872780e-01,  -6.99880274e-02,  -3.92397645e-01,
		 -3.75548202e-01,  -1.46310859e-01,   2.32023337e-01,
		  3.95559616e-01,   1.35873965e-01,  -5.39084481e-01,
		  6.09594994e-04,  -5.40605882e-01,  -1.44134460e-01,
		  3.74455383e-02,  -1.02515975e-01,  -1.80970403e-01,
		 -6.02231014e-01,   2.56400767e-01,   2.29310839e-01,
		 -5.69873030e-01,   0.00000000e+00,   4.26348165e-01,
		  9.05252391e-01,  -8.29148649e-01,  -1.04877726e+00,
		  1.97557184e+00,  -2.30921587e-01,   1.22704454e-01,
		 -1.20112541e-01,   1.60674952e-01,   1.22704454e-01,
		 -3.93304737e-02,  -9.90983432e-03,  -1.04194391e-01,
		 -8.34758593e-03,   1.11763036e-01,   0.00000000e+00,
		  0.00000000e+00,   0.00000000e+00,  -1.43524865e-01,
		 -9.23977633e-01,   1.16907792e-01,   7.80452768e-01,
		  1.06250856e+00,  -5.51473877e-01,  -3.98033716e-01,
		 -1.52963239e-01,   4.75250600e-01,   9.43837362e-03,
		  1.65733704e+00,  -8.40516580e-01,  -1.80086191e+00,
		  0.00000000e+00,   0.00000000e+00,  -1.43524865e-01,
	};
	public static double intercept = -0.14352486;

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
