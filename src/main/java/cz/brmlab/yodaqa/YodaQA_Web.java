package cz.brmlab.yodaqa;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;

import cz.brmlab.yodaqa.flow.MultiCASPipeline;
import cz.brmlab.yodaqa.flow.asb.ParallelEngineFactory;
import cz.brmlab.yodaqa.io.web.WebInterface;
import cz.brmlab.yodaqa.io.web.WebAnswerPrinter;
import cz.brmlab.yodaqa.io.web.WebQuestionReader;
import cz.brmlab.yodaqa.pipeline.YodaQA;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;


/* YodaQA_Web is a web interface that can asynchronously communicate while
 * pipeline(s) are running; it can provide an end-user interface, but
 * primarily is meant to be used as a REST API.  Open the URL
 *
 * 	http://localhost:4567/
 *
 * This is of course all completely experimental and the API is not stable
 * for now. */

public class YodaQA_Web {
	public static void main(String[] args) throws Exception {
		WebInterface web = new WebInterface();
		Thread webThread = new Thread(web);
		webThread.setDaemon(true);
		webThread.start();

		CollectionReaderDescription reader = createReaderDescription(
				WebQuestionReader.class,
				WebQuestionReader.PARAM_LANGUAGE, "en");

		AnalysisEngineDescription pipeline = YodaQA.createEngineDescription();

		AnalysisEngineDescription printer = createEngineDescription(
				WebAnswerPrinter.class);

		ParallelEngineFactory.registerFactory(); // comment out for a linear single-thread flow
		/* XXX: Later, we will want to create an actual flow
		 * to support scaleout. */
		MultiCASPipeline.runPipeline(reader,
				pipeline,
				printer);
	}
}
