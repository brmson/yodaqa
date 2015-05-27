package cz.brmlab.yodaqa.provider.crf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jcrfsuite.util.Pair;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/** A container for CRF-determined sequence of tagged items plus
 * their probabilities. */
public class CRFTagging {
	private static final Logger logger = LoggerFactory.getLogger(CRFTagging.class);

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

	public void logProb(List<Token> tokens) {
		int i = 0;
		StringBuilder sb = new StringBuilder();
		for (Pair<String, Double> pair : pairs) {
			sb.append(String.format(Locale.ENGLISH, "%s%.3f:%s ", pair.first,
						pair.second,
						tokens.get(i).getCoveredText()));
			i++;
		}
		logger.debug(sb.toString());
	}

	/* List<> delegates: */
	public int size() {
		return pairs.size();
	}
	public Pair<String, Double> get(int index) {
		return pairs.get(index);
	}
	public Iterator<Pair<String, Double>> iterator() {
		return pairs.iterator();
	}
}
