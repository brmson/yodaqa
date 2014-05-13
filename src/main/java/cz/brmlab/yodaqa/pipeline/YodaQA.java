package cz.brmlab.yodaqa.pipeline;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.FlowControllerFactory;
import org.apache.uima.flow.impl.FixedFlowController;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.analysis.answer.AnswerAnalysisAE;
import cz.brmlab.yodaqa.analysis.question.QuestionAnalysisAE;
import cz.brmlab.yodaqa.pipeline.solrfull.SolrFullAnswerProducer;
import cz.brmlab.yodaqa.provider.SolrNamedSource;

/**
 * The main YodaQA pipeline.
 *
 * This is an aggregate AE that will run all stages of a pipeline
 * that takes a fresh QuestionCAS on its input (from a collection
 * reader) and produces a cooked FinalAnswerCAS on its output (for
 * the consumer). */

public class YodaQA /* XXX: extends AggregateBuilder ? */ {
	static {
		try {
			/* We have two options here - either use a local embedded
			 * instance based on a solr index in a given local directory,
			 * or connect to a remote instance (e.g. indexing Wikipedia).
			 *
			 * By default, we connect to a remote instance; see README
			 * for instructions on how to set up your own.  Uncomment
			 * the guten line below and comment the enwiki one to use
			 * a local solr core instead, again see README for
			 * instructions on how to obtain an example one. */

			//SolrNamedSource.register("guten", "data/guten", null);
			SolrNamedSource.register("enwiki", "collection1", "http://pasky.or.cz:8983/solr/");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("*** Exception caught during SolrNamedSource initialization. ***");
			System.err.println("You will get a fatal NullPointerException later, but the issue is above.");
		}
	}

	public static AnalysisEngineDescription createEngineDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		AnalysisEngineDescription questionAnalysis = QuestionAnalysisAE.createEngineDescription();
		builder.add(questionAnalysis);

		AnalysisEngineDescription answerProducer = SolrFullAnswerProducer.createEngineDescription();
		builder.add(answerProducer);

		AnalysisEngineDescription answerAnalysis = AnswerAnalysisAE.createEngineDescription();
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
