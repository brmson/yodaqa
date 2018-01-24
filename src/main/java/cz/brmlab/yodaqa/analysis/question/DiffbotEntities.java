package cz.brmlab.yodaqa.analysis.question;


import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.provider.diffbot.DiffbotEntityLinker;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class DiffbotEntities extends JCasAnnotator_ImplBase {
	private static final Logger logger = LoggerFactory.getLogger(DiffbotEntities.class);
	private final DiffbotEntityLinker linker = new DiffbotEntityLinker();

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		List<DiffbotEntityLinker.Article> entityCandidates = null;
		logger.debug("Question text: {}", jCas.getDocumentText());
		try {
			entityCandidates = linker.entityLookup(jCas.getDocumentText());
			logger.debug("Size {} ", entityCandidates.size());
			for(DiffbotEntityLinker.Article candidate: entityCandidates) {
				for (int i = 0; i < candidate.count; i++) {
					addConcept(jCas, candidate, i);
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void addConcept(JCas jCas, DiffbotEntityLinker.Article candidate, int idx) {
		Concept concept = new Concept(jCas);
		concept.setBegin(candidate.offsets.title.get(idx).get(0));
		concept.setEnd(candidate.offsets.title.get(idx).get(0));
		concept.setFullLabel(candidate.label);
		concept.setCookedLabel(cookLabel(candidate.label, candidate.label));
		concept.setScore(candidate.score);
		concept.setOrigin(candidate.origin);
		if (candidate.kg != null) {
			concept.setId(candidate.kg.id);
			concept.setDescription(candidate.kg.decription);
		} else {
			logger.warn("KG field is empty. Cannot set knowledge base id!");
		}
		logger.debug("creating concept <<{}>> cooked <<{}>> --> {}, id={}, origin={}",
				concept.getFullLabel(), concept.getCookedLabel(),
				String.format(Locale.ENGLISH, "%.3f", concept.getScore()),
				concept.getId(), concept.getOrigin());
		concept.addToIndexes();
	}

	/** Produce a pretty label from sometimes-unwieldy enwiki article
	 * name. */
	protected String cookLabel(String clueLabel, String canonLabel) {
		String cookedLabel = canonLabel;
		if (cookedLabel.toLowerCase().matches("^list of .*")) {
			logger.debug("ignoring label <<{}>> for <<{}>>", cookedLabel, clueLabel);
			cookedLabel = clueLabel;
		}

		/* Remove trailing (...) (e.g. (disambiguation)). */
		/* TODO: We should model topicality of the
		 * concept; when asking about the director of
		 * "Frozen", the (film)-suffixed concepts should
		 * be preferred over e.g. the (House) suffix. */
		cookedLabel = cookedLabel.replaceAll("\\s+\\([^)]*\\)\\s*$", "");

		return cookedLabel;
	}
}
