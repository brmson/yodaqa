package cz.brmlab.yodaqa.pipeline;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.FlowControllerFactory;
import org.apache.uima.flow.impl.FixedFlowController;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * The main YodaQA pipeline.
 *
 * This is an aggregate AE that will run all stages of a pipeline
 * that takes a fresh QuestionCAS on its input (from a collection
 * reader) and produces a cooked FinalAnswerCAS on its output (for
 * the consumer). */

public class YodaQA /* XXX: extends AggregateBuilder ? */ {
	public static AnalysisEngineDescription createEngineDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		AnalysisEngineDescription questionAnalysis = QuestionAnalysis.createEngineDescription();
		builder.add(questionAnalysis);

		/* We have two options here - either use a local embedded
		 * instance based on a solr index in a given local directory,
		 * or connect to a remote instance (e.g. indexing Wikipedia).
		 *
		 * By default, we connect to a remote instance; see README for
		 * instructions on how to set up your own.  Uncomment the
		 * code below to use a local solr core instead, again see
		 * README for instructions on how to obtain an example one. */
		/*
		AnalysisEngineDescription primarySearch = AnalysisEngineFactory.createEngineDescription(
				PrimarySearch.class,
				"embedded", true,
				"core", "data/guten");
				*/
		AnalysisEngineDescription primarySearch = AnalysisEngineFactory.createEngineDescription(
				PrimarySearch.class,
				"embedded", false,
				"server-url", "http://pasky.or.cz:8983/solr/",
				"core", "collection1");
		builder.add(primarySearch);

		AnalysisEngineDescription resultAnalysis = ResultAnalysis.createEngineDescription();
		builder.add(resultAnalysis);

		AnalysisEngineDescription answerGenerator = AnalysisEngineFactory.createEngineDescription(
				AnswerGenerator.class);
		builder.add(answerGenerator);

		AnalysisEngineDescription answerAnalysis = AnswerAnalysis.createEngineDescription();
		builder.add(answerAnalysis);

		AnalysisEngineDescription answerMerger = AnalysisEngineFactory.createEngineDescription(
				AnswerMerger.class);
		builder.add(answerMerger);

		builder.setFlowControllerDescription(
				FlowControllerFactory.createFlowControllerDescription(
					FixedFlowController.class,
					FixedFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
		return aed;
	}
}
