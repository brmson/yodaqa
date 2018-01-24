package cz.brmlab.yodaqa.analysis.rdf;

import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.provider.rdf.DiffbotKGOntology;
import cz.brmlab.yodaqa.provider.rdf.PropertyValue;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DiffbotKGPropertySelection {
	private static final Logger logger = LoggerFactory.getLogger(DiffbotKGPropertySelection.class);
	private DiffbotKGOntology diffbo = new DiffbotKGOntology();

	public List<PropertyValue> fbpathBasedProperties(FBPathLogistic fbpathLogistic, JCas questionView, Concept concept) {
		List<PropertyValue> properties = new ArrayList<>();
		List<FBPathLogistic.PathScore> pathScs = fbpathLogistic.getPaths(fbpathLogistic.questionFeatures(questionView)).subList(0, 1);
		for(FBPathLogistic.PathScore ps: pathScs) {
			logger.debug("Diffbot path {}, {}", ps.path, ps.proba);
			if (ps.proba < 0.5) continue; // XXX: Manually selected fixed threshold
			try {
				properties.addAll(diffbo.queryFromLabel(ps, concept.getId(), concept.getCookedLabel(), logger));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return properties;
	}
}
