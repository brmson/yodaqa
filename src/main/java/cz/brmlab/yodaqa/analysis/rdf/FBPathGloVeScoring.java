package cz.brmlab.yodaqa.analysis.rdf;

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
 * Freebase Property Path generator using exploration and GloVe label-based
 * relevancy classifier.
 */
public class FBPathGloVeScoring {
	private static final String midPrefix = "http://rdf.freebase.com/ns/";
	private static final int TOP_N_WITNESSES = 2;

	private static FBPathGloVeScoring fbpgs = new FBPathGloVeScoring();
	protected Logger logger = LoggerFactory.getLogger(FBPathGloVeScoring.class);
	private static FreebaseOntology fbo = new FreebaseOntology();

	public static FBPathGloVeScoring getInstance() {
		return fbpgs;
	}

	private Relatedness r1 = new Relatedness(new MbWeights(FBPathGloVeScoring.class.getResourceAsStream("Mbrel1.txt")));
	private Relatedness r2 = new Relatedness(new MbWeights(FBPathGloVeScoring.class.getResourceAsStream("Mbrel2.txt")));
	private Relatedness r3 = new Relatedness(new MbWeights(FBPathGloVeScoring.class.getResourceAsStream("Mbrel3.txt")));

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

	/** Get top N estimated-most-promising paths based on exploration
	 * across all linked concepts. */
	public List<FBPathLogistic.PathScore> getPaths(JCas questionView, int pathLimitCnt) {
		/* Path-deduplicating set */
		Set<List<PropertyValue>> pvPaths = new TreeSet<>(new Comparator<List<PropertyValue>>() {
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

		List<String> qtoks = questionRepr(questionView);
		logger.debug("questionRepr: {}", qtoks);

		/* Generate pvPaths for the 1-level neighborhood. */
		for(Concept c: JCasUtil.select(questionView, Concept.class))
			addConceptPVPaths(pvPaths, qtoks, c);
		List<List<PropertyValue>> lenOnePaths = getTopPVPaths(pvPaths, pathLimitCnt);

		/* Expand pvPaths for the 2-level neighborhood. */
		pvPaths.clear();
		for (List<PropertyValue> path: lenOnePaths)
			addExpandedPVPaths(pvPaths, path, qtoks);

		/* Convert to a sorted list of PathScore objects. */
		List<FBPathLogistic.PathScore> scores = pvPathsToScores(pvPaths, pathLimitCnt);

		return scores;
	}

	/** Score and add all pvpaths of a concept to the pvPaths. */
	protected void addConceptPVPaths(Set<List<PropertyValue>> pvPaths, List<String> qtoks, Concept c) {
		List<PropertyValue> list = fbo.queryAllRelations(c.getPageID(), logger);
		for(PropertyValue pv: list) {
			if (pv.getValRes() != null && !pv.getValRes().startsWith(midPrefix))
				continue; // e.g. "Star Wars/m.0dtfn property: Trailers/film.film.trailers -> null (http://www.youtube.com/watch?v=efs57YVF2UE&feature=player_detailpage)"
			List<String> proptoks = tokenize(pv.getProperty());
			pv.setScore(r1.probability(qtoks, proptoks));

			List<PropertyValue> pvlist = new ArrayList<>();
			pvlist.add(pv);
			pvPaths.add(pvlist);
		}
	}

	protected List<List<PropertyValue>> getTopPVPaths(Set<List<PropertyValue>> pvPaths, int pathLimitCnt) {
		List<List<PropertyValue>> lenOnePaths = new ArrayList<>(pvPaths);
		Collections.sort(lenOnePaths, new Comparator<List<PropertyValue>>() {
			@Override
			public int compare(List<PropertyValue> list1, List<PropertyValue> list2) {
				// descending
				return list2.get(0).getScore().compareTo(list1.get(0).getScore());
			}
		});

		/* Debug print the considered properties */
		// NB that in case of multiple values, only one is shown!
		int i = 0;
		for (List<PropertyValue> pvPath : lenOnePaths) {
			PropertyValue pv = pvPath.get(0);
			logger.debug("{} {} {}/<<{}>>/[{}] -> {} (etc.)",
				i < pathLimitCnt ? "*" : "-",
				String.format(Locale.ENGLISH, "%.3f", pv.getScore()),
				pv.getPropRes(), pv.getProperty(), tokenize(pv.getProperty()),
				pv.getValue());
			i++;
		}

		if (lenOnePaths.size() > pathLimitCnt)
			lenOnePaths = lenOnePaths.subList(0, pathLimitCnt);
		return lenOnePaths;
	}

	/** Add a path to the pvPath set, possibly replacing it with
	 * a set of trans-metanode paths.  The original path might
	 * be ending up in CVT ("compound value type") which just binds
	 * other topics together (e.g. actor playing character in movie)
	 * and to get to the answer we need to crawl one more step. */
	protected void addExpandedPVPaths(Set<List<PropertyValue>> pvPaths,	List<PropertyValue> path, List<String> qtoks) {
		PropertyValue first = path.get(0);
		if (first.getValRes() != null && /* no label */ first.getValRes().endsWith(first.getValue())) {
			// meta-node, crawl it too
			String mid = first.getValRes().substring(midPrefix.length());
			List<List<PropertyValue>> secondPaths = scoreSecondRelation(mid, qtoks);
			for (List<PropertyValue> secondPath: secondPaths) {
				List<PropertyValue> newpath = new ArrayList<>(path);
				newpath.addAll(secondPath);
				pvPaths.add(newpath);

				// NB that in case of multiple metanodes/values, only one is shown!
				PropertyValue pv = newpath.get(1);
				logger.debug("+ {} {}/<<{}>>/[{}]{} -> {} (etc.)",
						String.format(Locale.ENGLISH, "%.3f", pv.getScore()),
						pv.getPropRes(), pv.getProperty(), tokenize(pv.getProperty()),
						newpath.size() == 3 ? " |" + newpath.get(2).getPropRes() : "",
						pv.getValue());
			}
		} else {
			pvPaths.add(path);
		}
	}

	protected List<List<PropertyValue>> scoreSecondRelation(String mid, List<String> qtoks) {
		List<PropertyValue> nextpvs = fbo.queryAllRelations(mid, "", logger);

		List<PropertyValue> witnessPvCandidates = new ArrayList<>(nextpvs);
		for(PropertyValue wpv: witnessPvCandidates) {
			List<String> wproptoks = tokenize(wpv.getProperty());
			wpv.setScore(r3.probability(qtoks, wproptoks));
		}

		/* Now, add the followup paths, possibly including a required
		 * witness match. */
		List<List<PropertyValue>> secondPaths = new ArrayList<>();
		for (PropertyValue pv: nextpvs) {

			List<String> proptoks = tokenize(pv.getProperty());
			pv.setScore(r2.probability(qtoks, proptoks));

			List<PropertyValue> secondPath = new ArrayList<>();
			secondPath.add(pv);
			secondPaths.add(secondPath);

			List<List<PropertyValue>> witnessPaths = new ArrayList<>();
			for(PropertyValue wpv: witnessPvCandidates) {
				List<PropertyValue> newpath = new ArrayList<>(secondPath);
				newpath.add(wpv);
				witnessPaths.add(newpath);
			}
			
			Collections.sort(witnessPaths, new Comparator<List<PropertyValue>>() {
				@Override
				public int compare(List<PropertyValue> list1, List<PropertyValue> list2) {
					// descending
					return list2.get(1).getScore().compareTo(list1.get(1).getScore());
				}
			});
			if (witnessPaths.size() > TOP_N_WITNESSES)
				witnessPaths = witnessPaths.subList(0, TOP_N_WITNESSES);
			secondPaths.addAll(witnessPaths);
		}
		return secondPaths;
	}

	protected List<FBPathLogistic.PathScore> pvPathsToScores(Set<List<PropertyValue>> pvPaths, int pathLimitCnt) {
		List<FBPathLogistic.PathScore> scores = new ArrayList<>();
		for (List<PropertyValue> path: pvPaths) {
			List<String> properties = new ArrayList<>();

			double score = 0;
			for(PropertyValue pv: path) {
				properties.add(pv.getPropRes());
				score += pv.getScore();
			}
			score /= path.size();

			PropertyPath pp = new PropertyPath(properties);
			// XXX: better way than averaging?

			FBPathLogistic.PathScore ps = new FBPathLogistic.PathScore(pp, score);
			scores.add(ps);
		}
		Collections.sort(scores, new Comparator<FBPathLogistic.PathScore>() {
			@Override
			public int compare(FBPathLogistic.PathScore ps1, FBPathLogistic.PathScore ps2) {
				// descending
				return Double.valueOf(ps2.proba).compareTo(ps1.proba);
			}
		});
		if (scores.size() > pathLimitCnt)
			scores = scores.subList(0, pathLimitCnt);
		return scores;
	}
}
