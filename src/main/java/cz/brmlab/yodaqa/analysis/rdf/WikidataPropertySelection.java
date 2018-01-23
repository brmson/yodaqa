package cz.brmlab.yodaqa.analysis.rdf;

import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.provider.rdf.PropertyValue;
import cz.brmlab.yodaqa.analysis.rdf.FBPathLogistic.PathScore;
import cz.brmlab.yodaqa.provider.rdf.WikidataOntology;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by honza on 11.8.16.
 */
public class WikidataPropertySelection {
	private WikidataOntology wkdo;
	private Logger logger;

	public WikidataPropertySelection() {
		logger = LoggerFactory.getLogger(WikidataPropertySelection.class);
		wkdo = new WikidataOntology();
	}

	public List<PropertyValue> pairScoringBasedProperties(JCas questionView, Concept concept) {
		List<PropertyValue> properties = wkdo.query(concept.getWikiUrl(), concept.getCookedLabel(), logger);
		List<String> labels = new ArrayList<>();
		for(PropertyValue pv: properties) {
			labels.add(pv.getProperty());
		}
		QuestionInfo qi = JCasUtil.selectSingle(questionView, QuestionInfo.class);
		String text = qi.getCAS().getDocumentText();
		List<Double> scores = KerasScoring.getScores(text, labels, 0);
		for (int i = 0; i < scores.size(); i++) {
			properties.get(i).setScore(scores.get(i));
		}
		Collections.sort(properties, new Comparator<PropertyValue>() {
			@Override
			public int compare(PropertyValue o1, PropertyValue o2) {
				return o2.getScore().compareTo(o1.getScore());
			}
		});
		for(PropertyValue pv: properties) {
			logger.debug("SORTED {} {} {}", pv.getProperty(), pv.getPropRes(), pv.getScore());
		}
		List<PropertyValue> res = new ArrayList<>();
		if (properties.size() > 0) {
			String first = properties.get(0).getProperty();
			for(PropertyValue pv: properties) {
				if (!first.equals(pv.getProperty())) break;
				res.add(pv);
			}
		}
		return res;
	}

	public List<PropertyValue> fbpathBasedProperties(FBPathLogistic fbpathLogistic, JCas questionView, Concept concept) {
		List<PropertyValue> properties = new ArrayList<>();
		List<PathScore> pathScs = fbpathLogistic.getPaths(fbpathLogistic.questionFeatures(questionView)).subList(0, 1);
		for(PathScore ps: pathScs) {
			logger.debug("WIKI path {}, {}", ps.path, ps.proba);
			if (ps.proba < 0.5) continue; // XXX: Manually selected fixed threshold
			properties.addAll(wkdo.queryFromLabel(ps, concept.getWikiUrl(), concept.getCookedLabel(), logger));
		}
		return properties;
	}
}
