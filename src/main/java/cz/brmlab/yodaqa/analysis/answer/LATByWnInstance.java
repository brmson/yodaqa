package cz.brmlab.yodaqa.analysis.answer;

import java.util.List;

import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.PointerTarget;
import net.didion.jwnl.data.PointerType;
import net.didion.jwnl.data.Synset;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATWnInstance;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.WnInstanceLAT;
import cz.brmlab.yodaqa.provider.JWordnet;
import cz.brmlab.yodaqa.provider.rdf.DBpediaTypes;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.NN;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Generate LAT annotations in a CandidateAnswerCAS. This is based on the
 * answer focus (or NamedEntity) which is looked up in Wordnet and LATs
 * based on instance-of relations are generated. */

public class LATByWnInstance extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATByWnInstance.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* A Focus is a good LAT query source. */
		try {
			for (Focus focus : JCasUtil.select(jcas, Focus.class)) {
				/* ...however, prefer an overlapping named entity. */
				boolean ne_found = false;
				for (NamedEntity ne : JCasUtil.selectCovering(NamedEntity.class, focus)) {
					if (processOne(jcas, focus, ne.getCoveredText()))
						ne_found = true;
				}
				if (!ne_found) {
					processOne(jcas, focus, focus.getToken().getLemma().getValue());
				}
			}
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

	public boolean processOne(JCas jcas, Annotation base, String text) throws Exception {
		IndexWord w = JWordnet.getDictionary().lookupIndexWord(net.didion.jwnl.data.POS.NOUN, text);
		if (w == null)
			return false;
		for (Synset synset : w.getSenses()) {
			for (PointerTarget t : synset.getTargets(PointerType.INSTANCE_HYPERNYM)) {
				addWordnetLAT(jcas, base, (Synset) t);
			}
		}
		return true;
	}

	protected void addWordnetLAT(JCas jcas, Annotation base, Synset synset) throws Exception {
		addLATFeature(jcas, AF_LATWnInstance.class);

		String lemma = synset.getWord(0).getLemma().replace('_', ' ');

		/* We have a synthetic noun(-ish), synthetize
		 * a POS tag for it. */
		/* XXX: We assume a hypernym is always a noun. */
		POS pos = new NN(jcas);
		pos.setBegin(base.getBegin());
		pos.setEnd(base.getEnd());
		pos.setPosValue("NNS");
		pos.addToIndexes();

		addLAT(new WnInstanceLAT(jcas), base.getBegin(), base.getEnd(), base, lemma, pos, synset.getOffset(), 0.0);
		logger.debug(".. LAT {}/{} is Wordnet of-instance of {}", lemma, synset.getOffset(), base.getCoveredText());
	}

	protected void addLAT(LAT lat, int begin, int end, Annotation base, String text, POS pos, long synset, double spec) {
		lat.setBegin(begin);
		lat.setEnd(end);
		lat.setBase(base);
		lat.setPos(pos);
		lat.setText(text);
		lat.setSpecificity(spec);
		lat.setSynset(synset);
		lat.addToIndexes();
	}

	protected void addLATFeature(JCas jcas, Class<? extends AnswerFeature> f) throws AnalysisEngineProcessException {
		AnswerInfo ai = JCasUtil.selectSingle(jcas, AnswerInfo.class);
		AnswerFV fv = new AnswerFV(ai);
		fv.setFeature(f, 1.0);

		for (FeatureStructure af : ai.getFeatures().toArray())
			((AnswerFeature) af).removeFromIndexes();
		ai.removeFromIndexes();

		ai.setFeatures(fv.toFSArray(jcas));
		ai.addToIndexes();
	}
}
