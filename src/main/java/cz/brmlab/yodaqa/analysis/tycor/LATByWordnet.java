package cz.brmlab.yodaqa.analysis.tycor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.PointerTarget;
import net.didion.jwnl.data.PointerType;
import net.didion.jwnl.data.Synset;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.provider.JWordnet;

/**
 * Generate less specific LAT annotations from existing LAT annotations
 * based on Wordnet relationships.  At this point, we generate LATs
 * with gradually reduced specificity based on hypernymy. */

public class LATByWordnet extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATByWordnet.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* Gather all LATs. */
		List<LAT> lats = new LinkedList<LAT>();
		for (LAT lat : JCasUtil.select(jcas, LAT.class))
			lats.add(lat);

		/* Generate derived LATs. */
		Map<Synset, LAT> latmap = new HashMap<Synset, LAT>();
		/* TODO: Populate with existing LATs for deduplication. */
		for (LAT lat : lats) {
			try {
				genDerivedLATs(latmap, lat);
			} catch (Exception e) {
				throw new AnalysisEngineProcessException(e);
			}
		}

		/* Add the remaining LATs. */
		for (LAT lat : latmap.values())
			lat.addToIndexes();
	}

	protected void genDerivedLATs(Map<Synset, LAT> latmap, LAT lat) throws Exception {
		IndexWord w = JWordnet.getDictionary().lookupIndexWord(POS.NOUN /* XXX */, lat.getText());
		if (w == null) {
			logger.info("?! word " + lat.getText() + " not in Wordnet");
			return;
		}

		for (Synset synset : w.getSenses()) {
			genDerivedSynsets(latmap, lat, synset);
		}
	}

	protected void genDerivedSynsets(Map<Synset, LAT> latmap, LAT lat, Synset synset) throws Exception {
		for (PointerTarget t : synset.getTargets(PointerType.HYPERNYM)) {
			Synset synset2 = (Synset) t;
			double spec = lat.getSpecificity() - 1;

			LAT l2 = latmap.get(synset2);
			if (l2 != null) {
				/* Ok, already exists. Try to raise
				 * specificity if possible. */
				if (l2.getSpecificity() < spec)
					l2.setSpecificity(spec);
				continue;
			}

			/* New LAT. */
			l2 = new LAT(lat.getCAS().getJCas());
			l2.setBegin(lat.getBegin());
			l2.setEnd(lat.getEnd());
			l2.setBase(lat.getBase());
			l2.setBaseLAT(lat);
			l2.setText(synset2.getWord(0).getLemma());
			l2.setSpecificity(spec);
			latmap.put(synset2, l2);

			/* ...and recurse. */
			genDerivedSynsets(latmap, l2, synset2);
		}
	}
}
