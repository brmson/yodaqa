package cz.brmlab.yodaqa.analysis.passage.biotagger;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.NamedFeatureExtractor1;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/** Generate token feature that carries type of the dependency (dependent
 * side) or null if not covered by a dependent (dependency tree root,
 * preposition or possessive suffix, etc). */

public class DependencyTypeExtractor<T extends Annotation> implements NamedFeatureExtractor1<T> {
	@Override
	public String getFeatureName() {
		return "Dep";
	}

	@Override
	public List<Feature> extract(JCas jCas, Annotation focusAnnotation) {
		List<Feature> features = new ArrayList<>();
		for (Dependency dep : JCasUtil.selectCovering(Dependency.class, focusAnnotation)) {
			features.add(new Feature(getFeatureName(), dep.getDependencyType()));
		}
		return features;
	}
}
