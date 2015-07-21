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
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;

/**
 * Share score between textually similar answers in AnswerHitlistCAS.
 * Basically, this is like AnswerTextMerger but just for loosely
 * similar answers and all of them are kept.
 *
 * Right now, we just determine syntactic equivalence, looking for
 * full prefixes and full suffixes (modulo the- etc. junk).
 *
 * XXX: We do not currently produce the full set of features defined
 * for this purpose, since some of them led to massive overfitting.
 * See below.
 *
 * TODO: Produce more diffusion engines, especially based on semantic
 * relations (like capital vs. country).  Then, we would rename this to
 * EvidenceSyntacticDiffusion and make EvidenceDiffusion an aggregate AE.
 *
 * FIXME: The syntactic diffusion code is largely duplicated in the
 * AnswerClueOverlap code! */

public class EvidenceDiffusion extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(EvidenceDiffusion.class);

	/** Score merging policy in case score is diffused from multiple
	 * answers. We can just sum the score, produce a decayed sum or
	 * take a maximum. */
	public static final String PARAM_SCORE_MERGE_POLICY = "score-merge-policy";
	public static final String SMP_SUM = "sum";
	public static final String SMP_DECAY_SUM = "decay-sum";
	public static final String SMP_MAXIMUM = "maximum";
	@ConfigurationParameter(name = PARAM_SCORE_MERGE_POLICY, mandatory = false, defaultValue = SMP_DECAY_SUM)
	protected String scoreMergePolicy;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	protected class Patterns {
		/* The required bounary. */
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

			/* XXX: This needs to be somehow cleaned up. */

			List<Double> prefixedScores = new ArrayList<Double>();
			List<Double> prefixingScores = new ArrayList<Double>();
			List<Double> suffixedScores = new ArrayList<Double>();
			List<Double> suffixingScores = new ArrayList<Double>();
			List<Double> substredScores = new ArrayList<Double>();
			List<Double> substringScores = new ArrayList<Double>();

			for (Answer a2 : answers) {
				if (a == a2)
					continue;
				String text2 = a2.getCanonText();
				Patterns ps2 = patterns.get(a2);
				if (ps2.prefix.matcher(text).matches()) {
					logger.debug("prefixed(<<{}>>) += <<{}>>:{}", a.getText(), a2.getText(), a2.getConfidence());
					prefixedScores.add(a2.getConfidence());
				} else if (ps.prefix.matcher(text2).matches()) {
					logger.debug("prefixing(<<{}>>) += <<{}>>:{}", a.getText(), a2.getText(), a2.getConfidence());
					prefixingScores.add(a2.getConfidence());
				} else if (ps2.suffix.matcher(text).matches()) {
					logger.debug("suffixed(<<{}>>) += <<{}>>:{}", a.getText(), a2.getText(), a2.getConfidence());
					suffixedScores.add(a2.getConfidence());
				} else if (ps.suffix.matcher(text2).matches()) {
					logger.debug("suffixing(<<{}>>) += <<{}>>:{}", a.getText(), a2.getText(), a2.getConfidence());
					suffixingScores.add(a2.getConfidence());
				} else if (ps2.substr.matcher(text).matches()) {
					logger.debug("substred(<<{}>>) += <<{}>>:{}", a.getText(), a2.getText(), a2.getConfidence());
					substredScores.add(a2.getConfidence());
				} else if (ps.substr.matcher(text2).matches()) {
					logger.debug("substring(<<{}>>) += <<{}>>:{}", a.getText(), a2.getText(), a2.getConfidence());
					substringScores.add(a2.getConfidence());
				} else {
					continue;
				}
			}

			/* XXX: We were detecting all kinds of overlaps, but we
			 * actually diffuse only across some. Specifically,
			 * we do not diffuse across the "prefixing" feature
			 * and "suffixed" feature, i.e. along answer tuples like:
			 * prefixing: "21 June" <- "21 June 2000" NOT (but -> YES)
			 * suffixed:  "21 June 2000" <-    "2000" NOT (but -> YES)
			 * These were the features that got assigned positive
			 * weights - all the others end up serving as negative
			 * feedback. */
			prefixingScores.clear();
			suffixedScores.clear();

			if (!(prefixedScores.size() > 0
				|| prefixingScores.size() > 0
				|| suffixedScores.size() > 0
				|| suffixingScores.size() > 0
				|| substredScores.size() > 0
				|| substringScores.size() > 0))
				continue;

			AnswerFV fv = new AnswerFV(a);
			if (prefixedScores.size() > 0) fv.setFeature(AF.EvDPrefixedScore, mergeScores(prefixedScores));
			if (prefixingScores.size() > 0) fv.setFeature(AF.EvDPrefixingScore, mergeScores(prefixingScores));
			if (suffixedScores.size() > 0) fv.setFeature(AF.EvDSuffixedScore, mergeScores(suffixedScores));
			if (suffixingScores.size() > 0) fv.setFeature(AF.EvDSuffixingScore, mergeScores(suffixingScores));
			if (substredScores.size() > 0) fv.setFeature(AF.EvDSubstredScore, mergeScores(substredScores));
			if (substringScores.size() > 0) fv.setFeature(AF.EvDSubstringScore, mergeScores(substringScores));
			for (FeatureStructure af : a.getFeatures().toArray())
				((AnswerFeature) af).removeFromIndexes();
			a.removeFromIndexes();
			a.setFeatures(fv.toFSArray(jcas));
			a.addToIndexes();
		}
	}

	/** Merge multiple diffused scores to a single value.  This is done
	 * according to the PARAM_SCORE_MERGE_POLICY. */
	protected double mergeScores(List<Double> scores) {
		double mScore = 0;

		if (scoreMergePolicy.equals(SMP_SUM)) {
			for (double score : scores)
				mScore += score;

		} else if (scoreMergePolicy.equals(SMP_DECAY_SUM)) {
			double i = 1;
			for (double score : scores) {
				mScore += score / i;
				i *= 2;
			}

		} else if (scoreMergePolicy.equals(SMP_MAXIMUM)) {
			for (double score : scores) {
				if (score > mScore)
					mScore = score;
			}

		} else assert(false);

		return mScore;
	}
}
