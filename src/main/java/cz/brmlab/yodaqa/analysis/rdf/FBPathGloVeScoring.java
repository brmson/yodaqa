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
	private static final String midPrefix = "http://rdf.freebase.com/ns/";

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

	public List<FBPathLogistic.PathScore> getPaths(JCas questionView, int pathLimitCnt) {
		List<FBPathLogistic.PathScore> scores = new ArrayList<>();
		Set<List<PropertyValue>> set = new TreeSet<>(new Comparator<List<PropertyValue>>() {
			@Override
			public int compare(List<PropertyValue> o1, List<PropertyValue> o2) {
				if (o1.size() != o2.size()) return o2.size() - o1.size();
				for (int i = 0; i < o1.size(); i++) {
					int c = o1.get(i).getPropRes().compareToIgnoreCase(o2.get(i).getPropRes());
					if (c != 0) return c;
				}
				return 0;
			}
		});

		for(Concept c: JCasUtil.select(questionView, Concept.class)) {
			List<PropertyValue> list = fbo.queryAllRelations(c.getPageID(), logger);
			for(PropertyValue pv: list) {
				if (pv.getValue() == null && pv.getValRes() == null)
					continue; // ???
				if (pv.getValue() == null && !pv.getValRes().startsWith(midPrefix))
					continue; // e.g. "Star Wars/m.0dtfn property: Trailers/film.film.trailers -> null (http://www.youtube.com/watch?v=efs57YVF2UE&feature=player_detailpage)"
				List<String> qtoks = questionRepr(questionView);
				List<String> proptoks = tokenize(pv.getProperty());
				pv.setScore(r1.probability(qtoks, proptoks));

				List<PropertyValue> pvlist = new ArrayList<>();
				pvlist.add(pv);
				set.add(pvlist);
			}
		}
		List<List<PropertyValue>> lenOnePaths = new ArrayList<>(set);
		Collections.sort(lenOnePaths, new Comparator<List<PropertyValue>>() {
			@Override
			public int compare(List<PropertyValue> list1, List<PropertyValue> list2) {
				return list1.get(0).getScore().compareTo(list2.get(0).getScore());
			}
		});
		if (lenOnePaths.size() > pathLimitCnt)
			lenOnePaths = lenOnePaths.subList(0, pathLimitCnt);

		set.clear();
		for (List<PropertyValue> path: lenOnePaths) {
			PropertyValue first = path.get(0);
			if (first.getValue() == null) {
				// meta-node, crawl it too
				String mid = first.getValRes().substring(midPrefix.length());
				List<PropertyValue> secondRels = scoreSecondRelation(mid, questionView);
				for(PropertyValue second: secondRels) {
					List<PropertyValue> tmpList = new ArrayList<>(path);
					tmpList.add(second);
					set.add(tmpList);
				}
			} else {
				set.add(path);
			}
		}
		for (List<PropertyValue> path: set) {
			List<String> properties = new ArrayList<>();

			for(PropertyValue pv: path) {
				properties.add(pv.getPropRes());
			}

			PropertyPath pp = new PropertyPath(properties);
			scores.add(new FBPathLogistic.PathScore(pp, path.get(path.size() - 1).getScore()));//Score of last relation prefer larger relations significantly
		}
		Collections.sort(scores, new Comparator<FBPathLogistic.PathScore>() {
			@Override
			public int compare(FBPathLogistic.PathScore ps1, FBPathLogistic.PathScore ps2) {
				return Double.valueOf(ps2.proba).compareTo(ps1.proba);
			}
		});
		//if (scores.size() > pathLimitCnt) scores = scores.subList(0, pathLimitCnt);
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
