package cz.brmlab.yodaqa.pipeline;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.answer.AnswerFV;
import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_MergedSyntaxScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;

/**
 * Merge textually similar answers in AnswerHitlistCAS.  Right now,
 * we just determine syntactic equivalence based on canonText
 * (dropping "the"-ish stuff, whitespaces and interpunction).
 *
 * (Do not confuse with AnswerCASMerger which just puts all CASes
 * of candidate answers together in a single CAS - the hitlist.)
 *
 * TODO: Produce more merging engines, e.g. LAT-specific mergers (esp. for
 * person names and dates) or "ignore everything up to the last comma" or
 * "drop parentheses" mergers.  Then, we would rename this to
 * AnswerSyntacticMerger and make AnswerTextMerger an aggregate AE. */

public class AnswerTextMerger extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(AnswerTextMerger.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* We determine a "core text" for each answer and merge
		 * answers wit the same core text. */

		Map<String, List<Answer>> answersByCoreText = new HashMap<String, List<Answer>>();
		for (Answer a : JCasUtil.select(jcas, Answer.class)) {
			String coreText = a.getCanonText();
			// logger.debug("answer {} coreText {}", a.getText(), coreText);
			List<Answer> answers = answersByCoreText.get(coreText);
			if (answers == null) {
				answers = new LinkedList<Answer>();
				answersByCoreText.put(coreText, answers);
			}
			answers.add(a);
		}

		/* Now, we keep the top scored answer and sum up the rest in
		 * a specific AF. */
		for (Entry<String, List<Answer>> entry : answersByCoreText.entrySet()) {
			List<Answer> answers = entry.getValue();
			if (answers.size() == 1)
				continue;
			Answer bestAnswer = getBestAnswer(answers);

			/* Generate an AF of the other answers */
			double sumScore = sumAnswerScore(answers, bestAnswer);
			logger.debug("merged answer {} (sumScore {})", bestAnswer.getText(), sumScore);
			setMergedScore(jcas, bestAnswer, sumScore);

			/* Remove the other answers */
			for (Answer oa : answers) {
				if (oa == bestAnswer)
					continue;
				logger.debug("...subsumed {}:{} < {}", oa.getText(), oa.getConfidence(), bestAnswer.getText());
				removeAnswer(oa);
			}
		}
	}

	protected Answer getBestAnswer(List<Answer> answers) {
		Answer bestAnswer = null;
		for (Answer a : answers) {
			if (bestAnswer == null || a.getConfidence() > bestAnswer.getConfidence())
				bestAnswer = a;
		}
		return bestAnswer;
	}

	protected double sumAnswerScore(List<Answer> answers, Answer except) {
		double score = 0;
		for (Answer a : answers) {
			if (a == except)
				continue;
			score += a.getConfidence();
		}
		return score;
	}

	protected void setMergedScore(JCas jcas, Answer a, double sumScore) throws AnalysisEngineProcessException {
		AnswerFV fv = new AnswerFV(a);
		fv.setFeature(AF_MergedSyntaxScore.class, sumScore);

		for (FeatureStructure af : a.getFeatures().toArray())
			((AnswerFeature) af).removeFromIndexes();
		a.removeFromIndexes();

		a.setFeatures(fv.toFSArray(jcas));
		a.addToIndexes();
	}

	protected void removeAnswer(Answer a) {
		if (a.getFeatures() != null)
			for (FeatureStructure af : a.getFeatures().toArray())
				((AnswerFeature) af).removeFromIndexes();
		a.removeFromIndexes();
	}
}
