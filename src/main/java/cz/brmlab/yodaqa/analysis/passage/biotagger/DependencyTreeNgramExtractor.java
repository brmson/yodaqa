package cz.brmlab.yodaqa.analysis.passage.biotagger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.NamedFeatureExtractor1;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/** A FeatureExtractor wrapper to extract a *tree-based* dependency type
 * Ngram feature. We cannot use the basic n-gram infrastructure
 * of cleartk as in case of children, we want to return multiple
 * n-gram features for each possible path in the tree (or some
 * aggregation across these paths).
 *
 * XXX: This looks like generic Annotation-type extractor, but in fact
 * the annotations must be Tokens. */

public class DependencyTreeNgramExtractor {
	static List<Annotation> annAtOffset(JCas jcas, Annotation ann, int offset) {
		List<Annotation> anns = new ArrayList<>();
		if (offset == 0) {
			return Arrays.asList(ann);
		} else if (offset < 0) {
			/* XXX: This is a really wide select, which we do in
			 * a recursive loop, many times! */
			for (Dependency dep : JCasUtil.select(jcas, Dependency.class)) {
				if (dep.getDependent() != ann)
					continue;
				anns.addAll(annAtOffset(jcas, dep.getGovernor(), offset+1));
			}
		} else if (offset > 0) {
			for (Dependency dep : JCasUtil.select(jcas, Dependency.class)) {
				if (dep.getGovernor() != ann)
					continue;
				anns.addAll(annAtOffset(jcas, dep.getDependent(), offset-1));
			}
		}
		return anns;
	}

	static String fNameOfs(String name, int offset) {
		return name + "[" + Integer.toString(offset) + "]";
	}

	static List<Feature> extractNgram(JCas jcas, Annotation focusAnnotation, int[] offsets, NamedFeatureExtractor1 extractor)
			throws CleartkExtractorException {
		List<Feature> inFeatures = null;
		if (offsets.length > 1) {
			// XXX: we do some needless double-walking here
			inFeatures = extractNgram(jcas, focusAnnotation, Arrays.copyOfRange(offsets, 1, offsets.length), extractor);
		}

		int offset = offsets[0];
		List<Feature> outFeatures = new ArrayList<>();
		Collection<Annotation> hereAnns = annAtOffset(jcas, focusAnnotation, offset);
		if (hereAnns.isEmpty()) {
			if (inFeatures == null) {
				outFeatures.add(new Feature(fNameOfs(extractor.getFeatureName(), offset), "OOB"));
			} else for (Feature fIn : inFeatures) {
				outFeatures.add(new Feature(fNameOfs(extractor.getFeatureName(), offset) + "_" + fIn.getName(), "OOB" + "_" + fIn.getValue()));
			}
		} else for (Annotation depAnn : hereAnns) {
			Collection<Feature> hereFeatures = extractor.extract(jcas, depAnn);
			if (hereFeatures.isEmpty()) {
				if (inFeatures == null) {
					outFeatures.add(new Feature(fNameOfs(extractor.getFeatureName(), offset), "X"));
				} else for (Feature fIn : inFeatures) {
					outFeatures.add(new Feature(fNameOfs(extractor.getFeatureName(), offset) + "_" + fIn.getName(), "X" + "_" + fIn.getValue()));
				}
			} else for (Feature f : hereFeatures) {
				/* XXX: For each hereFeature, we create a separate
				 * feature chain here; maybe we should rather paste
				 * them together like cleartk does? */
				if (inFeatures == null) {
					outFeatures.add(new Feature(fNameOfs(f.getName(), offset), f.getValue()));
				} else for (Feature fIn : inFeatures) {
					outFeatures.add(new Feature(fNameOfs(f.getName(), offset) + "_" + fIn.getName(), f.getValue() + "_" + fIn.getValue()));
				}
			}
		}
		return outFeatures;
	}
}
