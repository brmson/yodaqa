package cz.brmlab.yodaqa.analysis.rdf;

import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.provider.SynonymsPCCP;
import cz.brmlab.yodaqa.provider.SynonymsPCCP.Synonym;
import cz.brmlab.yodaqa.provider.rdf.PropertyValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Counts probability of property containing a correct answer to given question.
 * Uses SynonymsPCCP to estimate similarity of (cooked) property to LAT.
 *
 * XXX: Method duplication with PropertyGloVeScoring.
 */
public class SynonymPCCPPropertyScorer {
	final Logger logger = LoggerFactory.getLogger(SynonymPCCPPropertyScorer.class);

	List<String> baseTexts;
	Map<String, Double> synonyms;

	public SynonymPCCPPropertyScorer(JCas questionView) {
		baseTexts = new ArrayList<>();
		synonyms = new HashMap<>();
		for (LAT lat : JCasUtil.select(questionView, LAT.class)) {
			if (lat.getSpecificity() <= -1)
				continue;
			logger.debug("LAT [{}]?", lat.getText());
			loadSynonyms(lat.getText());
		}
	}

	protected void loadSynonyms(String text) {
		baseTexts.add(text);

		List<Synonym> latSyns = SynonymsPCCP.getSynonyms(text);
		for (Synonym s : latSyns) {
			String w = s.getWord().toLowerCase();
			logger.debug("[{}] new syn <<{}>> {}", text, w, s.getScore());
			if (synonyms.containsKey(w))
				synonyms.put(w, synonyms.get(w) + s.getScore());
			else
				synonyms.put(w, s.getScore());
		}
	}

	public Double getPropertyScore(PropertyValue pv) {
		String prop = pv.getProperty().toLowerCase();
		prop = prop.replaceAll("[0-9]*$", "");
		if (baseTexts.contains(prop)) {
			logger.debug("prop {} EXACT", prop);
			return 2.0; // synonym scores go up to ~1.87
		} else if (synonyms.containsKey(prop)) {
			logger.debug("prop {}: {}", prop, synonyms.get(prop));
			return synonyms.get(prop);
		} else {
			logger.debug("unk prop <<{}>>", prop);
			return null;
		}
	}
}
