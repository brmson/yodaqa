package cz.brmlab.yodaqa.pipeline.dbpedia;

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
 * In this case, this is just a thin wrapper of DBpediaRelationPrimarySearch,
 * producing infobox-extracted dbpedia:ontology relationships of ClueSubject
 * entities as answers.
 *
 * E.g. if subject is "Abraham Lincoln",
 * 	<http://dbpedia.org/resource/Abraham_Lincoln> <http://dbpedia.org/ontology/birthDate> "1809-02-12"^^<http://www.w3.org/2001/XMLSchema#date> .
 * shall produce answer 1809-09-12 with LATs "birth date" and "date".
 *
 * 	<http://dbpedia.org/resource/Alabama> <http://dbpedia.org/ontology/largestCity> <http://dbpedia.org/resource/Birmingham,_Alabama> .
 * shall produce answer "Birmingham, Alabama" with LATs "largest city" and "city".
 *
 * 	<http://dbpedia.org/resource/Anna_Kournikova> <http://dbpedia.org/ontology/Person/height> "173.0"^^<http://dbpedia.org/datatype/centimetre> .
 * shall produce answer "173.0 centimetre" with LAT "height", and
 *
 * 	<http://dbpedia.org/resource/Azincourt> <http://dbpedia.org/ontology/PopulatedPlace/area> "8.46"^^<http://dbpedia.org/datatype/squareKilometre> .
 * shall produce answer "8.46 squared kilmetre" with LAT "area".
 */

public class DBpediaRelationAnswerProducer /* XXX: extends AggregateBuilder ? */ {
	public static AnalysisEngineDescription createEngineDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		/* This generates a CAS that also carries an annotation
		 * for the unit (if specified) and ontology relation. */
		AnalysisEngineDescription primarySearch = AnalysisEngineFactory.createEngineDescription(
				DBpediaRelationPrimarySearch.class);
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
		return aed;
	}
}
