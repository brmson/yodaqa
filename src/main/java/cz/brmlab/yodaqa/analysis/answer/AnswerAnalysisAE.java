package cz.brmlab.yodaqa.analysis.answer;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.component.CasDumpWriter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.FindReqParse;
import cz.brmlab.yodaqa.analysis.tycor.LATByWordnet;
import cz.brmlab.yodaqa.analysis.tycor.LATMatchTyCor;

import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

/**
 * Annotate the CandidateAnswerCAS, eventually producing various features.
 *
 * This is an aggregate AE that will run a variety of annotators on the
 * CandidateAnswerCAS, running possibly more detailed linguistic analysis,
 * determining its LAT etc. and correlating the results with the Question
 * view to estimate correctness. */

public class AnswerAnalysisAE /* XXX: extends AggregateBuilder ? */ {
	final static Logger logger = LoggerFactory.getLogger(AnswerAnalysisAE.class);

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
		 * adjectives, but can be a complex subsentence system. */

		/* In addition, we already have StanfordParser annotations,
		 * so tokens, POS, lemmas, constituents and dependencies.
		 * One exception is if the source sentence was too long;
		 * in that case, rerun StanfordParser just on the answer. */
		builder.add(createPrimitiveDescription(FindReqParse.class),
			CAS.NAME_DEFAULT_SOFA, "Answer");
		builder.add(createPrimitiveDescription(
				StanfordParser.class,
				StanfordParser.PARAM_MAX_TOKENS, 50, // more takes a lot of RAM and is sloow, StanfordParser is O(N^2)
				StanfordParser.PARAM_ANNOTATIONTYPE_TO_PARSE, "cz.brmlab.yodaqa.model.CandidateAnswer.PassageForParsing"),
			CAS.NAME_DEFAULT_SOFA, "Answer");

		/* Determine the focus and LAT of each answer. */
		builder.add(createPrimitiveDescription(FocusGenerator.class),
			CAS.NAME_DEFAULT_SOFA, "Answer");
		builder.add(createPrimitiveDescription(LATByFocus.class),
			CAS.NAME_DEFAULT_SOFA, "Answer");
		/* Multiplicate LATs */
		builder.add(createPrimitiveDescription(LATByWordnet.class),
			CAS.NAME_DEFAULT_SOFA, "Answer");

		/* Perform type coercion. */
		builder.add(createPrimitiveDescription(LATMatchTyCor.class));


		/* Some debug dumps of the intermediate CAS. */
		if (false) {//logger.isDebugEnabled()) {
			builder.add(createPrimitiveDescription(
				CasDumpWriter.class,
				CasDumpWriter.PARAM_OUTPUT_FILE, "/tmp/yodaqa-aacas.txt"));
		}

		return builder.createAggregateDescription();
	}
}
