package cz.brmlab.yodaqa.pipeline;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasMultiplier_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;

import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;

/**
 * Take a question CAS and search for keywords, yielding a search result
 * CAS instance.
 *
 * So far, this "searching" is super-primitive, just to place something
 * dummy in our pipeline.  It just generates one result per clue with
 * the clue as the sofa. */

public class PrimarySearch extends JCasMultiplier_ImplBase {
	JCas src_jcas;
	FSIterator clues;
	int i;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		clues = jcas.getAnnotationIndex(Clue.type).iterator();
		i = 0;

		src_jcas = jcas;
	}

	public boolean hasNext() throws AnalysisEngineProcessException {
		return clues.hasNext();
	}

	public AbstractCas next() throws AnalysisEngineProcessException {
		JCas jcas = getEmptyJCas();
		try {
			jcas.createView("Question");
			copyQuestion(src_jcas, jcas.getView("Question"));

			jcas.createView("Result");
			generateResult(clues, jcas.getView("Result"));
		} catch (Exception e) {
			jcas.release();
			throw new AnalysisEngineProcessException(e);
		}
		i++;
		return jcas;
	}

	protected void copyQuestion(JCas src, JCas jcas) throws Exception {
		CasCopier.copyCas(src.getCas(), jcas.getCas(), true);
	}

	protected void generateResult(FSIterator clues, JCas jcas) throws Exception {
		Clue clue = (Clue) clues.next();
		jcas.setDocumentText(clue.getCoveredText());

		ResultInfo ri = new ResultInfo(jcas);
		ri.setRelevance(1.0 / (i + 1.0));
		ri.setIsLast(!clues.hasNext());
		ri.addToIndexes();
	}
}
