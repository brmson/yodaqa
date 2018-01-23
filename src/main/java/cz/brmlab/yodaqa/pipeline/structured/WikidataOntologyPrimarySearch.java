package cz.brmlab.yodaqa.pipeline.structured;

import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.rdf.FBPathLogistic;
import cz.brmlab.yodaqa.analysis.rdf.FBPathLogistic.PathScore;
import cz.brmlab.yodaqa.analysis.rdf.KerasScoring;
import cz.brmlab.yodaqa.analysis.rdf.WikidataPropertySelection;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceStructured;
import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.TyCor.WikidataOntologyLAT;
import cz.brmlab.yodaqa.provider.rdf.PropertyValue;
import cz.brmlab.yodaqa.provider.rdf.WikidataOntology;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikidataOntologyPrimarySearch extends StructuredPrimarySearch {
	public WikidataOntologyPrimarySearch() {
		// FIXME Wikidata specific features
		super("WikidataOntology", AF.OriginFBO_ClueType, AF.OriginFBONoClue);
		logger = LoggerFactory.getLogger(WikidataOntologyPrimarySearch.class);
	}

	final WikidataOntology wdo = new WikidataOntology();

	protected static FBPathLogistic fbpathLogistic = null;
	private static WikidataPropertySelection wikiprop = new WikidataPropertySelection();

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
//		List<PropertyValue> properties = wikiprop.pairScoringBasedProperties(questionView, concept);
		List<PropertyValue> properties = wikiprop.fbpathBasedProperties(fbpathLogistic, questionView, concept);
//		List<PropertyValue> properties = wdo.query(concept.getWikiUrl(), concept.getCookedLabel(), logger);
		for (PropertyValue pv: properties) {
			pv.setValue(normalizeDate(pv.getValue()));
		}
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

	private String normalizeDate (String text) {
		HashMap<String, String> months = new HashMap<>();
		months.put("01", "ledna"); months.put("02", "února"); months.put("03", "března");
		months.put("04", "dubna"); months.put("05", "května"); months.put("06", "června");
		months.put("07", "července"); months.put("08", "srpna"); months.put("09", "září");
		months.put("10", "října"); months.put("11", "listopadu"); months.put("12", "prosince");
		Pattern pattern = Pattern.compile("(\\d{4,4})-(\\d{2,2})-(\\d{2,2})T00:00:00Z");
		Matcher m = pattern.matcher(text);
		if (m.find()) {
			String day = Integer.valueOf(m.group(3)).toString();
			String month = months.get(m.group(2));
			String year = Integer.valueOf(m.group(1)).toString();
			return day + ". " + month + " " + year;
		}
		return text;
	}
}
