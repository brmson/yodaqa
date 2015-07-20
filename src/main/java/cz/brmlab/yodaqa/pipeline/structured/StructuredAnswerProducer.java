package cz.brmlab.yodaqa.pipeline.structured;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.component.CasDumpWriter;
import org.apache.uima.fit.component.JCasMultiplier_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.FlowControllerFactory;
import org.apache.uima.flow.impl.FixedFlowController;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.provider.OpenNlpNamedEntities;

import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;

/**
 * From the QuestionCAS, generate a bunch of CandidateAnswerCAS instances.
 * This is an abstract base class for implementing structured answer
 * generation, i.e. those where to passage processing is going on and
 * the primary search directly produces the answers and often also can
 * already guess their LATs. */

public class StructuredAnswerProducer /* XXX: extends AggregateBuilder ? */ {
	public static AnalysisEngineDescription createEngineDescription(String aggName, Class<? extends JCasMultiplier_ImplBase> primarySearchAE)
			throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		/* This generates a CAS that also carries an annotation
		 * for the unit (if specified) and ontology relation. */
		AnalysisEngineDescription primarySearch = AnalysisEngineFactory.createEngineDescription(
				primarySearchAE);
		builder.add(primarySearch);

		/* A bunch of DKpro-bound NLP processors (these are
		 * the giants we stand on the shoulders of) */
		/* The mix below corresponds to what we use in
		 * Passage analysis, we just do minimal answer
		 * preprocessing expected by AnswerAnalysis. */

		/* Tokenize: */
		builder.add(AnalysisEngineFactory.createEngineDescription(LanguageToolSegmenter.class),
			CAS.NAME_DEFAULT_SOFA, "Answer");

		/* Note that from now on, we should actually typically
		 * do a better job with something more specialized
		 * here (esp. wrt. named entities). */

		/* POS, constituents, dependencies: */
		builder.add(AnalysisEngineFactory.createEngineDescription(
				StanfordParser.class,
				StanfordParser.PARAM_MAX_TOKENS, 50, // more takes a lot of RAM and is sloow, StanfordParser is O(N^2)
				StanfordParser.PARAM_WRITE_POS, true),
			CAS.NAME_DEFAULT_SOFA, "Answer");

		/* Lemma features: */
		builder.add(AnalysisEngineFactory.createEngineDescription(LanguageToolLemmatizer.class),
			CAS.NAME_DEFAULT_SOFA, "Answer");

		/* Named Entities: */
		/* XXX: Do we really want to do this? */
		builder.add(OpenNlpNamedEntities.createEngineDescription(),
			CAS.NAME_DEFAULT_SOFA, "Answer");

		/* TODO: Generate LATs. */

		builder.setFlowControllerDescription(
				FlowControllerFactory.createFlowControllerDescription(
					FixedFlowController.class,
					FixedFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
		aed.getAnalysisEngineMetaData().setName(aggName);
		return aed;
	}
}

