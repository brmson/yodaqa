package cz.brmlab.yodaqa;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;

import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.flow.asb.ParallelEngineFactory;
import cz.brmlab.yodaqa.io.machine.MachineAnswerPrinter;
import cz.brmlab.yodaqa.io.machine.MachineQuestionReader;
import cz.brmlab.yodaqa.pipeline.YodaQA;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;


/* YodaQA_Machine is almost like YodaQA_Interactive but writing the answers
 * in a machine readable format to stdout, which is expected to be a pipe
 * tied up to a programmatic consumer.  This allows YodaQA to be used as
 * a component in other software.  For example, the
 * 	contrib/irssi-brmson-pipe.pl
 * script uses it to connect YodaQA to IRC.
 *
 * XXX DEPRECATED - please use the REST API of the web interface instead.
 * Whenever someone rewrites the irssi connector, this interface *will*
 * go away. */

public class YodaQA_Machine {
	public static void main(String[] args) throws Exception {
		CollectionReaderDescription reader = createReaderDescription(
				MachineQuestionReader.class,
				MachineQuestionReader.PARAM_LANGUAGE, "en");

		AnalysisEngineDescription pipeline = YodaQA.createEngineDescription();

		AnalysisEngineDescription printer = createEngineDescription(
				MachineAnswerPrinter.class);

		ParallelEngineFactory.registerFactory(); // comment out for a linear single-thread flow
		/* XXX: Later, we will want to create an actual flow
		 * to support scaleout. */
		MultiCASPipeline.runPipeline(reader,
				pipeline,
				printer);
	}
}
