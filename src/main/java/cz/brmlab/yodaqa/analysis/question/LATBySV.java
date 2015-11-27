package cz.brmlab.yodaqa.analysis.question;

import java.util.HashSet;
import java.util.Set;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.PointerTarget;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.data.Word;
import net.sf.extjwnl.dictionary.Dictionary;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.SV;
import cz.brmlab.yodaqa.model.TyCor.QuestionWordLAT;
import cz.brmlab.yodaqa.model.TyCor.SVLAT;
import cz.brmlab.yodaqa.provider.Wordnet;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NSUBJ;

/**
 * Generate LAT annotations in a QuestionCAS based on SV.  This is used
 * when the Focus is the SV's noun subject (NSUBJ) - the motivation is
 * e.g. getting "director" for "Who directed Fight Club".  OTOH this is
 * used only when we have just a QuestionWordLAT.  N.B. "Who was elected
 * as president of US?" doesn't fall here as the "who"-"elected" is
 * a NSUBJPASS. */

public class LATBySV extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATBySV.class);
	
	// wordnet instance
	Dictionary dictionary = null;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		dictionary = Wordnet.getDictionary();
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		if (JCasUtil.select(jcas, QuestionWordLAT.class).isEmpty())
			return;

		/* Question word LAT is included, try to produce a LAT
		 * based on the SV too. */
		if (JCasUtil.select(jcas, NSUBJ.class).isEmpty())
			return;
		// we just grab the sv instead of NSUBJ dependent as the SV
		// strategy may be more complex in case of tricky constructions

		for (SV sv : JCasUtil.select(jcas, SV.class)) {
			try {
				deriveSVLAT(jcas, sv);
			} catch (JWNLException e) {
				throw new AnalysisEngineProcessException(e);
			}
		}
	}

	public void deriveSVLAT(JCas jcas, SV sv) throws JWNLException {
		IndexWord w = dictionary.getIndexWord(net.sf.extjwnl.data.POS.VERB, sv.getBase().getLemma().getValue());
		if (w == null)
			return;

		/* Try to derive a noun. */
		Set<Long> producedSynsets = new HashSet<>();
		for (Synset synset : w.getSenses()) {
			for (PointerTarget t : synset.getTargets(net.sf.extjwnl.data.PointerType.DERIVATION)) {
				Word nounw = (Word) t;
				if (nounw.getPOS() != net.sf.extjwnl.data.POS.NOUN)
					continue;
				long ss = nounw.getSynset().getOffset();
				if (producedSynsets.contains(ss))
					continue;
				else
					producedSynsets.add(ss);
				addSVLAT(jcas, sv, nounw.getLemma(), ss);
			}
		}
	}

	protected SVLAT addSVLAT(JCas jcas, SV sv, String text, long synset) {
		SVLAT lat = new SVLAT(jcas);
		lat.setBegin(sv.getBegin());
		lat.setEnd(sv.getEnd());
		lat.setBase(sv);
		lat.setText(text);
		lat.setSpecificity(0);
		lat.setSynset(synset);
		lat.addToIndexes();
		logger.debug("new LAT by {}: <<{}>>/{}", sv.getCoveredText(), lat.getText(), lat.getSynset());
		return lat;
	}
}
