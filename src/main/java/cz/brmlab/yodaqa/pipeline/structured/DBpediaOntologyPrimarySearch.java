package cz.brmlab.yodaqa.pipeline.structured;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceStructured;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.model.TyCor.DBpOntologyLAT;
import cz.brmlab.yodaqa.provider.rdf.DBpediaOntology;
import cz.brmlab.yodaqa.provider.rdf.PropertyValue;

/**
 * From the QuestionCAS, generate a bunch of CandidateAnswerCAS
 * instances.  In this case, we generate answers from DBpedia
 * ontology relations. */

public class DBpediaOntologyPrimarySearch extends StructuredPrimarySearch {
	public DBpediaOntologyPrimarySearch() {
		super("DBpediaOntology", AF.OriginDBpO_ClueType, AF.OriginDBpONoClue);
		logger = LoggerFactory.getLogger(DBpediaOntologyPrimarySearch.class);
	}

	final DBpediaOntology dbo = new DBpediaOntology();

	protected List<PropertyValue> getConceptProperties(JCas questionView, Concept concept) {
		List<PropertyValue> properties = new ArrayList<>();
		/* Query the DBpedia ontology dataset - cleaned up
		 * but quite sparse infobox based data. */
		/* TODO: Fetch by pageID. */
		properties.addAll(dbo.query(concept.getCookedLabel(), logger));
		return properties;
	}

	protected AnswerSourceStructured makeAnswerSource(PropertyValue property) {
		return new AnswerSourceStructured(AnswerSourceStructured.TYPE_DBPEDIA,
				property.getOrigin(), property.getObjRes(), property.getObject());
	}

	protected void addTypeLAT(JCas jcas, AnswerFV fv, String type) throws AnalysisEngineProcessException {
		fv.setFeature(AF.LATDBpOntology, 1.0);
		addTypeLAT(jcas, fv, type, new DBpOntologyLAT(jcas));
	}
}
