package cz.brmlab.yodaqa.analysis.result;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;
import cz.brmlab.yodaqa.model.SearchResult.Passage;

/**
 * Create CandidateAnswers simply from whole Passages.
 *
 * This is just a naive generator for debugging. */

@SofaCapability(
	inputSofas = { "PickedPassages", "Result" },
	outputSofas = { "Result" }
)

public class CanByPassage extends JCasAnnotator_ImplBase {
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas passagesView, resultView;
		try {
			passagesView = jcas.getView("PickedPassages");
			resultView = jcas.getView("Result");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}
		for (Passage passage : JCasUtil.select(passagesView, Passage.class)) {
			System.err.println("ca " + passage.getCoveredText());
			CandidateAnswer ca = new CandidateAnswer(resultView);
			ca.setBegin(passage.getBegin());
			ca.setEnd(passage.getEnd());
			ca.setPassage(passage);
			ca.setBase(passage);
			ca.setConfidence(1.0);
			ca.addToIndexes();
		}
	}
}
