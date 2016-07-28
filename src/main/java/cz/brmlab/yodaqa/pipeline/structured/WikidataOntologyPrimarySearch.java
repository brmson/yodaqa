package cz.brmlab.yodaqa.pipeline.structured;

import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.rdf.FBPathLogistic;
import cz.brmlab.yodaqa.analysis.rdf.FBPathLogistic.PathScore;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceStructured;
import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.model.TyCor.WikidataOntologyLAT;
import cz.brmlab.yodaqa.provider.rdf.PropertyValue;
import cz.brmlab.yodaqa.provider.rdf.WikidataOntology;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WikidataOntologyPrimarySearch extends StructuredPrimarySearch {
	public WikidataOntologyPrimarySearch() {
		// FIXME Wikidata specific features
		super("WikidataOntology", AF.OriginFBO_ClueType, AF.OriginFBONoClue);
		logger = LoggerFactory.getLogger(WikidataOntologyPrimarySearch.class);
	}

	protected static FBPathLogistic fbpathLogistic = null;
	final WikidataOntology wkdo = new WikidataOntology();

	@Override
	public synchronized void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		if (fbpathLogistic == null) {
			fbpathLogistic = new FBPathLogistic();
			fbpathLogistic.initialize();
		}
	}

	@Override
	protected List<PropertyValue> getConceptProperties(JCas questionView, Concept concept) {
		List<PropertyValue> properties = new ArrayList<>();
		List<PathScore> pathScs = fbpathLogistic.getPaths(fbpathLogistic.questionFeatures(questionView)).subList(0, 2);
		for(PathScore ps: pathScs) {
			logger.debug("WIKI path {}, {}", ps.path, ps.proba);
			if (ps.proba < 0.2) continue; // XXX: Manually selected fixed threshold
			properties.addAll(wkdo.queryFromLabel(ps, concept.getCookedLabel(), logger));
		}
//		properties.addAll(wkdo.query(concept.getCookedLabel(), logger));
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
