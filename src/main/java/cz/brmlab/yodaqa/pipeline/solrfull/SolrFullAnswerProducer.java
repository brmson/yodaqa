package cz.brmlab.yodaqa.pipeline.solrfull;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.FlowControllerFactory;
import org.apache.uima.flow.impl.FixedFlowController;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.analysis.passage.PassageAnalysisAE;
import cz.brmlab.yodaqa.analysis.passextract.PassageExtractorAE;
import cz.brmlab.yodaqa.flow.FixedParallelFlowController;

/**
 * From the QuestionCAS, generate a bunch of CandidateAnswerCAS instances.
 *
 * This is an aggregate AE that will run a particular flow based on primary
 * search, result analysis, passage extraction and generating candidate
 * answers from chosen document passages.
 *
 * We actually just reuse the provided snippets and generate passages
 * from them. */

public class SolrFullAnswerProducer /* XXX: extends AggregateBuilder ? */ {
	public static AnalysisEngineDescription createEngineDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		AnalysisEngineDescription passageProducer = createPassageProducerDescription();
		builder.add(passageProducer);
		AnalysisEngineDescription passageAnalysis = PassageAnalysisAE.createEngineDescription();
		builder.add(passageAnalysis);
		AnalysisEngineDescription answerGenerator = AnalysisEngineFactory.createEngineDescription(
				AnswerGenerator.class);
		builder.add(answerGenerator);

		builder.setFlowControllerDescription(
				FlowControllerFactory.createFlowControllerDescription(
					FixedFlowController.class,
					FixedFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
		return aed;
	}

	public static AnalysisEngineDescription createPassageProducerDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		/* Since each of these CAS multipliers will eventually produce
		 * a single CAS marked as "isLast", if you add another one
		 * here, you must also bump the AnswerCASMerger parameter
		 * PARAM_ISLAST_BARRIER. */

		AnalysisEngineDescription fromSnippet = AnalysisEngineFactory.createEngineDescription(
				SnippetPassageProducer.class);
		builder.add(fromSnippet);

		builder.setFlowControllerDescription(
				FlowControllerFactory.createFlowControllerDescription(
					FixedParallelFlowController.class,
					FixedParallelFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
		return aed;
	}
}
