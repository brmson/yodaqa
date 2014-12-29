package cz.brmlab.yodaqa.analysis.answer;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;

/**
 * Generate a canonic form of the answer in CandidateAnswerCAS and
 * store it in the AnswerInfo. It is not for user consumption but for
 * internal comparison between syntactically similar answers.
 *
 * Basically, we lowercase, remove leading/trailing whitespace and
 * interpunction and leading DETs. */

public class SyntaxCanonization extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(SyntaxCanonization.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		String text = jcas.getDocumentText();
		String canonText = getCanonText(text);

		AnswerInfo ai = JCasUtil.selectSingle(jcas, AnswerInfo.class);
		ai.removeFromIndexes();
		ai.setCanonText(canonText);
		ai.addToIndexes();
	}

	/* Basically, throw away any DET and decorations at the
	 * beginning and end (like quotes, commas or apostrophes). */
	protected String getCanonText(String text) {
		text = text.toLowerCase();
		text = text.replaceAll("\\W*$", "");
		text = text.replaceAll("^\\W*", "");
		text = text.replaceAll("^(the|a|an|one)\\s+", "");
		text = text.replaceAll("^\\W*", "");
		return text;
	}
}
