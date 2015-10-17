package cz.brmlab.yodaqa.analysis.tycor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import org.apache.uima.impl.AnalysisEngineFactory_impl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.provider.Wordnet;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;

/**
 * Normalize various low quality LAT forms.  This annotator goes through
 * all the LATs, runs them through a micro NLP analysis UIMA pipeline,
 * possibly modifies them and generates some derived LATs (TODO: removes
 * duplicate LATs).
 *
 * In practice, we:
 *
 * (i) Segment, pos tag and lemmatize each LAT.
 *
 * (ii) In case of multi-word LATs, we find the most important "head
 * noun", using some silly heuristics.
 *
 * (iii) From plural word LATs, replace them with singular variants.
 * Type coercion needs an exact match, so "cities" is much less useful
 * than "city".
 *
 * (iv) Spin off head nouns of multi-word LATs to separate LATs.
 * Type coercion needs an exact match, but LAT "max. temperature"
 * or "area total" will not match.  So we want to also produce
 * "temperature" or "area" from these.  ("length meters" -> "length"
 * is trickier with our current implementation of (ii) though.)
 *
 * We ignore all LATs that are already covered by Wordnet and keep
 * a cache of the LAT transformations as the analysis is quite
 * expensive. */
/* XXX: Should we reduce specificity? Introducing another hiearchy
 * besides Wordnet hypernymy will need some work. */
/* XXX: This should ideally be a more modular pipeline with isolated
 * annotators. */

public class LATNormalize extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATNormalize.class);

	static Dictionary dictionary = null;

	/** An instantiated UIMA pipeline that processes an LAT.
	 * FIXME: This should be better encapsulated rather than being
	 * spread all around this class. */
	AnalysisEngine pipeline;

	/* A global cache that stores the LAT transformations, as it
	 * turns out that even lemmatization is quite slow. */
	public class LATCacheEntry {
		String singularForm, singleWord;
	};
	static Map<String, LATCacheEntry> latCache;
	{
		latCache = new ConcurrentHashMap<>();
	}

	public synchronized void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		dictionary = Wordnet.getDictionary();

		AnalysisEngineDescription pipelineDesc = AnalysisEngineFactory.createEngineDescription(
				AnalysisEngineFactory.createEngineDescription(StanfordPosTagger.class),
				AnalysisEngineFactory.createEngineDescription(LanguageToolLemmatizer.class)
			);
		/* XXX: We cannot create sub-pipelines using generic mechanisms
		 * in the vein of
		 * 	pipeline = AnalysisEngineFactory.createEngine(pipelineDesc);
		 * since the AnalysisEngineFactory uses the globally registered
		 * AE factory implementation, which in our case is customized
		 * to create an instance of ParallelAnalysisEngine which would
		 * attempt to use the same main thread pool as the global
		 * pipeline does.  So in the end, if all threads of the main
		 * thread pool enter LATNormalize, we end up hanging forever
		 * waiting for them to free up to run our sub-pipeline.
		 *
		 * Therefore, we do this manually, explicitly using the stock
		 * UIMA factory that produces non-parallelized aggregates.
		 * Note that we cannot build pipelines with nested aggregates
		 * this way, though! */
		AnalysisEngineFactory_impl aeFactory = new AnalysisEngineFactory_impl();
		pipeline = (AnalysisEngine) aeFactory.produceResource(AnalysisEngine.class, pipelineDesc, null);
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
			LATCacheEntry lce = latCache.get(lat.getText());
			if (lce == null) {
				try {
					lce = processLAT(lat.getText(), jcas.getDocumentLanguage());
				} catch (Exception e) {
					throw new AnalysisEngineProcessException(e);
				}
				if (lce != null)
					latCache.put(lat.getText(), lce);
			}

			if (lce == null)
				continue;

			if (!lce.singularForm.toLowerCase().equals(lat.getText().toLowerCase()))
				updateLAT(lat, lce.singularForm);
			if (!lce.singleWord.toLowerCase().equals(lat.getText().toLowerCase())
			    && !lce.singleWord.toLowerCase().equals(lce.singularForm.toLowerCase()))
				copyLAT(lat, lce.singleWord);
		}
	}

	protected LATCacheEntry processLAT(String text, String language) throws Exception {
		/* Prepare a tiny JCas with the processed LAT. */
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
		//	logger.debug("{}   |   {} {}", v.getCoveredText(), v.getPos().getPosValue(), v.getLemma().getValue());

		/* Find multi-word LAT head token. */
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
				jcas.release();
				return null; // not even that?! nothing to do
			}
		}

		/* Singular head noun form. */
		String singHead = head.getLemma().getValue();
		if (singHead.equals("people"))
			singHead = "person"; // special case, super-common

		/* --- analysis over, now generate results --- */

		LATCacheEntry lce = new LATCacheEntry();

		/* Generate a single-word, singular LAT form. */
		lce.singleWord = singHead;

		/* Generate a multi-word, singular LAT form - rebuild
		 * the LAT with head replaced with its singular form.
		 * This also normalizes whitespaces as a side effect. */
		List<String> words = new ArrayList<>();
		for (Token t : JCasUtil.select(jcas, Token.class)) {
			if (t == head) {
				words.add(singHead);
			} else {
				words.add(t.getCoveredText());
			}
		}
		lce.singularForm = StringUtils.join(words, " ");

		jcas.release();

		return lce;
	}

	/** Modify the text of a given LAT. */
	protected void updateLAT(LAT lat, String text) {
		String oldText = lat.getText();
		lat.setText(text);
		lat.addToIndexes();
		logger.debug(".. LAT <<{}>> updated to <<{}>>", oldText, text);
	}

	/** Generate a copy of a given LAT, just with different text. */
	protected void copyLAT(LAT lat, String text) {
		LAT newLAT = (LAT) lat.clone();
		newLAT.setText(text);
		newLAT.addToIndexes();
		logger.debug(".. LAT <<{}>> copied to <<{}>>", lat.getText(), text);
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

