package cz.brmlab.yodaqa.pipeline.structured;

import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceStructured;
import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.model.TyCor.WikidataOntologyLAT;
import cz.brmlab.yodaqa.provider.rdf.PropertyValue;
import cz.brmlab.yodaqa.provider.rdf.WikidataOntology;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WikidataOntologyPrimarySearch extends StructuredPrimarySearch {
	public WikidataOntologyPrimarySearch() {
		// FIXME Wikidata specific features
		super("WikidataOntology", AF.OriginFBO_ClueType, AF.OriginFBONoClue);
		logger = LoggerFactory.getLogger(WikidataOntologyPrimarySearch.class);
	}

	final WikidataOntology wdo = new WikidataOntology();

	@Override
	protected List<PropertyValue> getConceptProperties(JCas questionView, Concept concept) {
		List<PropertyValue> properties = new ArrayList<>();
		properties.addAll(wdo.query(concept.getCookedLabel(), logger));
		return properties;
	}

	@Override
	protected AnswerSourceStructured makeAnswerSource(PropertyValue property) {
		return new AnswerSourceStructured(AnswerSourceStructured.TYPE_WIKIDATA,
				property.getOrigin(), property.getObjRes(), property.getObject());
	}

	@Override
	protected void addTypeLAT(JCas jcas, AnswerFV fv, String type) throws AnalysisEngineProcessException {
		// FIXME Wikidata specific features
		fv.setFeature(AF.LATFBOntology, 1.0);
		addTypeLAT(jcas, fv, type, new WikidataOntologyLAT(jcas));
	}
}
