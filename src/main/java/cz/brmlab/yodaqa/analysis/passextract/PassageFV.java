package cz.brmlab.yodaqa.analysis.passextract;

import java.util.ArrayList;

import org.apache.uima.cas.FeatureStructure;

import cz.brmlab.yodaqa.model.SearchResult.PF_ClueMatch;
import cz.brmlab.yodaqa.model.SearchResult.PF_ClueWeight;
import cz.brmlab.yodaqa.model.SearchResult.PF_AboutClueMatch;
import cz.brmlab.yodaqa.model.SearchResult.PF_AboutClueWeight;
import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.model.SearchResult.PassageFeature;

/**
 * A Passage feature vector.  I.e. a single, fixed array where each index
 * represents one feature and the indices are stable between passages.
 *
 * XXX: Make this much more similar to AnswerFV; possibly with a common
 * base class too. */


public class PassageFV {
	public static ArrayList<Class<? extends PassageFeature>> features;
	public static String labels[] = {
		"clueMatch", "clueWeight",
		"aboutClueMatch", "aboutClueWeight",
	};

	public double values[];

	public PassageFV(Passage passage) {
		/* This is a huge mess - seems like static initializer of
		 * the array list is impossible to do. */
		if (features == null) {
			features = new ArrayList<Class<? extends PassageFeature>>();
			features.add(PF_ClueMatch.class);
			features.add(PF_ClueWeight.class);
			features.add(PF_AboutClueMatch.class);
			features.add(PF_AboutClueWeight.class);
		}

		values = new double[labels.length];

		for (FeatureStructure fs : passage.getFeatures().toArray()) {
			PassageFeature pf = (PassageFeature) fs;
			int index = features.indexOf(pf.getClass());
			assert(index >= 0);
			// System.err.println(index + " <- " + pf.getValue());
			values[index] += pf.getValue();
		}
	}

	public double[] getValues() {
		return values;
	}

	public static ArrayList<Class<? extends PassageFeature>> getFeatures() {
		return features;
	}

	public static int featureIndex(Class<? extends PassageFeature> f) {
		return features.indexOf(f);
	}
}
