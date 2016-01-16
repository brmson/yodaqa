package cz.brmlab.yodaqa.analysis.question;

import cz.brmlab.yodaqa.model.Question.SV;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.WordnetLAT;
import cz.brmlab.yodaqa.provider.glove.MbWeights;
import cz.brmlab.yodaqa.provider.glove.Relatedness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

/**
 * Counts probability of concept being relevant to the given question.
 * More info can be found at https://github.com/brmson/Sentence-selection,
 * we use the "property" mode.
 *
 * XXX: Method duplication with PropertyGloVeScoring.
 */
public class ConceptGloVeScoring {

	private static ConceptGloVeScoring cgs = new ConceptGloVeScoring();

	public static ConceptGloVeScoring getInstance() {
		return cgs;
	}

	private Relatedness r = new Relatedness(new MbWeights(ConceptGloVeScoring.class.getResourceAsStream("Mbdesc.txt")));

	public double relatedness(List<String> qtoks, List<String> dtoks) {
		double res = r.probability(qtoks, dtoks);
		return res;
	}

	/** For legacy reasons, we use our own tokenization.
	 * We also lower-case while at it, and might do some other
	 * normalization steps...
	 * XXX: Rely on pipeline instead? */
	public static List<String> tokenize(String str) {
		return new ArrayList<>(Arrays.asList(str.toLowerCase().split("[\\p{Punct}\\s]+")));
	}

	/** Generate bag-of-words representation for the question.
	 * We may not include *all* words in this representation
	 * and use a more sophisticated strategy than tokenize(). */
	public static List<String> questionRepr(JCas questionView) {
		List<String> tokens = new ArrayList<>();
		for (LAT lat : JCasUtil.select(questionView, LAT.class)) {
			if (lat instanceof WordnetLAT)
				continue; // junk
			tokens.add(lat.getText());
		}
		for (SV sv : JCasUtil.select(questionView, SV.class)) {
			tokens.add(sv.getCoveredText());
		}
		return tokens;
	}
}
