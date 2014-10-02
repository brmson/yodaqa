package cz.brmlab.yodaqa.analysis.tycor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.IndexWordSet;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.PointerTarget;
import net.didion.jwnl.data.PointerType;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.data.Word;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.WordnetLAT;
import cz.brmlab.yodaqa.provider.JWordnet;

/**
 * Generate less specific LAT annotations from existing LAT annotations
 * based on Wordnet relationships.  At this point, we generate LATs
 * with gradually reduced specificity based on hypernymy. */

public class LATByWordnet extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATByWordnet.class);

	/* We don't generalize further over the noun.Tops words that
	 * represent the most abstract hierarchy and generally words
	 * that have nothing in common anymore.
	 *
	 * sed -ne 's/^{ //p' data/wordnet/dict/dbfiles//noun.Tops | sed -re 's/[.:^_a-zA-Z0-9]+,[^ ]+ //g; s/ \(.*$//; s/\[ | \]|,//g; s/ .*$//'
	 *
	 * XXX: It would be better to have these as synset IDs, but that
	 * would be more complicated to obtain.
	 */
	protected static String tops_list[] = {
		"entity", "physical_entity", "abstraction", "thing", "object",
		"whole", "congener", "living_thing", "organism", "benthos",
		"dwarf", "heterotroph", "parent", "life", "biont", "cell",
		"causal_agent", "person", "animal", "plant", "native",
		"natural_object", "substance", "substance1", "matter", "food",
		"nutrient1", "artifact", "article", "psychological_feature",
		"cognition", "motivation", "attribute", "state", "feeling",
		"location", "shape", "time", "space", "absolute_space",
		"phase_space", "event", "process", "act", "group", "relation",
		"possession", "social_relation", "communication", "measure",
		"phenomenon",
	};
	protected Set<String> tops;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		if (tops == null)
			tops = new HashSet<String>(Arrays.asList(tops_list));

		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* Gather all LATs. */
		List<LAT> lats = new LinkedList<LAT>();
		for (LAT lat : JCasUtil.select(jcas, LAT.class))
			lats.add(lat);

		/* Generate derived LATs. */
		Map<Synset, WordnetLAT> latmap = new HashMap<Synset, WordnetLAT>();
		/* TODO: Populate with existing LATs for deduplication. */
		for (LAT lat : lats) {
			try {
				genDerivedLATs(latmap, lat);
			} catch (Exception e) {
				throw new AnalysisEngineProcessException(e);
			}
		}

		/* Add the remaining LATs. */
		for (WordnetLAT lat : latmap.values())
			lat.addToIndexes();
	}

	protected void genDerivedLATs(Map<Synset, WordnetLAT> latmap, LAT lat) throws Exception {
		/* TODO: Use pos information from the parser?
		 * Currently, we just assume a noun and the rest is
		 * a fallback path that derives the noun.
		 * (Typically: How hot is the sun? -> hotness) */

		IndexWordSet ws = JWordnet.getDictionary().lookupAllIndexWords(lat.getText());
		if (ws == null || ws.size() == 0) {
			logger.info("?! word " + lat.getText() + " not in Wordnet");
			return;
		}

		IndexWord wnoun = ws.getIndexWord(POS.NOUN);
		if (wnoun != null) {
			/* Got a noun right away. */
			genDerivedSynsets(latmap, lat, wnoun);
			return;
		}

		boolean foundNoun = false;
		for (IndexWord w : ws.getIndexWordArray()) {
			/* Try to derive a noun. */
			for (Synset synset : w.getSenses()) {
				for (PointerTarget t : synset.getTargets(PointerType.NOMINALIZATION)) {
					Word noun = (Word) t;
					foundNoun = true;
					genDerivedSynsets(latmap, lat, noun.getSynset());
				}
			}
		}
		if (!foundNoun)
			logger.info("?! word " + lat.getText() + " in Wordnet as non-noun but derived from no noun");
	}

	protected void genDerivedSynsets(Map<Synset, WordnetLAT> latmap, LAT lat, IndexWord wnoun) throws Exception {
		for (Synset synset : wnoun.getSenses()) {
			genDerivedSynsets(latmap, lat, synset);
		}
	}

	protected void genDerivedSynsets(Map<Synset, WordnetLAT> latmap, LAT lat, Synset synset) throws Exception {
		for (PointerTarget t : synset.getTargets(PointerType.HYPERNYM)) {
			Synset synset2 = (Synset) t;
			double spec = lat.getSpecificity() - 1;

			WordnetLAT l2 = latmap.get(synset2);
			if (l2 != null) {
				/* Ok, already exists. Try to raise
				 * specificity if possible. */
				if (l2.getSpecificity() < spec) {
					l2.setSpecificity(spec);
					l2.setBase(lat.getBase());
					l2.setBaseLAT(lat);
				}
				continue;
			}
			String lemma = synset2.getWord(0).getLemma();

			/* New LAT. */
			l2 = new WordnetLAT(lat.getCAS().getJCas());
			l2.setBegin(lat.getBegin());
			l2.setEnd(lat.getEnd());
			l2.setBase(lat.getBase());
			l2.setBaseLAT(lat);
			l2.setText(lemma);
			l2.setSpecificity(spec);
			l2.setIsHierarchical(true);
			latmap.put(synset2, l2);

			/* ...and recurse, unless we got into the noun.Tops
			 * realm already. */
			if (!tops.contains(lemma))
				genDerivedSynsets(latmap, l2, synset2);
		}
	}
}
