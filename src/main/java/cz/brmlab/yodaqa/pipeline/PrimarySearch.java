package cz.brmlab.yodaqa.pipeline;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;

/**
 * Take a question CAS and search for keywords, yielding a search result
 * CAS instance.
 *
 * So far, this "searching" is super-primitive, just to place something
 * dummy in our pipeline.  It generates one result per question word,
 * with the word wrapped in an "a WORD b" template. */

public class PrimarySearch extends JCasMultiplier_ImplBase {
	/* Prepared list of words to consider. */
	String[] words;
	int i;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		words = jcas.getDocumentText().split(" ");
		i = 0;
	}

	public boolean hasNext() throws AnalysisEngineProcessException {
		return i < words.length;
	}

	public AbstractCas next() throws AnalysisEngineProcessException {
		JCas jcas = getEmptyJCas();
		try {
			jcas.setDocumentText("a " + words[i] + " b");

			ResultInfo ri = new ResultInfo(jcas);
			ri.setRelevance(1.0 / (i + 1.0));
			ri.setIsLast(i == words.length - 1);
			ri.addToIndexes();
		} catch (Exception e) {
			jcas.release();
			throw new AnalysisEngineProcessException(e);
		}
		i++;
		return jcas;
	}
}
