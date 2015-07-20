package cz.brmlab.yodaqa.analysis.passextract;

import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.flow.asb.ParallelEngineFactory;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

/**
 * Extract most interesting passages from SearchResultCAS
 * for further analysis in the PickedPassages view.
 *
 * This is an aggregate AE that will run a variety of annotators on the
 * SearchResultCAS, focusing it on just a few most interesting passages. */

public class PassageExtractorAE /* XXX: extends AggregateBuilder ? */ {
	final static Logger logger = LoggerFactory.getLogger(PassageExtractorAE.class);

	/* passSelection parameter values */
	public static final int PARAM_PASS_SEL_BYCLUE = 0;
	public static final int PARAM_PASS_SEL_FIRST = 1;

	public static AnalysisEngineDescription createEngineDescription(int passSelection)
			throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		/* Token features: */
		// LanguageToolsSegmenter is the only one capable of dealing
		// with incomplete sentences e.g. separated by paragraphs etc.
		// However, StanfordSegmenter handles numerical quantities
		// (like 10,900) much better.
		builder.add(createPrimitiveDescription(StanfordSegmenter.class),
			CAS.NAME_DEFAULT_SOFA, "Result");

		/* At this point, we can filter the source to keep
		 * only sentences and tokens we care about: */
		builder.add(createPrimitiveDescription(PassSetup.class));
		switch (passSelection) {

		case PARAM_PASS_SEL_BYCLUE:
			builder.add(createPrimitiveDescription(PassByClue.class));
			builder.add(createPrimitiveDescription(PassScoreSimple.class),
				CAS.NAME_DEFAULT_SOFA, "Passages");
			break;

		case PARAM_PASS_SEL_FIRST:
			builder.add(createPrimitiveDescription(PassFirst.class));
			break;
		}

		/* Finally cut these only to the most interesting N sentences
		 * and copy these over to new view PickedPassages. */
		builder.add(createPrimitiveDescription(PassFilter.class));

		if (passSelection == PARAM_PASS_SEL_BYCLUE)
			builder.add(createPrimitiveDescription(PassGSHook.class,
						ParallelEngineFactory.PARAM_NO_MULTIPROCESSING, 1));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().setName("cz.brmlab.yodaqa.analysis.passextract.PassageExtractorAE");
		return aed;
	}
}
