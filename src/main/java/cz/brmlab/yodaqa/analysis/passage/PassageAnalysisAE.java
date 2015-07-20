package cz.brmlab.yodaqa.analysis.passage;

import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.component.CasDumpWriter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.PipelineLogger;
import cz.brmlab.yodaqa.analysis.passage.biotagger.CanByBIOTaggerAE;
import cz.brmlab.yodaqa.provider.OpenNlpNamedEntities;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

/**
 * Annotate the PickedPassages view of SearchResultCAS with
 * deep NLP analysis and CandidateAnswer annotations.
 *
 * This is an aggregate AE that will run a variety of annotators on the
 * SearchResultCAS already trimmed down within the PickedPassages view,
 * first preparing it for answer generation and then actually producing
 * some CandiateAnswer annotations. */

public class PassageAnalysisAE /* XXX: extends AggregateBuilder ? */ {
	final static Logger logger = LoggerFactory.getLogger(PassageAnalysisAE.class);

	public static AnalysisEngineDescription createEngineDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		/* A bunch of DKpro-bound NLP processors (these are
		 * the giants we stand on the shoulders of) */
		/* The mix below corresponds to what we use in
		 * QuestionAnalysis, refer there for details. */
		/* Our passages are already split to sentences
		 * and tokenized. */

		/* POS, constituents, dependencies: */
		builder.add(createPrimitiveDescription(PipelineLogger.class,
					PipelineLogger.PARAM_LOG_MESSAGE, "Parsing"));
		builder.add(createPrimitiveDescription(
				StanfordParser.class,
				StanfordParser.PARAM_MAX_TOKENS, 50, // more takes a lot of RAM and is sloow, StanfordParser is O(N^2)
				StanfordParser.PARAM_WRITE_POS, true),
			CAS.NAME_DEFAULT_SOFA, "PickedPassages");

		/* Lemma features: */
		builder.add(createPrimitiveDescription(PipelineLogger.class,
					PipelineLogger.PARAM_LOG_MESSAGE, "Lemmatization"));
		builder.add(createPrimitiveDescription(LanguageToolLemmatizer.class),
			CAS.NAME_DEFAULT_SOFA, "PickedPassages");

		/* Named Entities: */
		builder.add(createPrimitiveDescription(PipelineLogger.class,
					PipelineLogger.PARAM_LOG_MESSAGE, "Named Entities"));
		builder.add(OpenNlpNamedEntities.createEngineDescription(),
			CAS.NAME_DEFAULT_SOFA, "PickedPassages");


		builder.add(createPrimitiveDescription(PipelineLogger.class,
					PipelineLogger.PARAM_LOG_MESSAGE, "QA analysis"));

		/* Question LAT Text Matches: */
		//builder.add(createPrimitiveDescription(MatchQuestionLATs.class));
		/* Question Clue Text Matches: */
		builder.add(createPrimitiveDescription(MatchQuestionClues.class));


		/* Okay! Now, we can proceed with our key tasks. */

		/* CandidateAnswer from each NP constituent that does not match
		 * any of the clues. */
		builder.add(createPrimitiveDescription(CanByNPSurprise.class));
		/* CandidateAnswer from each named entity that does not match
		 * any of the clues. */
		builder.add(createPrimitiveDescription(CanByNESurprise.class));
		/* Passages like: The <question focus> is <CandidateAnswer>. */
		builder.add(createPrimitiveDescription(CanByLATSubject.class));
		/* CandidateAnswer based on token sequence tagging with B-I-O
		 * labels; basically a custom answer-specific named entity
		 * recognizer. */
		builder.add(CanByBIOTaggerAE.createEngineDescription());

		/* Blacklist some answers that are just linguistic artifacts */
		builder.add(createPrimitiveDescription(CanBlacklist.class),
			CAS.NAME_DEFAULT_SOFA, "PickedPassages");


		/* Finishing touches: */

		/* Merge CandidateAnswer annotations with the same text. */
		builder.add(createPrimitiveDescription(CanMergeByText.class),
			CAS.NAME_DEFAULT_SOFA, "PickedPassages");


		/* Some debug dumps of the intermediate CAS. */
		if (false) {//logger.isDebugEnabled()) {
			builder.add(createPrimitiveDescription(
				CasDumpWriter.class,
				CasDumpWriter.PARAM_OUTPUT_FILE, "/tmp/yodaqa-pacas.txt"));
		}

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().setName("cz.brmlab.yodaqa.analysis.passage.PassageAnalysisAE");
		return aed;
	}
}
