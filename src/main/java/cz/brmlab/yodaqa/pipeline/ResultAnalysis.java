package cz.brmlab.yodaqa.pipeline;

import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpNameFinder;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.component.CasDumpWriter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.result.CanByNPSurprise;
import cz.brmlab.yodaqa.analysis.result.CanMergeByText;
import cz.brmlab.yodaqa.analysis.result.PassByClue;
import cz.brmlab.yodaqa.analysis.result.CanByPassage;
import cz.brmlab.yodaqa.analysis.result.PassFilter;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

/**
 * Annotate the SearchResultCAS.
 *
 * This is an aggregate AE that will run a variety of annotators on the
 * SearchResultCAS, first preparing it for answer generation and then
 * actually producing some CandiateAnswer annotations. */

public class ResultAnalysis /* XXX: extends AggregateBuilder ? */ {
	final static Logger logger = LoggerFactory.getLogger(ResultAnalysis.class);

	public static AnalysisEngineDescription createEngineDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		/* A bunch of DKpro-bound NLP processors (these are
		 * the giants we stand on the shoulders of) */
		/* The mix below corresponds to what we use in
		 * QuestionAnalysis, refer there for details. */

		/* Token features: */
		// LanguageToolsSegmenter is the only one capable of dealing
		// with incomplete sentences e.g. separated by paragraphs etc.
		builder.add(createPrimitiveDescription(LanguageToolSegmenter.class),
			CAS.NAME_DEFAULT_SOFA, "Result");

		/* At this point, we can filter the source to keep
		 * only sentences and tokens we care about: */
		builder.add(createPrimitiveDescription(PassByClue.class));

		/* Further cut these only to the most interesting N sentences. */
		builder.add(createPrimitiveDescription(PassFilter.class));

		/* POS, lemmas, constituents, dependencies: */
		builder.add(createPrimitiveDescription(
				StanfordParser.class,
				StanfordParser.PARAM_MAX_TOKENS, 50), // more takes a lot of RAM and is sloow, StanfordParser is O(N^2)
			CAS.NAME_DEFAULT_SOFA, "PickedPassages");

		/* Named Entities: */

		String[] ner_variants = {
		      "date", "location", "money", "organization",
		      "percentage", "person", "time"
		};
		for (String variant : ner_variants) {
			builder.add(createPrimitiveDescription(
					OpenNlpNameFinder.class,
					OpenNlpNameFinder.PARAM_VARIANT, variant),
				CAS.NAME_DEFAULT_SOFA, "PickedPassages");
		}



		/* Okay! Now, we can proceed with our key tasks. */

		/* CandidateAnswer from each (complete) Passage - just for debugging. */
		//builder.add(createPrimitiveDescription(CanByPassage.class));
		/* CandidateAnswer from each NP constituent that does not match
		 * any of the clues - this might actually be useful! */
		builder.add(createPrimitiveDescription(CanByNPSurprise.class));


		/* Finishing touches: */

		/* Merge CandidateAnswer annotations with the same text. */
		builder.add(createPrimitiveDescription(CanMergeByText.class),
			CAS.NAME_DEFAULT_SOFA, "PickedPassages");


		/* Some debug dumps of the intermediate CAS. */
		if (logger.isDebugEnabled()) {
			builder.add(createPrimitiveDescription(
				CasDumpWriter.class,
				CasDumpWriter.PARAM_OUTPUT_FILE, "/tmp/yodaqa-racas.txt"));
		}

		return builder.createAggregateDescription();
	}
}
