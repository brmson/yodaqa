package cz.brmlab.yodaqa.pipeline;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.model.FinalAnswer.Answer;

public class AnswerRanker extends CasAnnotator_ImplBase {
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(CAS aCAS) throws AnalysisEngineProcessException {
		try {
			Answer answer = new Answer(aCAS.getJCas());
			answer.setText(aCAS.getDocumentText());
			answer.setConfidence(1.0);
			answer.addToIndexes();
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}
}
