package cz.brmlab.yodaqa.analysis.ansscore;

import java.util.LinkedList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
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
		if (fv.isFeatureSet(AF.SpWordNet))
			specificity = fv.getFeatureValue(AF.SpWordNet);
		else
			specificity = Math.exp(-4);

		double passageLogScore = 0;
		if (fv.isFeatureSet(AF.PassageLogScore))
			passageLogScore = fv.getFeatureValue(AF.PassageLogScore);
		else if (fv.getFeatureValue(AF.OriginDocTitle) > 0.0)
			passageLogScore = Math.log(1 + 2);

		double neBonus = 0;
		if (fv.isFeatureSet(AF.OriginPsgNE))
			neBonus = 1;

		double score = specificity
			* Math.exp(neBonus)
			* fv.getFeatureValue(AF.Occurences)
			* passageLogScore
			* fv.getFeatureValue(AF.ResultLogScore);
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

			/* If we used this as true scorer... */
			// as.a.setConfidence(as.score);

			/* ...but instead we just add it as an extra feature
			 * for a more complex scorer. */
			AnswerFV fv = new AnswerFV(as.a);
			fv.setFeature(AF.SimpleScore, as.score);

			for (FeatureStructure af : as.a.getFeatures().toArray())
				((AnswerFeature) af).removeFromIndexes();
			as.a.setFeatures(fv.toFSArray(jcas));

			as.a.addToIndexes();
		}
	}
}
