package cz.brmlab.yodaqa.pipeline.dbpedia;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATDBpRelation;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginDBpRelation;
import cz.brmlab.yodaqa.model.Question.ClueConcept;
import cz.brmlab.yodaqa.model.TyCor.DBpRelationLAT;
import cz.brmlab.yodaqa.pipeline.StructuredPrimarySearch;
import cz.brmlab.yodaqa.provider.rdf.DBpediaOntology;
import cz.brmlab.yodaqa.provider.rdf.DBpediaProperties;
import cz.brmlab.yodaqa.provider.rdf.PropertyValue;

/* XXX: The clue-specific features, ugh. */
import cz.brmlab.yodaqa.model.Question.*;
import cz.brmlab.yodaqa.model.CandidateAnswer.*;

/**
 * From the QuestionCAS, generate a bunch of CandidateAnswerCAS
 * instances.  In this case, we generate answers from DBpedia
 * ontology relations. */

public class DBpediaRelationPrimarySearch extends StructuredPrimarySearch {
	public DBpediaRelationPrimarySearch() {
		super("DBpedia", AF_OriginDBpRelation.class, AF_OriginDBpRNoClue.class);
		logger = LoggerFactory.getLogger(DBpediaRelationPrimarySearch.class);
	}

	final DBpediaOntology dbo = new DBpediaOntology();
	final DBpediaProperties dbp = new DBpediaProperties();

	protected List<PropertyValue> getConceptProperties(ClueConcept concept) {
		List<PropertyValue> properties = new ArrayList<>();
			/* Query the DBpedia ontology dataset - cleaned up
			 * but quite sparse infobox based data. */
			properties.addAll(dbo.query(concept.getLabel(), logger));
			/* Query the DBpedia raw infobox dataset - uncleaned
			 * but depnse infobox based data. */
			/* We disable this again for the time being because
			 * this dataset really is awful:
			 *
			 * (i) It actually is not complete; e.g. the <Sun>
			 * resource is missing most of the infobox stuff.
			 * Also, many values like dates are stripped to just
			 * day-of-month number or such.
			 *
			 * (ii) It contains too much junk that is not relevant
			 * as it is not presented to the user.  For example
			 * image alts, alignments and links.  Various
			 * properties like "date" for city are actually not
			 * pertaining to the entity but just some part of the
			 * infobox that describes e.g. the leader, and such
			 * segmentation is lost.
			 *
			 * A useful raw infobox dataset needs to carry labels
			 * *as they are shown to the user*, and needs to
			 * actually render the infobox template internally to
			 * determine any grouping and representation of data! */
			// properties.addAll(dbp.query(concept.getLabel(), logger));
		return properties;
	}


	protected void addTypeLAT(JCas jcas, AnswerFV fv, String type) throws AnalysisEngineProcessException {
		fv.setFeature(AF_LATDBpRelation.class, 1.0);
		addTypeLAT(jcas, fv, type, new DBpRelationLAT(jcas));
	}

	protected void clueAnswerFeatures(AnswerFV afv, Clue clue) {
		     if (clue instanceof ClueToken     ) afv.setFeature(AF_OriginDBpRClueToken.class, 1.0);
		else if (clue instanceof CluePhrase    ) afv.setFeature(AF_OriginDBpRCluePhrase.class, 1.0);
		else if (clue instanceof ClueSV        ) afv.setFeature(AF_OriginDBpRClueSV.class, 1.0);
		else if (clue instanceof ClueNE        ) afv.setFeature(AF_OriginDBpRClueNE.class, 1.0);
		else if (clue instanceof ClueLAT       ) afv.setFeature(AF_OriginDBpRClueLAT.class, 1.0);
		else if (clue instanceof ClueSubject   ) {
			afv.setFeature(AF_OriginDBpRClueSubject.class, 1.0);
			     if (clue instanceof ClueSubjectNE) afv.setFeature(AF_OriginDBpRClueSubjectNE.class, 1.0);
			else if (clue instanceof ClueSubjectToken) afv.setFeature(AF_OriginDBpRClueSubjectToken.class, 1.0);
			else if (clue instanceof ClueSubjectPhrase) afv.setFeature(AF_OriginDBpRClueSubjectPhrase.class, 1.0);
			else assert(false);
		} else if (clue instanceof ClueConcept ) {
			afv.setFeature(AF_OriginDBpRClueConcept.class, 1.0);
			ClueConcept concept = (ClueConcept) clue;
			if (concept.getBySubject())
				afv.setFeature(AF_OriginDBpRClueSubject.class, 1.0);
			if (concept.getByLAT())
				afv.setFeature(AF_OriginDBpRClueLAT.class, 1.0);
			if (concept.getByNE())
				afv.setFeature(AF_OriginDBpRClueNE.class, 1.0);
		}
		else assert(false);
	}
}
