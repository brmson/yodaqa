package cz.brmlab.yodaqa.pipeline.structured;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ResourceInitializationException;


/**
 * From the QuestionCAS, generate a bunch of CandidateAnswerCAS instances.
 *
 * In this case, this is just a thin wrapper of DBpediaOntologyPrimarySearch,
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

public class DBpediaOntologyAnswerProducer extends StructuredAnswerProducer {
	public static AnalysisEngineDescription createEngineDescription()
			throws ResourceInitializationException {
		return createEngineDescription("cz.brmlab.yodaqa.pipeline.structured.DBpediaOntologyAnswerProducer",
				DBpediaOntologyPrimarySearch.class);
	}
}
