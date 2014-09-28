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

import cz.brmlab.yodaqa.model.CandidateAnswer.AF_Occurences;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginDocTitle;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_PassageLogScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_ResultLogScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_SpWordNet;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;

/**
 * Annotate Answer view with score based on the present AnswerFeatures.
 * This particular implementation contains an extremely simple ad hoc
 * score computation that we have historically used. */


public class AnswerScoreSimple extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(AnswerScoreSimple.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	protected class AnswerScore {
		AnswerInfo ai;
		double score;

		public AnswerScore(AnswerInfo ai_, double score_) {
			ai = ai_;
			score = score_;
		}
	}

	public void process(JCas answerView) throws AnalysisEngineProcessException {
		List<AnswerScore> answers = new LinkedList<AnswerScore>();

		for (AnswerInfo ai : JCasUtil.select(answerView, AnswerInfo.class)) {
			AnswerFV fv = new AnswerFV(ai);

			double specificity;
			if (fv.isFeatureSet(AF_SpWordNet.class))
				specificity = fv.getFeatureValue(AF_SpWordNet.class);
			else
				specificity = Math.exp(-4);

			double passageLogScore = 0;
			if (fv.isFeatureSet(AF_PassageLogScore.class))
				passageLogScore = fv.getFeatureValue(AF_PassageLogScore.class);
			else if (fv.getFeatureValue(AF_OriginDocTitle.class) > 0.0)
				passageLogScore = Math.log(1 + 2);

			double score = specificity
				* fv.getFeatureValue(AF_Occurences.class)
				* passageLogScore
				* fv.getFeatureValue(AF_ResultLogScore.class);
			answers.add(new AnswerScore(ai, score));
		}

		/* Reindex the touched answer info(s). */
		/* XXX: This is somewhat more complex than it needs to be
		 * but later we will do this in FinalAnswer view where we
		 * will have many Answer objects. */
		for (AnswerScore as : answers) {
			as.ai.removeFromIndexes();
			as.ai.setConfidence(as.score);
			as.ai.addToIndexes();
		}
	}
}
