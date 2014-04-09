package cz.brmlab.yodaqa.pipeline;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
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

		builder.add(createPrimitiveDescription(WordTokenizer.class));

		return builder.createAggregateDescription();
	}
}
