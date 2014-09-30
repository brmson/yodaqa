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
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginNE;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_PassageLogScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_ResultLogScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_SpWordNet;
import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;

/**
 * Annotate the AnswerHitlistCAS Answer FSes with score based on the
 * present AnswerFeatures.  This particular implementation contains
 * an extremely simple ad hoc score computation that we have
 * historically used. */


public class AnswerScoreSimple extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(AnswerScoreSimple.class);

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

	public static double scoreAnswer(Answer a) {
		AnswerFV fv = new AnswerFV(a);

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

		double neBonus = 0;
		if (fv.isFeatureSet(AF_OriginNE.class))
			neBonus = 1;

		double score = specificity
			* Math.exp(neBonus)
			* fv.getFeatureValue(AF_Occurences.class)
			* passageLogScore
			* fv.getFeatureValue(AF_ResultLogScore.class);
		return score;
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		List<AnswerScore> answers = new LinkedList<AnswerScore>();

		for (Answer a : JCasUtil.select(jcas, Answer.class)) {
			double score = scoreAnswer(a);
			answers.add(new AnswerScore(a, score));
		}

		/* Reindex the touched answer info(s). */
		for (AnswerScore as : answers) {
			as.a.removeFromIndexes();
			as.a.setConfidence(as.score);
			as.a.addToIndexes();
		}
	}
}
