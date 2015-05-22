package cz.brmlab.yodaqa.provider.crf;

import java.util.ArrayList;
import java.util.Arrays;
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
	protected boolean[] forced;

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
			sb.append(String.format(Locale.ENGLISH, "%s%s%.3f:%s ", pair.first,
						(forced != null && forced[i] ? "F" : ""),
						pair.second,
						tokens.get(i).getCoveredText()));
			i++;
		}
		logger.debug(sb.toString());
	}

	/** Generate extra B/I outcomes from low-probability O outcomes.
	 * To distinguish low-probability O outcomes, we use MAD*mmSpread
	 * to be outlier resistant.
	 *
	 * Given M = median(p), MAD = median(|p-median(p)|), we select
	 * outcomes by a log-formula, and report their
	 * score = p * scoreRescale.  This method was proposed by Jacana
	 * (Yao and van Durme, 2013). */
	public void forceByMedian(double mmSpread, double scoreRescale) {
		if (size() <= 3)
			return;

		/* Compute log-median */
		double[] scores = new double[size()];
		for (int i = 0; i < size(); i++)
			scores[i] = Math.log(get(i).second);
		Arrays.sort(scores);
		double M = median(scores);

		/* Compute median absolute log-deviation */
		double[] ad = new double[size()];
		for (int i = 0; i < size(); i++)
			ad[i] = Math.abs(Math.log(get(i).second) - M);
		Arrays.sort(ad);
		double MAD = median(ad);

		/* XXX: In the above, we ignore labelling, even though
		 * strictly speaking we should use just O-label probs.
		 * I'm kind of lazy though, and non-Os will be just
		 * outliers anyway. */

		/* Determine which outcomes should be forced
		 * and re-tag */
		double p_thres = Math.exp(M - MAD * mmSpread);
		forced = new boolean[size()];
		logger.debug("M "+M+" "+Math.exp(M)+" MAD "+MAD+" "+Math.exp(-MAD)+" pt "+p_thres);
		for (int i = 0; i < size(); i++) {
			if (get(i).first.equals("O") && get(i).second <= p_thres) {
				forced[i] = true;
				if (i > 0 && forced[i-1]) {
					// we just assume that a sequence
					// of outliers is a single answer
					get(i).first = "I";
				} else {
					get(i).first = "B";
				}
				get(i).second *= scoreRescale;
			} else {
				forced[i] = false;
			}
			//logger.debug("["+i+"] "+get(i).first+" "+get(i).second+" "+forced[i]);
		}
	}

	static private final double median(double[] seq) {
		return seq.length % 2 == 1 ? seq[seq.length / 2] : (seq[seq.length/2-1] + seq[seq.length/2]) / 2;
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

	public boolean isForced(int index) {
		return forced != null && forced[index];
	}
}
