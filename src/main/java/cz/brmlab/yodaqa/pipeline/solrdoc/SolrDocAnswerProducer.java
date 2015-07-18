package cz.brmlab.yodaqa.pipeline.solrdoc;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.component.CasDumpWriter;
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
 *
 * In this case, this is just a thin wrapper of SolrDocPrimarySearch,
 * performing "whole-document" search and suggesting the document titles
 * as possible answers. After all, "inventor of lightbulb" might match
 * a document on "Thomas Alva Edison" (or whoever invented it!). */

public class SolrDocAnswerProducer /* XXX: extends AggregateBuilder ? */ {
	public static AnalysisEngineDescription createEngineDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		AnalysisEngineDescription primarySearch = AnalysisEngineFactory.createEngineDescription(
				SolrDocPrimarySearch.class);
		builder.add(primarySearch);

		/* A bunch of DKpro-bound NLP processors (these are
		 * the giants we stand on the shoulders of) */
		/* The mix below corresponds to what we use in
		 * Passage analysis, we just do minimal answer
		 * preprocessing expected by AnswerAnalysis. */

		/* Tokenize: */
		builder.add(AnalysisEngineFactory.createEngineDescription(LanguageToolSegmenter.class),
			CAS.NAME_DEFAULT_SOFA, "Answer");

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
		builder.add(OpenNlpNamedEntities.createEngineDescription(),
			CAS.NAME_DEFAULT_SOFA, "Answer");

		builder.setFlowControllerDescription(
				FlowControllerFactory.createFlowControllerDescription(
					FixedFlowController.class,
					FixedFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
		aed.getAnalysisEngineMetaData().setName("cz.brmlab.yodaqa.pipeline.solrdoc.SolrDocAnswerProducer");
		return aed;
	}
}
