package cz.brmlab.yodaqa.analysis.tycor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.dictionary.Dictionary;

import org.apache.commons.lang.StringUtils;
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
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;

/**
 * From plural word LATs, generate also their single word variants.
 * Type coercion needs an exact match, so "cities" is much less useful
 * than "city".
 *
 * Basically, what we do here is create a micro custom UIMA pipeline
 * to analyze LATs and generate their lemmas as LATs too.  We also
 * attempt to deal with multi-word LATs by a simple heuristic to lemmatize
 * the head noun.
 *
 * We ignore all LATs that are already covered by Wordnet.
 *
 * XXX: Should we reduce specificity? Introducing another hiearchy
 * besides Wordnet hypernymy will need some work. */

public class LATByPlural extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATByPlural.class);

	Dictionary dictionary = null;

	/** An instantiated UIMA pipeline that processes an LAT.
	 * FIXME: This should be better encapsulated rather than being
	 * spread all around this class. */
	AnalysisEngine pipeline;

	/* A global cache that stores the LAT transformations, as it
	 * turns out that even lemmatization is quite slow. */
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

		AnalysisEngineDescription pipelineDesc = AnalysisEngineFactory.createEngineDescription(
				AnalysisEngineFactory.createEngineDescription(StanfordPosTagger.class),
				AnalysisEngineFactory.createEngineDescription(LanguageToolLemmatizer.class)
			);
		pipeline = AnalysisEngineFactory.createEngine(pipelineDesc);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* Gather all LATs.  Deduplicate for good measure. */
		Set<LAT> lats = new HashSet<LAT>();
		for (LAT lat : JCasUtil.select(jcas, LAT.class)) {
			try {
				if (lat.getSynset() != 0
				    || dictionary.getIndexWord(net.sf.extjwnl.data.POS.NOUN, lat.getText()) != null) {
					// too noisy
					// logger.debug("ignoring Wordnet LAT <<{}>>", lat.getText());
					continue;
				}
			} catch (JWNLException e) {
				throw new AnalysisEngineProcessException(e);
			}
			lats.add(lat);
		}

		/* Process the LATs. */
		for (LAT lat : lats) {
			String sing = latCache.get(lat.getText());
			if (sing == null) {
				try {
					sing = singLAT(lat.getText(), jcas.getDocumentLanguage());
				} catch (Exception e) {
					throw new AnalysisEngineProcessException(e);
				}
			}

			if (sing == null)
				continue;
			latCache.put(lat.getText(), sing);

			if (!sing.toLowerCase().equals(lat.getText().toLowerCase()))
				copyLAT(lat, sing);
		}
	}

	protected String singLAT(String text, String language) throws Exception {
		/* Prepare a tiny JCas with the multi-word LAT. */
		JCas jcas = JCasFactory.createJCas();
		jcas.setDocumentText(text);
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
		//	logger.debug("{} | {}", v.getCoveredText(), v.getLemma().getValue());

		/* Deal with multi-word LATs. */
		Token head = null;
		if (head == null) {
			/* Grab the last noun in the first sequence of nouns. */
			/* That sounds weird, but it makes sense.  Our idea
			 * is to take the first noun, but often other nouns
			 * are prepended as adjectives (but not appended):
			 * NN     NN     IN ...
			 * member states of the commonwealth of nations
			 *        ^^^^^^ <- LAT
			 */
			/* N.B. this heuristic is also used in LATByMultiWord. */
			for (Token v : JCasUtil.select(jcas, Token.class)) {
				if (v.getPos().getPosValue().matches("^N.*")) {
					head = v;
				} else if (head != null) {
					break;
				}
			}
		}
		if (head == null) {
			/* Oops. Grab the first (possibly only!) word then. */
			try {
				head = JCasUtil.select(jcas, Token.class).iterator().next();
			} catch (NoSuchElementException e) {
				head = null; // not even that?!
			}
		}

		/* Now, rebuild the LAT with head replaced with its lemma.
		 * This also normalizes whitespaces as a side effect. */
		List<String> words = new ArrayList<>();
		for (Token t : JCasUtil.select(jcas, Token.class)) {
			if (t == head) {
				words.add(t.getLemma().getValue());
			} else {
				words.add(t.getCoveredText());
			}
		}

		jcas.release();

		String newText = StringUtils.join(words, " ");
		return newText;
	}

	/** Generate a copy of a given LAT, just with different text. */
	protected void copyLAT(LAT lat, String text) {
		LAT newLAT = (LAT) lat.clone();
		newLAT.setText(text);
		newLAT.addToIndexes();
		logger.debug(".. LAT <<{}>> to singular <<{}>>", lat.getText(), text);
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
