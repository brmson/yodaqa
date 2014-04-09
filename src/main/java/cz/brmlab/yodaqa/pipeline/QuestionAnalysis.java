package cz.brmlab.yodaqa.pipeline;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.component.CasDumpWriter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.annotator.WordTokenizer;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

/**
 * Annotate the QuestionCAS.
 *
 * This is an aggregate AE that will run a variety of annotators on the
 * QuestionCAS, preparing it for the PrimarySearch and AnswerGenerator
 * stages. */

public class QuestionAnalysis /* XXX: extends AggregateBuilder ? */ {
	public static AnalysisEngineDescription createEngineDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		/* Our way to tokenize (TODO: to be phased out) */
		builder.add(createPrimitiveDescription(WordTokenizer.class));

		/* DKPro tokenizer */
		builder.add(createPrimitiveDescription(BreakIteratorSegmenter.class));
		/* DKPro pos tagger */
		builder.add(createPrimitiveDescription(OpenNlpPosTagger.class));

		/* Dump the intermediate CAS. */
		/* builder.add(createPrimitiveDescription(
			CasDumpWriter.class,
			CasDumpWriter.PARAM_OUTPUT_FILE, "/tmp/yodaqa-qacas.txt")); */

		return builder.createAggregateDescription();
	}
}
