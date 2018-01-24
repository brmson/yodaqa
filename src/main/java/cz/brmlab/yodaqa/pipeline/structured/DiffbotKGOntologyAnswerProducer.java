package cz.brmlab.yodaqa.pipeline.structured;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ResourceInitializationException;

public class DiffbotKGOntologyAnswerProducer extends StructuredAnswerProducer {
	public static AnalysisEngineDescription createEngineDescription()
			throws ResourceInitializationException {
		return createEngineDescription("cz.brmlab.yodaqa.pipeline.structured.DiffbotKGOntologyAnswerProducer",
				DiffbotKGOntologyPrimarySearch.class);
	}
}
