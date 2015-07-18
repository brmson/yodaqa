package cz.brmlab.yodaqa.pipeline.structured;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATDBpOntology;
import cz.brmlab.yodaqa.model.Question.ClueConcept;
import cz.brmlab.yodaqa.model.TyCor.DBpOntologyLAT;
import cz.brmlab.yodaqa.provider.rdf.DBpediaOntology;
import cz.brmlab.yodaqa.provider.rdf.PropertyValue;

/* XXX: The clue-specific features, ugh. */
import cz.brmlab.yodaqa.model.Question.*;
import cz.brmlab.yodaqa.model.CandidateAnswer.*;

/**
 * From the QuestionCAS, generate a bunch of CandidateAnswerCAS
 * instances.  In this case, we generate answers from DBpedia
 * ontology relations. */

public class DBpediaOntologyPrimarySearch extends StructuredPrimarySearch {
	public DBpediaOntologyPrimarySearch() {
		super("DBpediaOntology", AF_OriginDBpONoClue.class);
		logger = LoggerFactory.getLogger(DBpediaOntologyPrimarySearch.class);
	}

	final DBpediaOntology dbo = new DBpediaOntology();

	protected List<PropertyValue> getConceptProperties(JCas questionView, ClueConcept concept) {
		List<PropertyValue> properties = new ArrayList<>();
			/* Query the DBpedia ontology dataset - cleaned up
			 * but quite sparse infobox based data. */
			properties.addAll(dbo.query(concept.getLabel(), logger));
		return properties;
	}


	protected void addTypeLAT(JCas jcas, AnswerFV fv, String type) throws AnalysisEngineProcessException {
		fv.setFeature(AF_LATDBpOntology.class, 1.0);
		addTypeLAT(jcas, fv, type, new DBpOntologyLAT(jcas));
	}

	protected void clueAnswerFeatures(AnswerFV afv, Clue clue) {
		     if (clue instanceof ClueToken     ) afv.setFeature(AF_OriginDBpOClueToken.class, 1.0);
		else if (clue instanceof CluePhrase    ) afv.setFeature(AF_OriginDBpOCluePhrase.class, 1.0);
		else if (clue instanceof ClueSV        ) afv.setFeature(AF_OriginDBpOClueSV.class, 1.0);
		else if (clue instanceof ClueNE        ) afv.setFeature(AF_OriginDBpOClueNE.class, 1.0);
		else if (clue instanceof ClueLAT       ) afv.setFeature(AF_OriginDBpOClueLAT.class, 1.0);
		else if (clue instanceof ClueSubject   ) {
			afv.setFeature(AF_OriginDBpOClueSubject.class, 1.0);
			     if (clue instanceof ClueSubjectNE) afv.setFeature(AF_OriginDBpOClueSubjectNE.class, 1.0);
			else if (clue instanceof ClueSubjectToken) afv.setFeature(AF_OriginDBpOClueSubjectToken.class, 1.0);
			else if (clue instanceof ClueSubjectPhrase) afv.setFeature(AF_OriginDBpOClueSubjectPhrase.class, 1.0);
			else assert(false);
		} else if (clue instanceof ClueConcept ) {
			afv.setFeature(AF_OriginDBpOClueConcept.class, 1.0);
			ClueConcept concept = (ClueConcept) clue;
			if (concept.getBySubject())
				afv.setFeature(AF_OriginDBpOClueSubject.class, 1.0);
			if (concept.getByLAT())
				afv.setFeature(AF_OriginDBpOClueLAT.class, 1.0);
			if (concept.getByNE())
				afv.setFeature(AF_OriginDBpOClueNE.class, 1.0);
		}
		else assert(false);
	}
}
