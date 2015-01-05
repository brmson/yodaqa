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
import cz.brmlab.yodaqa.pipeline.AnswerGenerator;
import cz.brmlab.yodaqa.pipeline.ResultGenerator;

/**
 * From the QuestionCAS, generate a bunch of CandidateAnswerCAS instances.
 *
 * This is an aggregate AE that will run a particular flow based on primary
 * search, result analysis, passage extraction and generating candidate
 * answers from chosen document passages.
 *
 * In this case, the flow is based on processing fulltext results of
 * a Solr search. */

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

		AnalysisEngineDescription fulltext = createFulltextPassageProducerDescription();
		builder.add(fulltext);
		AnalysisEngineDescription titleInClue = createTitleInCluePassageProducerDescription();
		builder.add(titleInClue);

		builder.setFlowControllerDescription(
				FlowControllerFactory.createFlowControllerDescription(
					FixedParallelFlowController.class,
					FixedParallelFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
		return aed;
	}

	public static AnalysisEngineDescription createFulltextPassageProducerDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		AnalysisEngineDescription primarySearch = AnalysisEngineFactory.createEngineDescription(
				SolrFullPrimarySearch.class,
				SolrFullPrimarySearch.PARAM_RESULT_INFO_ORIGIN, "cz.brmlab.yodaqa.pipeline.solrfull.fulltext");
		builder.add(primarySearch);
		AnalysisEngineDescription resultGenerator = AnalysisEngineFactory.createEngineDescription(
				ResultGenerator.class,
				ResultGenerator.PARAM_RESULT_INFO_ORIGIN, "cz.brmlab.yodaqa.pipeline.solrfull.fulltext");
		builder.add(resultGenerator);
		AnalysisEngineDescription passageExtractor = PassageExtractorAE.createEngineDescription(
				PassageExtractorAE.PARAM_PASS_SEL_BYCLUE);
		builder.add(passageExtractor);

		builder.setFlowControllerDescription(
				FlowControllerFactory.createFlowControllerDescription(
					FixedFlowController.class,
					FixedFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
		return aed;
	}

	public static AnalysisEngineDescription createTitleInCluePassageProducerDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		/* XXX: In SolrFullPrimarySearch, when generating features,
		 * we assume that PARAM_SEARCH_FULL_TEXT always corresponds
		 * to passage PARAM_PASS_SEL_FIRST and title-in-clue search.
		 * If more variations are created, we will need to adjust
		 * feature generation. */
		AnalysisEngineDescription primarySearch = AnalysisEngineFactory.createEngineDescription(
				SolrFullPrimarySearch.class,
				SolrFullPrimarySearch.PARAM_RESULT_INFO_ORIGIN, "cz.brmlab.yodaqa.pipeline.solrfull.titleInClue",
				SolrFullPrimarySearch.PARAM_SEARCH_FULL_TEXT, false,
				SolrFullPrimarySearch.PARAM_CLUES_ALL_REQUIRED, false);
		builder.add(primarySearch);
		AnalysisEngineDescription resultGenerator = AnalysisEngineFactory.createEngineDescription(
				ResultGenerator.class,
				ResultGenerator.PARAM_RESULT_INFO_ORIGIN, "cz.brmlab.yodaqa.pipeline.solrfull.titleInClue");
		builder.add(resultGenerator);
		AnalysisEngineDescription passageExtractor = PassageExtractorAE.createEngineDescription(
				PassageExtractorAE.PARAM_PASS_SEL_FIRST);
		builder.add(passageExtractor);

		builder.setFlowControllerDescription(
				FlowControllerFactory.createFlowControllerDescription(
					FixedFlowController.class,
					FixedFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
		return aed;
	}
}
