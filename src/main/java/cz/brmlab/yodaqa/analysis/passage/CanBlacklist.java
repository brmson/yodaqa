package cz.brmlab.yodaqa.analysis.passage;

import java.util.ArrayList;
import java.util.Collection;

import cz.brmlab.yodaqa.analysis.StopWordFilter;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;

/**
 * Remove CandidateAnswers with bogus text.  This covers answers like
 * "the", "a", ",", "'" and so on. */

public class CanBlacklist extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(CanBlacklist.class);
	protected final StopWordFilter blacklist = new StopWordFilter();

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		Collection<CandidateAnswer> toRemove = new ArrayList<CandidateAnswer>();
answers:	for (CandidateAnswer ca : JCasUtil.select(jcas, CandidateAnswer.class)) {
			/* Remove single-letter answers. */
			if (ca.getEnd() - ca.getBegin() == 1) {
				toRemove.add(ca);
				continue;
			}
			/* Remove blacklisted answers. */
			if (blacklist.contains(ca.getCoveredText())) {
				toRemove.add(ca);
			}
		}

		for (CandidateAnswer ca : toRemove) {
			logger.debug("removing blacklisted answer {}", ca.getCoveredText());
			if (ca.getFeatures() != null)
				for (FeatureStructure af : ca.getFeatures().toArray())
					((AnswerFeature) af).removeFromIndexes();
			ca.removeFromIndexes();
		}
	}
}
