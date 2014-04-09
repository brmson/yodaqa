package cz.brmlab.yodaqa;

import java.io.File;
import java.lang.Thread;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;

import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.io.interactive.InteractiveAnswerPrinter;
import cz.brmlab.yodaqa.io.interactive.InteractiveQuestionReader;
import cz.brmlab.yodaqa.pipeline.AnswerGenerator;
import cz.brmlab.yodaqa.pipeline.AnswerRanker;
import cz.brmlab.yodaqa.pipeline.PrimarySearch;
import cz.brmlab.yodaqa.pipeline.QuestionAnalysis;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;


public class YodaQAApp {
	public static void main(String[] args) throws Exception {
		CollectionReaderDescription reader = createReaderDescription(
				InteractiveQuestionReader.class);

		AnalysisEngineDescription questionAnalysis = QuestionAnalysis.createEngineDescription();
		AnalysisEngineDescription primarySearch = createEngineDescription(
				PrimarySearch.class);
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
				answerGenerator,
				answerRanker,
				printer);
	}
}
