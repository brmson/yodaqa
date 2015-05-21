package cz.brmlab.yodaqa.provider.crf;

import java.util.ArrayList;
import java.util.List;

import com.github.jcrfsuite.util.Pair;

/** A container for CRF-determined sequence of tagged items plus
 * their probabilities. */
public class CRFTagging {
	public List<Pair<String, Double>> pairs;

	public CRFTagging(List<Pair<String, Double>> pairs) {
		this.pairs = pairs;
	}

	public List<String> getOutcomes() {
		List<String> outcomes = new ArrayList<String>(pairs.size());
		for (Pair<String, Double> pair : pairs)
			outcomes.add(pair.first);
		return outcomes;
	}
}
