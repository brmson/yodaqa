package cz.brmlab.yodaqa;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;

import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.io.bioasq.BioASQQuestionReader;
import cz.brmlab.yodaqa.io.bioasq.GoldStandardAnswerPrinter;
import cz.brmlab.yodaqa.pipeline.YodaQA;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;


public class YodaQA_BioASQGS {
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: YodaQA_GS INPUT.JSON OUTPUT.TSV");
			System.err.println("Measures YodaQA performance on some BioASQ Gold Standard (or not) questions.");
			System.exit(1);
		}

		CollectionReaderDescription reader = createReaderDescription(
				BioASQQuestionReader.class,
				BioASQQuestionReader.PARAM_JSONFILE, args[0],
				BioASQQuestionReader.PARAM_LANGUAGE, "en");

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
