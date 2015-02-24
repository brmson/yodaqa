package cz.brmlab.yodaqa.analysis.tycor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.dictionary.Dictionary;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.TyCor.LAT;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;

/**
 * From multi-word LATs, generate also their single word variants.
 * Type coercion needs an exact match, but LAT "max. temperature"
 * or "length meters" will not match.  So we want to also produce
 * "temperature" or "length" from these.
 *
 * Basically, what we do here is create a micro custom UIMA pipeline
 * to analyze the multi-word LATs so that we know which word is most
 * important.  As a first approximation, we just POS-tag and pick
 * a suitable noun.
 *
 * We do not split multi-word LATs contained in Wordnet.
 *
 * XXX: Should we reduce specificity? Introducing another hiearchy
 * besides Wordnet hypernymy will need some work. */

public class LATByMultiWord extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATByMultiWord.class);
	
	Dictionary dictionary = null;

	/** An instantiated UIMA pipeline that processes an LAT.
	 * FIXME: This should be better encapsulated rather than being
	 * spread all around this class. */
	AnalysisEngine pipeline;

	/* A global cache that stores the LAT transformations, as it
	 * turns out that even just POS tagging is quite slow. */
	static Map<String, String> latCache;
	{
		latCache = new HashMap<>();
	}

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		try {
			if (dictionary == null)
				dictionary = Dictionary.getDefaultResourceInstance();
		} catch (JWNLException e) {
			throw new ResourceInitializationException(e);
		}

		AnalysisEngineDescription pipelineDesc =
			AnalysisEngineFactory.createEngineDescription(StanfordPosTagger.class);
		pipeline = AnalysisEngineFactory.createEngine(pipelineDesc);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* Gather all multi-word LATs.  Deduplicate for good measure. */
		Set<LAT> lats = new HashSet<LAT>();
		for (LAT lat : JCasUtil.select(jcas, LAT.class)) {
			if (!lat.getText().contains(" "))
				continue;
			try {
				if (dictionary.getIndexWord(net.sf.extjwnl.data.POS.NOUN, lat.getText()) != null) {
					logger.debug("ignoring Wordnet phrase <<{}>>", lat.getText());
					continue;
				}
			} catch (JWNLException e) {
				throw new AnalysisEngineProcessException(e);
			}
			lats.add(lat);
		}

		/* Process the LATs. */
		for (LAT lat : lats) {
			String word = latCache.get(lat.getText());
			if (word == null) {
				try {
					word = extractWord(lat.getText(), jcas.getDocumentLanguage());
				} catch (Exception e) {
					throw new AnalysisEngineProcessException(e);
				}
				latCache.put(lat.getText(), word);
			}
			copyLAT(lat, word);
		}
	}

	protected String extractWord(String multiWord, String language) throws Exception {
		/* Prepare a tiny JCas with the multi-word LAT. */
		JCas jcas = JCasFactory.createJCas();
		jcas.setDocumentText(multiWord);
		jcas.setDocumentLanguage(language);

		/* Pre-do segmentation. */
		Sentence s = new Sentence(jcas);
		s.setBegin(0);
		s.setEnd(jcas.getDocumentText().length());
		s.addToIndexes();
		/* Match word tokens, plus possibly trailing
		 * punctuation and stuff (like "max."). */
		Matcher m = Pattern.compile("\\b\\w+\\b\\S*").matcher(jcas.getDocumentText());
		while (m.find()) {
			Token t = new Token(jcas);
			t.setBegin(m.start());
			t.setEnd(m.end());
			t.addToIndexes();
		}

		/* Process the LAT sentence. */
		pipeline.process(jcas);
		//for (Token v : JCasUtil.select(jcas, Token.class))
		//	logger.debug("{}  {}", v.getPos().getPosValue(), v.getCoveredText());

		String word = null;
		if (word == null) {
			/* Grab the last noun in the first sequence of nouns. */
			/* That sounds weird, but it makes sense.  Our idea
			 * is to take the first noun, but often other nouns
			 * are prepended as adjectives (but not appended):
			 * NN     NN     IN ...
			 * member states of the commonwealth of nations
			 *        ^^^^^^ <- LAT
			 */
			for (Token v : JCasUtil.select(jcas, Token.class)) {
				if (v.getPos().getPosValue().matches("^N.*")) {
					word = v.getCoveredText();
				} else if (word != null) {
					break;
				}
			}
		}
		if (word == null) {
			/* Oops. Grab the first word then. */
			word = JCasUtil.select(jcas, Token.class).iterator().next().getCoveredText();
		}
		jcas.release();
		return word;
	}

	/** Generate a copy of a given LAT, just with different text. */
	protected void copyLAT(LAT lat, String text) {
		LAT newLAT = (LAT) lat.clone();
		newLAT.setText(text);
		newLAT.addToIndexes();
		logger.debug(".. LAT <<{}>> to single-word <<{}>>", lat.getText(), text);
	}

	public void destroy() {
		try {
			pipeline.collectionProcessComplete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		pipeline.destroy();
	}
}
