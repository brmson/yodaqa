package cz.brmlab.yodaqa;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;

import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.io.collection.CollectionQuestionReader;
import cz.brmlab.yodaqa.io.collection.GoldStandardAnswerPrinter;
import cz.brmlab.yodaqa.pipeline.AnswerGenerator;
import cz.brmlab.yodaqa.pipeline.AnswerMerger;
import cz.brmlab.yodaqa.pipeline.PrimarySearch;
import cz.brmlab.yodaqa.pipeline.ResultAnalysis;
import cz.brmlab.yodaqa.pipeline.QuestionAnalysis;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;


/* FIXME: Massive code duplication of YodaQA_Interactive and YodaQA_GS.
 * Let's abstract out the processing pipeline later. */

public class YodaQA_GS {
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: YodaQA_GS INPUT.TSV OUTPUT.TSV");
			System.err.println("Measures YodaQA performance on some Gold Standard questions.");
			System.exit(1);
		}

		CollectionReaderDescription reader = createReaderDescription(
				CollectionQuestionReader.class,
				CollectionQuestionReader.PARAM_TSVFILE, args[0],
				CollectionQuestionReader.PARAM_LANGUAGE, "en");

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
		AnalysisEngineDescription answerMerger = createEngineDescription(
				AnswerMerger.class);
		AnalysisEngineDescription printer = createEngineDescription(
				GoldStandardAnswerPrinter.class,
				GoldStandardAnswerPrinter.PARAM_TSVFILE, args[1]);

		/* XXX: Later, we will want to create an actual flow
		 * to support scaleout. */
		MultiCASPipeline.runPipeline(reader,
				questionAnalysis,
				primarySearch,
				resultAnalysis,
				answerGenerator,
				answerMerger,
				printer);
	}
}
