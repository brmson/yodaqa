package cz.brmlab.yodaqa.analysis.ansscore;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.analysis.ansscore.AF;

/**
 * Record the answer confidence in the feature vector.
 */

public class AnswerScoreToFV extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(AnswerScoreToFV.class);

	/**
	 * Pipeline phase in which we are scoring.  We may be scoring
	 * multiple times and will use different features to differentiate
	 * the model scores.
	 */
	public static final String PARAM_SCORING_PHASE = "SCORING_PHASE";
	@ConfigurationParameter(name = PARAM_SCORING_PHASE, mandatory = true)
	protected String scoringPhase;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		if (scoringPhase.equals("2"))
			return;

		ArrayList<Answer> answers = new ArrayList<Answer>();
		for (Answer a : JCasUtil.select(jcas, Answer.class)) {
			answers.add(a);
		}

		for (Answer a : answers) {
			a.removeFromIndexes();

			AnswerFV fv = new AnswerFV(a);
			if (scoringPhase.equals(""))
				fv.setFeature(AF.Phase0Score, a.getConfidence());
			else if (scoringPhase.equals("1"))
				fv.setFeature(AF.Phase1Score, a.getConfidence());

			for (FeatureStructure af : a.getFeatures().toArray())
				((AnswerFeature) af).removeFromIndexes();
			a.setFeatures(fv.toFSArray(jcas));

			a.addToIndexes();
		}
	}
}
