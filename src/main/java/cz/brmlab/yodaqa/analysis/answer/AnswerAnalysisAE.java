package cz.brmlab.yodaqa.analysis.answer;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.component.CasDumpWriter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.FindReqParse;
import cz.brmlab.yodaqa.analysis.tycor.LATByWordnet;
import cz.brmlab.yodaqa.analysis.tycor.LATMatchTyCor;
import cz.brmlab.yodaqa.analysis.tycor.LATNormalize;

import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
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
				StanfordParser.PARAM_ANNOTATIONTYPE_TO_PARSE, "cz.brmlab.yodaqa.model.CandidateAnswer.PassageForParsing",
				StanfordParser.PARAM_WRITE_POS, true),
			CAS.NAME_DEFAULT_SOFA, "Answer");

		/* Generate and store on the side a "syntactically canonical
		 * form" of the answer text. */
		builder.add(createPrimitiveDescription(SyntaxCanonization.class),
			CAS.NAME_DEFAULT_SOFA, "Answer");

		/* Determine overlaps of the answer and question clues. */
		builder.add(createPrimitiveDescription(AnswerClueOverlap.class));

		/* Determine the focus of each answer. */
		builder.add(createPrimitiveDescription(FocusGenerator.class),
			CAS.NAME_DEFAULT_SOFA, "Answer");

		/* Generate Wordnet instance-of based LATs */
		builder.add(createPrimitiveDescription(LATByWnInstance.class),
			CAS.NAME_DEFAULT_SOFA, "Answer");
		/* Generate DBpedia LATs based on wordnet mappings */
		builder.add(createPrimitiveDescription(LATByDBpediaWN.class),
			CAS.NAME_DEFAULT_SOFA, "Answer");
		/* Generate DBpedia LATs based on type properties
		 * (mostly article category based; noisiest source) */
		builder.add(createPrimitiveDescription(LATByDBpedia.class),
			CAS.NAME_DEFAULT_SOFA, "Answer");

		/* Post-process LATs gathered so far: */
		/* Convert plurals to singulars, spin off single-word LATs
		 * from multi-words. */
		builder.add(createPrimitiveDescription(LATNormalize.class),
			CAS.NAME_DEFAULT_SOFA, "Answer");
		/* Multiplicate LATs by wordnet hypernymy. */
		builder.add(createPrimitiveDescription(LATByWordnet.class),
			CAS.NAME_DEFAULT_SOFA, "Answer");

		/* We do no LAT multiplication below here since all of our LATs
		 * are already "on-the-spot" and won't benefit from further
		 * generalization - already sufficiently generic. */

		/* Generate NamedEntity type LATs */
		builder.add(createPrimitiveDescription(LATByNE.class),
			CAS.NAME_DEFAULT_SOFA, "Answer");
		/* Generate LATs based on quantity statements. */
		builder.add(createPrimitiveDescription(LATByQuantity.class),
			CAS.NAME_DEFAULT_SOFA, "Answer");

		/* Perform type coercion. */
		builder.add(createPrimitiveDescription(LATMatchTyCor.class));
		/* Add features accordign to the question class*/
		builder.add(AnalysisEngineFactory.createEngineDescription(QuestionClassFeatures.class));


		/* Some debug dumps of the intermediate CAS. */
		if (false) {//logger.isDebugEnabled()) {
			builder.add(createPrimitiveDescription(
				CasDumpWriter.class,
				CasDumpWriter.PARAM_OUTPUT_FILE, "/tmp/yodaqa-aacas.txt"));
		}

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().setName("cz.brmlab.yodaqa.analysis.answer.AnswerAnalysisAE");
		return aed;
	}
}
