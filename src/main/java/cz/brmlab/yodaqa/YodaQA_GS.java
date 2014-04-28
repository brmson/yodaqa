package cz.brmlab.yodaqa;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;

import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.io.collection.CollectionQuestionReader;
import cz.brmlab.yodaqa.io.collection.GoldStandardAnswerPrinter;
import cz.brmlab.yodaqa.pipeline.YodaQA;

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

		AnalysisEngineDescription pipeline = YodaQA.createEngineDescription();

		AnalysisEngineDescription printer = createEngineDescription(
				GoldStandardAnswerPrinter.class,
				GoldStandardAnswerPrinter.PARAM_TSVFILE, args[1]);

		/* XXX: Later, we will want to create an actual flow
		 * to support scaleout. */
		MultiCASPipeline.runPipeline(reader,
				pipeline,
				printer);
	}
}
