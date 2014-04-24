package cz.brmlab.yodaqa;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;

import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.io.interactive.InteractiveAnswerPrinter;
import cz.brmlab.yodaqa.io.interactive.InteractiveQuestionReader;
import cz.brmlab.yodaqa.pipeline.AnswerGenerator;
import cz.brmlab.yodaqa.pipeline.AnswerRanker;
import cz.brmlab.yodaqa.pipeline.PrimarySearch;
import cz.brmlab.yodaqa.pipeline.ResultAnalysis;
import cz.brmlab.yodaqa.pipeline.QuestionAnalysis;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;


public class YodaQA_Interactive {
	public static void main(String[] args) throws Exception {
		CollectionReaderDescription reader = createReaderDescription(
				InteractiveQuestionReader.class,
				InteractiveQuestionReader.PARAM_LANGUAGE, "en");

		AnalysisEngineDescription questionAnalysis = QuestionAnalysis.createEngineDescription();

		/* We have two options here - either use a local embedded
		 * instance based on a solr index in a given local directory,
		 * or connect to a remote instance (e.g. indexing Wikipedia).
		 *
		 * By default, we connect to a remote instance; see README for
		 * instructions on how to set up your own.  Uncomment the
		 * code below to use a local solr core instead, again see
		 * README for instructions on how to obtain an example one. */
		/*
		AnalysisEngineDescription primarySearch = createEngineDescription(
				PrimarySearch.class,
				"embedded", true,
				"core", "data/guten");
				*/
		AnalysisEngineDescription primarySearch = createEngineDescription(
				PrimarySearch.class,
				"embedded", false,
				"server-url", "http://pasky.or.cz:8983/solr/",
				"core", "collection1");

		AnalysisEngineDescription resultAnalysis = ResultAnalysis.createEngineDescription();
		AnalysisEngineDescription answerGenerator = createEngineDescription(
				AnswerGenerator.class);
		AnalysisEngineDescription answerRanker = createEngineDescription(
				AnswerRanker.class);
		AnalysisEngineDescription printer = createEngineDescription(
				InteractiveAnswerPrinter.class);

		/* XXX: Later, we will want to create an actual flow
		 * to support scaleout. */
		MultiCASPipeline.runPipeline(reader,
				questionAnalysis,
				primarySearch,
				resultAnalysis,
				answerGenerator,
				answerRanker,
				printer);
	}
}
