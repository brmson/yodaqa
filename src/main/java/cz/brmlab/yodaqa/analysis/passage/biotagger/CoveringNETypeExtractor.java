package cz.brmlab.yodaqa.analysis.passage.biotagger;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.NamedFeatureExtractor1;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;

/** Generate token feature that carries type of a covering NamedEntity
 * or null if not part of a named entity. */

public class CoveringNETypeExtractor<T extends Annotation> implements NamedFeatureExtractor1<T> {
	@Override
	public String getFeatureName() {
		return "NE";
	}

	@Override
	public List<Feature> extract(JCas jCas, Annotation focusAnnotation) {
		List<Feature> features = new ArrayList<>();
		for (NamedEntity ne : JCasUtil.selectCovering(NamedEntity.class, focusAnnotation)) {
			features.add(new Feature(getFeatureName(), ne.getValue()));
		}
		return features;
	}
}
