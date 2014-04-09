package cz.brmlab.yodaqa;

import java.io.File;
import java.lang.Thread;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;

import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.io.interactive.InteractiveQuestionReader;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;


public class YodaQAApp {
	public static void main(String[] args) throws Exception {
		CollectionReaderDescription reader = createReaderDescription(
				InteractiveQuestionReader.class);

		/* FIXME: Support for aggregate CASes not yet ready.
		AnalysisEngineDescription questionAnalysis = createEngineDescription(
				"cz.brmlab.yodaqa.pipeline.QuestionAnalysis");
				*/
		AnalysisEngineDescription questionAnalysis = createEngineDescription(
				"cz.brmlab.yodaqa.annotator.WordTokenizer");
		AnalysisEngineDescription primarySearch = createEngineDescription(
				"cz.brmlab.yodaqa.pipeline.PrimarySearch");
		AnalysisEngineDescription answerGenerator = createEngineDescription(
				"cz.brmlab.yodaqa.pipeline.AnswerGenerator");
		AnalysisEngineDescription answerRanker = createEngineDescription(
				"cz.brmlab.yodaqa.pipeline.AnswerRanker");
		AnalysisEngineDescription printer = createEngineDescription(
				"cz.brmlab.yodaqa.io.interactive.InteractiveAnswerPrinter");

		/* XXX: Later, we will want to create an actual flow
		 * to support scaleout. */
		MultiCASPipeline.runPipeline(reader,
				questionAnalysis,
				primarySearch,
				answerGenerator,
				answerRanker,
				printer);
	}
}
