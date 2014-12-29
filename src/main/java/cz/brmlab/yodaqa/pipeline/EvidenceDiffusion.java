package cz.brmlab.yodaqa.pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.answer.AnswerFV;
import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_EvDPrefixedScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_EvDPrefixingScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_EvDSubstredScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_EvDSubstringScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_EvDSuffixedScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_EvDSuffixingScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;

/**
 * Share score between textually similar answers in AnswerHitlistCAS.
 * Basically, this is like AnswerTextMerger but just for loosely
 * similar answers and all of them are kept.
 *
 * Right now, we just determine syntactic equivalence, looking for
 * full prefixes and full suffixes (modulo the- etc. junk).
 *
 * TODO: Produce more diffusion engines, especially based on semantic
 * relations (like capital vs. country).  Then, we would rename this to
 * EvidenceSyntacticDiffusion and make EvidenceDiffusion an aggregate AE. */

public class EvidenceDiffusion extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(EvidenceDiffusion.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	protected class Patterns {
		/* The required bounary. */
		/* TODO: Try to relax boundary to just \\W */
		static final String boundary = "\\s*[.,:;()–-]\\s*";

		Pattern prefix, suffix, substr;

		public Patterns(String text) {
			String qText = Pattern.quote(text);
			prefix = Pattern.compile("^" + qText + boundary + ".*");
			suffix = Pattern.compile(".*" + boundary + qText + "$");
			substr = Pattern.compile(".*" + boundary + qText + boundary + ".*");
			// logger.debug("<<{}>> substr {} <<{}>>", text, substr, ".*" + boundary + qText + boundary + ".*");
		}
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* Pre-compute matching regexes */
		Map<Answer, Patterns> patterns = new HashMap<Answer, Patterns>();
		List<Answer> answers = new ArrayList<Answer>();

		FSIndex idx = jcas.getJFSIndexRepository().getIndex("SortedAnswers");
		FSIterator sortedAnswers = idx.iterator();
		while (sortedAnswers.hasNext()) {
			Answer a = (Answer) sortedAnswers.next();
			answers.add(a);
			Patterns ps = new Patterns(a.getCanonText());
			patterns.put(a, ps);
		}

		/* Cross-match */
		/* XXX: This is O(N^2)... */

		for (Answer a : answers) {
			String text = a.getCanonText();
			Patterns ps = patterns.get(a);

			double prefixedScore = 0;
			double prefixingScore = 0;
			double suffixedScore = 0;
			double suffixingScore = 0;
			double substredScore = 0;
			double substringScore = 0;

			for (Answer a2 : answers) {
				if (a == a2)
					continue;
				String text2 = a2.getCanonText();
				Patterns ps2 = patterns.get(a2);
				if (ps2.prefix.matcher(text).matches()) {
					logger.debug("prefixed(<<{}>>) += <<{}>>:{}", a.getText(), a2.getText(), a2.getConfidence());
					prefixedScore += a2.getConfidence();
				} else if (ps.prefix.matcher(text2).matches()) {
					logger.debug("prefixing(<<{}>>) += <<{}>>:{}", a.getText(), a2.getText(), a2.getConfidence());
					prefixingScore += a2.getConfidence();
				} else if (ps2.suffix.matcher(text).matches()) {
					logger.debug("suffixed(<<{}>>) += <<{}>>:{}", a.getText(), a2.getText(), a2.getConfidence());
					suffixedScore += a2.getConfidence();
				} else if (ps.suffix.matcher(text2).matches()) {
					logger.debug("suffixing(<<{}>>) += <<{}>>:{}", a.getText(), a2.getText(), a2.getConfidence());
					suffixingScore += a2.getConfidence();
				} else if (ps2.substr.matcher(text).matches()) {
					logger.debug("substred(<<{}>>) += <<{}>>:{}", a.getText(), a2.getText(), a2.getConfidence());
					substredScore += a2.getConfidence();
				} else if (ps.substr.matcher(text2).matches()) {
					logger.debug("substring(<<{}>>) += <<{}>>:{}", a.getText(), a2.getText(), a2.getConfidence());
					substringScore += a2.getConfidence();
				} else {
					continue;
				}
			}

			if (prefixedScore + prefixingScore + suffixedScore + suffixingScore + substredScore + substringScore == 0)
				continue;

			AnswerFV fv = new AnswerFV(a);
			if (prefixedScore > 0) fv.setFeature(AF_EvDPrefixedScore.class, prefixedScore);
			if (prefixingScore > 0) fv.setFeature(AF_EvDPrefixingScore.class, prefixingScore);
			if (suffixedScore > 0) fv.setFeature(AF_EvDSuffixedScore.class, suffixedScore);
			if (suffixingScore > 0) fv.setFeature(AF_EvDSuffixingScore.class, suffixingScore);
			if (substredScore > 0) fv.setFeature(AF_EvDSubstredScore.class, substredScore);
			if (substringScore > 0) fv.setFeature(AF_EvDSubstringScore.class, substringScore);
			for (FeatureStructure af : a.getFeatures().toArray())
				((AnswerFeature) af).removeFromIndexes();
			a.removeFromIndexes();
			a.setFeatures(fv.toFSArray(jcas));
			a.addToIndexes();
		}
	}
}
