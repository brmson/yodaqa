package cz.brmlab.yodaqa.pipeline.structured;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ResourceInitializationException;


/**
 * From the QuestionCAS, generate a bunch of CandidateAnswerCAS instances.
 *
 * In this case, this is just a thin wrapper of FreebaseOntologyPrimarySearch,
 * producing Freebase relationships of ClueSubject entities as answers.
 */

public class FreebaseOntologyAnswerProducer extends StructuredAnswerProducer {
	public static AnalysisEngineDescription createEngineDescription()
			throws ResourceInitializationException {
		return createEngineDescription("cz.brmlab.yodaqa.pipeline.structured.FreebaseOntologyAnswerProducer",
				FreebaseOntologyPrimarySearch.class);
	}
}
