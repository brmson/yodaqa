package cz.brmlab.yodaqa.annotator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.model.NLP.WordToken;

/**
 * Annotate individual words of a sofa based on a naive whitespace and
 * punctuation tokenization.
 *
 * Prospectively, we may want to supersede this with official UIMA's
 * WhitespaceTokenizer. */

public class WordTokenizer extends JCasAnnotator_ImplBase {
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		Matcher matcher = Pattern.compile("[^ ,.:?!\"'()\\[\\]]+").matcher(jcas.getDocumentText());

		while (matcher.find()) {
			WordToken token = new WordToken(jcas);
			token.setBegin(matcher.start());
			token.setEnd(matcher.end());
			token.addToIndexes();
		}
	}
}
