package cz.brmlab.yodaqa;

import cz.brmlab.yodaqa.analysis.question.QuestionAnalysisAE;
import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.flow.asb.ParallelEngineFactory;
import cz.brmlab.yodaqa.io.collection.TSVQuestionReader;
import cz.brmlab.yodaqa.io.debug.QuestionPrinter;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

/**
 * Simplified pipeline to print QuestionAnalysis results
 */
public class QuestionDump {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: QuestionDump INPUT.TSV OUTPUT.json");
            System.err.println("Outputs the result of QuestionAnalysis");
            System.exit(1);
        }

        CollectionReaderDescription reader = createReaderDescription(
                TSVQuestionReader.class,
                TSVQuestionReader.PARAM_TSVFILE, args[0],
                TSVQuestionReader.PARAM_LANGUAGE, "en");

        AnalysisEngineDescription pipeline = QuestionAnalysisAE.createEngineDescription();


        AnalysisEngineDescription printer = createEngineDescription(
                QuestionPrinter.class,
                QuestionPrinter.PARAM_JSONFILE, args[1],
                ParallelEngineFactory.PARAM_NO_MULTIPROCESSING, 1);

     //   ParallelEngineFactory.registerFactory(); // comment out for a linear single-thread flow
		/* XXX: Later, we will want to create an actual flow
		 * to support scaleout. */
        MultiCASPipeline.runPipeline(reader,
                pipeline,
                printer);
    }

}
