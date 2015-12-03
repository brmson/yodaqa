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

	public class PathScore extends FBPathLogistic {
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

	private Relatedness r1 = new Relatedness(new MbWeights(FBPathGloVeScoring.class.getResourceAsStream("Mbrel1.txt")));
	private Relatedness r2 = new Relatedness(new MbWeights(FBPathGloVeScoring.class.getResourceAsStream("Mbrel2.txt")));

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
		Set<List<PropertyValue>> set = new TreeSet<>(new Comparator<List<PropertyValue>>() {
			@Override
			public int compare(List<PropertyValue> o1, List<PropertyValue> o2) {
				if (o1.size() != o2.size()) return 1;
				for (int i = 0; i < o1.size(); i++) {
//					logger.info("COMPARE " + o1.get(i).getPropRes() + " " + o2.get(i).getPropRes() + " " + o1.get(i).getPropRes().equalsIgnoreCase(o2.get(i).getPropRes()));
					if (!o1.get(i).getPropRes().equalsIgnoreCase(o2.get(i).getPropRes())) return 1;
				}
				return 0;
			}
		});
		for(Concept c:  JCasUtil.select(questionView, Concept.class)) {
			List<PropertyValue> list = fbo.queryAllRelations(c.getPageID(), logger);
			logger.info("LIST " + list.size());
			for(PropertyValue pv: list) {
				logger.info("PROPERTYY " + pv.getValue() + " " + pv.getValRes() + " " + pv.getProperty());
				List<String> qtoks = questionRepr(questionView);
				List<String> proptoks = tokenize(pv.getProperty());
				pv.setScore(r1.probability(qtoks, proptoks));
				if (pv.getValue() == null && pv.getValRes() == null) {
					continue;
				} else if (pv.getValue() == null) {
					List<PropertyValue> secondRels = scoreSecondRelation(pv.getValRes().substring(27), questionView);
					for(PropertyValue second: secondRels) {
						List<PropertyValue> pvlist = new ArrayList<>();
						pvlist.add(pv);
						pvlist.add(second);
						logger.info("Contains " + pv.getPropRes() + " " + second.getPropRes() + " " + set.contains(pvlist));
						set.add(pvlist);
					}
				} else {
					List<PropertyValue> pvlist = new ArrayList<>();
					pvlist.add(pv);
					set.add(pvlist);
				}
			}
		}
		for (List<PropertyValue> pvlist: set) {
			List<String> properties = new ArrayList<>();
			double score = 1;
			String tmp = "";
			for(PropertyValue pv: pvlist) {
				properties.add(pv.getPropRes());
				score *= pv.getScore();
				tmp += pv.getPropRes() + "(" + pv.getScore() + ")|";
			}
			logger.info("PATH2 " + tmp);
			PropertyPath path = new PropertyPath(properties);
			scores.add(new PathScore(path, score));
		}
		Collections.sort(scores, new Comparator<PathScore>() {
			@Override
			public int compare(PathScore ps1, PathScore ps2) {
				return Double.valueOf(ps2.proba).compareTo(ps1.proba);
			}
		});
		return scores;
	}

	protected List<PropertyValue> scoreSecondRelation(String mid, JCas questionView) {
		List<PropertyValue> list = fbo.queryAllRelations(mid, "", logger);
		for(PropertyValue pv: list) {
			List<String> qtoks = questionRepr(questionView);
			List<String> proptoks = tokenize(pv.getProperty());
			pv.setScore(r2.probability(qtoks, proptoks));
		}
		return list;
	}
}
