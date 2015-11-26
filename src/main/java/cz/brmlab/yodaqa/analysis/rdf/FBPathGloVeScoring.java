package cz.brmlab.yodaqa.analysis.rdf;

import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;
import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.model.Question.SV;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.WordnetLAT;
import cz.brmlab.yodaqa.provider.glove.MbWeights;
import cz.brmlab.yodaqa.provider.glove.Relatedness;
import cz.brmlab.yodaqa.provider.rdf.FreebaseOntology;
import cz.brmlab.yodaqa.provider.rdf.PropertyPath;
import cz.brmlab.yodaqa.provider.rdf.PropertyValue;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by honza on 25.11.15.
 */
public class FBPathGloVeScoring {

	public class PathScore {
		public PropertyPath path;
		public double proba;

		public PathScore(PropertyPath path, double proba) {
			this.path = path;
			this.proba = proba;
		}
	}

	private static FBPathGloVeScoring fbpgs = new FBPathGloVeScoring();
	protected Logger logger = LoggerFactory.getLogger(FBPathGloVeScoring.class);
	private static FreebaseOntology fbo = new FreebaseOntology();

	public static FBPathGloVeScoring getInstance() {
		return fbpgs;
	}

	private Relatedness r = new Relatedness(new MbWeights(FBPathGloVeScoring.class.getResourceAsStream("Mbrel1.txt")));

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

	public List<PathScore> getPaths(JCas questionView) {
		List<PathScore> scores = new ArrayList<>();
		Set<PropertyValue> set = new TreeSet<>(new Comparator<PropertyValue>() {
			@Override
			public int compare(PropertyValue o1, PropertyValue o2) {
				if(o1.getPropRes().equalsIgnoreCase(o2.getPropRes())){
					return 0;
				}
				return 1;
			}
		});
		for(Concept c:  JCasUtil.select(questionView, Concept.class)) {
			//test number of returned relations, note the query filters!
			List<PropertyValue> list = fbo.queryAllRelations(c.getPageID(), logger);
			for(PropertyValue pv: list) {
				List<String> qtoks = PropertyGloVeScoring.questionRepr(questionView);
				List<String> proptoks = PropertyGloVeScoring.tokenize(pv.getProperty());
				pv.setScore(r.probability(qtoks, proptoks));
				set.add(pv);
			}
		}
		for (PropertyValue pv: set) {
			List<String> properties = new ArrayList<>();
			properties.add(pv.getPropRes());
			PropertyPath path = new PropertyPath(properties);
			scores.add(new PathScore(path, pv.getScore()));
		}
		Collections.sort(scores, new Comparator<PathScore>(){
			@Override
			public int compare(PathScore ps1, PathScore ps2){
				return Double.valueOf(ps2.proba).compareTo(Double.valueOf(ps1.proba));
			}
		});
		return scores;
	}
}
