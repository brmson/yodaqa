package cz.brmlab.yodaqa.analysis.answer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.Question.ClueConcept;
import cz.brmlab.yodaqa.model.Question.ClueNE;

/**
 * Mark answers textually similar to clues.  We generate various features
 * that mark the overlap (or equivalence) and also distinguish between
 * concept+NE and other clues.
 *
 * FIXME: The syntactic diffusion code is largely duplicated in the
 * EvidenceDiffusion code! */

public class AnswerClueOverlap extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(AnswerClueOverlap.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	protected class Patterns {
		/* The required bounary. */
		static final String boundary = "\\s*\\W\\s*";

		Pattern prefix, suffix, substr;

		public Patterns(String text) {
			String qText = Pattern.quote(text);
			prefix = Pattern.compile("^" + qText + boundary + ".*");
			suffix = Pattern.compile(".*" + boundary + qText + "$");
			substr = Pattern.compile(".*" + boundary + qText + boundary + ".*");
			// logger.debug("<<{}>> substr {} <<{}>>", text, substr, ".*" + boundary + qText + boundary + ".*");
		}
	}

	interface ClueFilter {
		boolean test(Clue c);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, answerView;
		try {
			questionView = jcas.getView("Question");
			answerView = jcas.getView("Answer");
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
		AnswerInfo ai = JCasUtil.selectSingle(answerView, AnswerInfo.class);

		matchClues(ai, questionView, answerView,
				new ClueFilter() {
					public boolean test(Clue c) {
						return !(c instanceof ClueNE || c instanceof ClueConcept);
					}
				},
				AF.ClOMatchScore,
				AF.ClOPrefixedScore, AF.ClOPrefixingScore,
				AF.ClOSuffixedScore, AF.ClOSuffixingScore,
				AF.ClOSubstredScore, AF.ClOSubstringScore,
				AF.ClOMetaMatchScore);

		matchClues(ai, questionView, answerView,
				new ClueFilter() {
					public boolean test(Clue c) {
						return (c instanceof ClueNE || c instanceof ClueConcept);
					}
				},
				AF.ClOCMatchScore,
				AF.ClOCPrefixedScore, AF.ClOCPrefixingScore,
				AF.ClOCSuffixedScore, AF.ClOCSuffixingScore,
				AF.ClOCSubstredScore, AF.ClOCSubstringScore,
				AF.ClOCMetaMatchScore);
	}

	/* XXX: This is so ugly calling convention...
	 * TODO: Repair now that we can generate custom feature names. */
	protected void matchClues(AnswerInfo ai,
			JCas questionView, JCas answerView,
			ClueFilter filter,
			String matchAF,
			String prefixedAF, String prefixingAF,
			String suffixedAF, String suffixingAF,
			String substredAF, String substringAF,
			String metaMatchAF)
				throws AnalysisEngineProcessException {

		String text = ai.getCanonText();
		if (text.length() == 0)
			return; // Don't generate stuff for empty answers
		Patterns ps = new Patterns(text);

		/* Cross-match answer with clues */
		/* XXX: This is way too overconvoluted, omg.  It's just
		 * done the same way as in EvidenceDiffusion. */

		List<Double> matchScores = new ArrayList<Double>();
		List<Double> prefixedScores = new ArrayList<Double>();
		List<Double> prefixingScores = new ArrayList<Double>();
		List<Double> suffixedScores = new ArrayList<Double>();
		List<Double> suffixingScores = new ArrayList<Double>();
		List<Double> substredScores = new ArrayList<Double>();
		List<Double> substringScores = new ArrayList<Double>();

		for (Clue c : JCasUtil.select(questionView, Clue.class)) {
			if (!filter.test(c))
				continue;

			// XXX: Full syntax canonization?
			String text2 = c.getLabel().toLowerCase();
			Patterns ps2 = new Patterns(text2);

			/* XXX: This needs to be somehow cleaned up. */

			if (text.equals(text2)) {
				logger.debug("equals(<<{}>>) += <<{}>>", text, text2);
				matchScores.add(1.0);
			} else if (ps2.prefix.matcher(text).matches()) {
				logger.debug("prefixed(<<{}>>) += <<{}>>", text, text2);
				prefixedScores.add(1.0);
			} else if (ps.prefix.matcher(text2).matches()) {
				logger.debug("prefixing(<<{}>>) += <<{}>>", text, text2);
				prefixingScores.add(1.0);
			} else if (ps2.suffix.matcher(text).matches()) {
				logger.debug("suffixed(<<{}>>) += <<{}>>", text, text2);
				suffixedScores.add(1.0);
			} else if (ps.suffix.matcher(text2).matches()) {
				logger.debug("suffixing(<<{}>>) += <<{}>>", text, text2);
				suffixingScores.add(1.0);
			} else if (ps2.substr.matcher(text).matches()) {
				logger.debug("substred(<<{}>>) += <<{}>>", text, text2);
				substredScores.add(1.0);
			} else if (ps.substr.matcher(text2).matches()) {
				logger.debug("substring(<<{}>>) += <<{}>>", text, text2);
				substringScores.add(1.0);
			} else {
				continue;
			}
		}

		if (!(matchScores.size() > 0
			|| prefixedScores.size() > 0
			|| prefixingScores.size() > 0
			|| suffixedScores.size() > 0
			|| suffixingScores.size() > 0
			|| substredScores.size() > 0
			|| substringScores.size() > 0))
			return;

		AnswerFV fv = new AnswerFV(ai);
		if (matchScores.size() > 0) fv.setFeature(matchAF, mergeScores(matchScores));
		if (prefixedScores.size() > 0) fv.setFeature(prefixedAF, mergeScores(prefixedScores));
		if (prefixingScores.size() > 0) fv.setFeature(prefixingAF, mergeScores(prefixingScores));
		if (suffixedScores.size() > 0) fv.setFeature(suffixedAF, mergeScores(suffixedScores));
		if (suffixingScores.size() > 0) fv.setFeature(suffixingAF, mergeScores(suffixingScores));
		if (substredScores.size() > 0) fv.setFeature(substredAF, mergeScores(substredScores));
		if (substringScores.size() > 0) fv.setFeature(substringAF, mergeScores(substringScores));
		/* If we are both prefixed and suffixed by a clue,
		 * we have a "meta-match", which can be quite nice
		 * answer, actually.  E.g.
		 *   <<what's OCD?>>
		 * has an answer
		 *   <<Obsessive–compulsive disorder (OCD)>>
		 * which is suffixed by OCD and prefixed by concept
		 * clue Obsessive–compulsive disorder.  And it's the
		 * correct answer. */
		if (prefixedScores.size() > 0 && suffixedScores.size() > 0)
			fv.setFeature(metaMatchAF, (mergeScores(prefixedScores) + mergeScores(suffixedScores)) / 2.0);

		if (ai.getFeatures() != null)
			for (FeatureStructure af : ai.getFeatures().toArray())
				((AnswerFeature) af).removeFromIndexes();
		ai.removeFromIndexes();
		ai.setFeatures(fv.toFSArray(answerView));
		ai.addToIndexes();
	}

	/** Merge multiple diffused scores to a single value. */
	protected double mergeScores(List<Double> scores) {
		double mScore = 0;
		for (double score : scores)
			mScore += score;
		return mScore;
	}
}
