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
import cz.brmlab.yodaqa.model.TyCor.DBpPropertyLAT;
import cz.brmlab.yodaqa.provider.rdf.DBpediaProperties;
import cz.brmlab.yodaqa.provider.rdf.PropertyValue;

/**
 * From the QuestionCAS, generate a bunch of CandidateAnswerCAS
 * instances.  In this case, we generate answers from DBpedia
 * ontology relations.
 *
 * This dataset really is awful:
 *
 * (i) It actually is not complete; e.g. the <Sun> resource is missing
 * most of the infobox stuff.  Also, many values like dates are stripped
 * to just day-of-month number or such.
 *
 * (ii) It contains too much junk that is not relevant as it is not
 * presented to the user.  For example image alts, alignments and links.
 * Various properties like "date" for city are actually not pertaining to
 * the entity but just some part of the infobox that describes e.g. the
 * leader, and such segmentation is lost.
 *
 * A useful raw infobox dataset needs to carry labels *as they are shown
 * to the user*, and needs to actually render the infobox template
 * internally to determine any grouping and representation of data! */

public class DBpediaPropertyPrimarySearch extends StructuredPrimarySearch {
	public DBpediaPropertyPrimarySearch() {
		super("DBpedia Property", AF.OriginDBpP_ClueType, AF.OriginDBpPNoClue);
		logger = LoggerFactory.getLogger(DBpediaPropertyPrimarySearch.class);
	}

	final DBpediaProperties dbp = new DBpediaProperties();

	protected List<PropertyValue> getConceptProperties(JCas questionView, Concept concept) {
		List<PropertyValue> properties = new ArrayList<>();
		/* Query the DBpedia raw infobox dataset - uncleaned
		 * but depnse infobox based data. */
		/* TODO: Fetch by pageID. */
		properties.addAll(dbp.query(concept.getCookedLabel(), logger));
		return properties;
	}

	protected AnswerSourceStructured makeAnswerSource(PropertyValue property) {
		return new AnswerSourceStructured(AnswerSourceStructured.TYPE_DBPEDIA,
				property.getOrigin(), property.getObjRes(), property.getObject());
	}

	protected void addTypeLAT(JCas jcas, AnswerFV fv, String type) throws AnalysisEngineProcessException {
		fv.setFeature(AF.LATDBpProperty, 1.0);
		addTypeLAT(jcas, fv, type, new DBpPropertyLAT(jcas));
	}
}
