package cz.brmlab.yodaqa.pipeline;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.component.CasDumpWriter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.analysis.answer.FocusGenerator;
import cz.brmlab.yodaqa.analysis.answer.LATGenerator;
import cz.brmlab.yodaqa.analysis.tycor.LATByWordnet;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

/**
 * Annotate the CandidateAnswerCAS.
 *
 * This is an aggregate AE that will run a variety of annotators on the
 * CandidateAnswerCAS, running possibly more detailed linguistic analysis,
 * determining its LAT etc. and correlating the results with the Question
 * view to estimate correctness. */

public class AnswerAnalysis /* XXX: extends AggregateBuilder ? */ {
	public static AnalysisEngineDescription createEngineDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		/* At this point, we have a piece of a sentence, e.g. ...
		 *   They
		 *   mathematics
		 *   the circuit
		 *   surface contaminants
		 *   several complex variables
		 *   many different and incompatible systems of notation for them
		 *   characteristics that are especially important for 'long tailed pair' differential amplifiers
		 *   an amplifier or an electrically controlled switch
		 *   The importance of this concept was realised first in the analytic theory of theta functions, and geometrically in the theory of bitangents
		 * ...so usually it's a simple term plus possibly some
		 * adjectives, but can be a complex subsentence system.
		 * In addition, we already have StanfordParser annotations,
		 * so tokens, POS, lemmas, constituents and dependencies. */

		/* Determine the focus and LAT of each answer. */
		builder.add(createPrimitiveDescription(FocusGenerator.class));
		builder.add(createPrimitiveDescription(LATGenerator.class));
		builder.add(createPrimitiveDescription(LATByWordnet.class));


		/* Some debug dumps of the intermediate CAS. */
		builder.add(createPrimitiveDescription(
			CasDumpWriter.class,
			CasDumpWriter.PARAM_OUTPUT_FILE, "/tmp/yodaqa-aacas.txt"));

		return builder.createAggregateDescription();
	}
}
