package cz.brmlab.yodaqa.analysis.passextract;

import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

/**
 * Extract most interesting passages from SearchResultCAS
 * for further analysis in the PickedPassages view.
 *
 * This is an aggregate AE that will run a variety of annotators on the
 * SearchResultCAS, focusing it on just a few most interesting passages. */

public class PassageExtractorAE /* XXX: extends AggregateBuilder ? */ {
	final static Logger logger = LoggerFactory.getLogger(PassageExtractorAE.class);

	public static AnalysisEngineDescription createEngineDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		/* Token features: */
		// LanguageToolsSegmenter is the only one capable of dealing
		// with incomplete sentences e.g. separated by paragraphs etc.
		builder.add(createPrimitiveDescription(LanguageToolSegmenter.class),
			CAS.NAME_DEFAULT_SOFA, "Result");

		/* At this point, we can filter the source to keep
		 * only sentences and tokens we care about: */
		builder.add(createPrimitiveDescription(PassSetup.class));
		builder.add(createPrimitiveDescription(PassByClue.class));

		/* Further cut these only to the most interesting N sentences. */
		builder.add(createPrimitiveDescription(PassFilter.class));

		return builder.createAggregateDescription();
	}
}
