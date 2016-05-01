package cz.brmlab.yodaqa.analysis.rdf;

import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.Question.SV;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.WordnetLAT;
import cz.brmlab.yodaqa.provider.glove.MbWeights;
import cz.brmlab.yodaqa.provider.glove.Relatedness;
import cz.brmlab.yodaqa.provider.rdf.FreebaseExploration;
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
	private static final int TOP_N_ENTITIES_REPLACE = 0;

	private static FBPathGloVeScoring fbpgs = new FBPathGloVeScoring();
	protected Logger logger = LoggerFactory.getLogger(FBPathGloVeScoring.class);
	private static FreebaseOntology fbo = new FreebaseOntology();

	public static FBPathGloVeScoring getInstance() {
		return fbpgs;
	}

	private Relatedness r1 = new Relatedness(new MbWeights(FBPathGloVeScoring.class.getResourceAsStream("Mbrel1.txt")));
	private Relatedness r2 = new Relatedness(new MbWeights(FBPathGloVeScoring.class.getResourceAsStream("Mbrel2.txt")));
	private Relatedness r3 = new Relatedness(new MbWeights(FBPathGloVeScoring.class.getResourceAsStream("Mbrel3.txt")));

	private List<List<PropertyValue>> pathDump;
	private LogisticRegressionFBPathRanking ranker = new LogisticRegressionFBPathRanking();
	private HashMap<Integer, Set<FreebaseOntology.TitledMid>> midCache = new HashMap<>();

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

	private String fullQuestionRepr(JCas questionView, List<PropertyValue> path) {
		String entityToken = "ENT_TOK";
		QuestionInfo qi = JCasUtil.selectSingle(questionView, QuestionInfo.class);
		String text = qi.getCAS().getDocumentText();
		List<Concept> allConcepts = new ArrayList<>(JCasUtil.select(questionView, Concept.class));
		HashSet<String> entities = new HashSet<>();
		entities.add(path.get(0).getObjRes().substring(1).replace("/", "."));
		if (path.size() > 2 && path.get(2).getObjRes() != null) entities.add(path.get(2).getObjRes().substring(1).replace("/", "."));
		List<Concept> concepts = new ArrayList<>();
		for(Concept c: allConcepts) {
			if (!midCache.containsKey(c.getPageID())) {
				midCache.put(c.getPageID(), fbo.queryTopicByPageID(c.getPageID(), logger));
			}
			for(FreebaseOntology.TitledMid tmid: midCache.get(c.getPageID())) {
				if (entities.contains(tmid.mid)) {
					concepts.add(c);
					logger.debug("Adding {} {}", c.getFullLabel(), c.getPageID());
					break;
				}
			}
		}
		Collections.sort(concepts, new Comparator<Concept>() {
			@Override
			public int compare(Concept c1, Concept c2) {
				return Double.valueOf(c2.getScore()).compareTo(c1.getScore());
			}
		});
		List<List<Integer>> idxs = new ArrayList<>();
		for(Concept concept: concepts) {
			List<Integer> beginEnd = new ArrayList<>();
			beginEnd.add(concept.getBegin());
			beginEnd.add(concept.getEnd());
			idxs.add(beginEnd);
		}
		Collections.sort(idxs, new Comparator<List<Integer>>() {
			@Override
			public int compare(List<Integer> l1, List<Integer> l2) {
				if (l1.get(0).equals(l2.get(0))) return l2.get(1).compareTo(l1.get(1));
				else return l2.get(0).compareTo(l1.get(0));
			}
		});
		int prevb = -1, preve = -1;
		for (int i = 0; i < idxs.size(); i++) {
//			if (i == entityLimit) break;
			int size = text.length();
			int b = idxs.get(i).get(0);
			int e = idxs.get(i).get(1);
			if (prevb == b && preve == e) continue;
			if (b < size && e <= size) {
				text = text.substring(0, b) + entityToken + text.substring(e, size);
			}
			prevb = b;
			preve = e;
		}
		return text;
	}

	/** Get top N estimated-most-promising paths based on exploration
	 * across all linked concepts. */
	public List<FBPathLogistic.PathScore> getPaths(JCas questionView, int pathLimitCnt) {
		/* Path-deduplicating set */
		Set<List<PropertyValue>> pathSet = new TreeSet<>(new Comparator<List<PropertyValue>>() {
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
		List<List<PropertyValue>> pvPaths = new ArrayList<>();
		FreebaseExploration fbex = new FreebaseExploration();
		List<String> qtoks = questionRepr(questionView);
		logger.debug("questionRepr: {}", qtoks);

//		questionText = fullQuestionRepr(questionView, TOP_N_ENTITIES_REPLACE);
		List<Concept> concepts = new ArrayList<>(JCasUtil.select(questionView, Concept.class));
		/* Generate pvPaths for the 1-level neighborhood. */
		for(Concept c: JCasUtil.select(questionView, Concept.class)) {
			pvPaths.addAll(fbex.getConceptNeighbourhood(c, concepts, JCasUtil.select(questionView, Clue.class)));
//			addConceptPVPaths(pvPaths, qtoks, c);
		}
		//XXX Select top N path counting only distincs ones
//		List<List<PropertyValue>> lenOnePaths = getTopPVPaths(pvPaths, Integer.MAX_VALUE);


		/* Expand pvPaths for the 2-level neighborhood. */
//		pvPaths.clear();

//		for(Concept c: concepts) {
//			logger.debug("CONCEPT " + c.getFullLabel() + " " + c.getPageID());
//			logger.debug(c.getBegin() + " " + c.getEnd());
//
//		}
//		for (List<PropertyValue> path: lenOnePaths)
//			addExpandedPVPaths(pvPaths, path, qtoks, concepts);

//		List<List<PropertyValue>> lenTwoPaths = new ArrayList<>(pvPaths);

		/* Add witness relations to paths of length 2 if possible */
//		pvPaths.clear();
//		List<List<PropertyValue>> potentialWitnesses = getPotentialWitnesses(concepts, qtoks);
//		for (List<PropertyValue> path: lenTwoPaths)
//			addWitnessPVPaths(pvPaths, path, potentialWitnesses);

		// Deduplication
		pathSet.addAll(pvPaths);
		pathDump = new ArrayList<>(pathSet);
		/* Convert to a sorted list of PathScore objects. */
		List<FBPathLogistic.PathScore> scores = pvPathsToScores(pathSet, questionView, pathLimitCnt);

		return scores;
	}

//	protected List<Double> getRnnScores(List<PropertyValue> list, int propertyNum) {
//		List<String> propLabels = new ArrayList<>();
//		for(PropertyValue pv: list) {
//			propLabels.add(pv.getProperty());
//		}
//		return RNNScoring.getScores(questionText, propLabels, propertyNum);
//	}

	protected List<Double> getFullPathRnnScores(List<List<PropertyValue>> list, JCas questionView) {
		HashMap<String, List<List<String>>> questions = new HashMap<>();
		for(List<PropertyValue> path: list) {
			String qtext = fullQuestionRepr(questionView, path);
			List<String> propLabels = new ArrayList<>();
			String tmp = "";
			for(PropertyValue pv: path) {
				propLabels.add(pv.getProperty());
				tmp += pv.getProperty() + " | ";
			}
			if (!questions.containsKey(qtext)) questions.put(qtext, new ArrayList<List<String>>());
			questions.get(qtext).add(propLabels);
		}
		List<Double> res = new ArrayList<>();
		for(Map.Entry<String, List<List<String>>> e: questions.entrySet()) {
			res.addAll(RNNScoring.getFullPathScores(e.getKey(), e.getValue()));
		}
		return res;
	}

	/** Score and add all pvpaths of a concept to the pvPaths. */
	protected void addConceptPVPaths(List<List<PropertyValue>> pvPaths, List<String> qtoks, Concept c) {
		List<PropertyValue> list = fbo.queryAllRelations(c.getPageID(), null, logger);
		for(PropertyValue pv: list) {
			pv.setProperty(fbo.queryPropertyLabel(pv.getPropRes()));
		}
//		List<Double> scores = getRnnScores(list, 1);
		int i = 0;
		for(PropertyValue pv: list) {
//			if (pv.getValRes() != null && !pv.getValRes().startsWith(midPrefix)) {
//				continue; // e.g. "Star Wars/m.0dtfn property: Trailers/film.film.trailers -> null (http://www.youtube.com/watch?v=efs57YVF2UE&feature=player_detailpage)"
//			}
			List<String> proptoks = tokenize(pv.getProperty());
			pv.setScore(r1.probability(qtoks, proptoks));
//			pv.setScore(scores.get(i));
//			logger.debug("SCORESS " + r1.probability(qtoks, proptoks) + " " + scores.get(i));
			logger.debug("FIRST " + pv.getPropRes());
			List<PropertyValue> pvlist = new ArrayList<>();
			pvlist.add(pv);
			pvPaths.add(pvlist);
			i++;
		}
	}

	protected List<List<PropertyValue>> getTopPVPaths(List<List<PropertyValue>> pvPaths, int pathLimitCnt) {
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
	protected void addExpandedPVPaths(List<List<PropertyValue>> pvPaths, List<PropertyValue> path, List<String> qtoks, List<Concept> concepts) {
		PropertyValue first = path.get(0);
//		if (first.getValRes() != null && /* no label */ first.getValRes().endsWith(first.getValue())) {
			// meta-node, crawl it too
//			String mid = first.getValRes().substring(midPrefix.length());
//			String title = first.getValue();
			List<List<PropertyValue>> secondPaths = scoreSecondRelation(first.getPropRes(), qtoks, concepts);
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
			if (secondPaths.size() == 0) pvPaths.add(path);
//		} else {
//			pvPaths.add(path);
//		}
	}

	protected List<List<PropertyValue>> scoreSecondRelation(String prop, List<String> qtoks, List<Concept> concepts) {
//		List<PropertyValue> nextpvs = fbo.queryAllRelations(mid, title, logger);
		List<PropertyValue> nextpvs = new ArrayList<>();
		for (Concept c: concepts) {
//			fbo.isExpandable(c.getPageID(), prop);
			String metaMid = fbo.preExpand(c.getPageID(), prop);
			if (metaMid == null) continue;
//			List<PropertyValue> results = fbo.queryAllRelations(metaMid, "", null, logger);
			List<PropertyValue> results = fbo.queryAllRelations(c.getPageID(), prop, logger);
			for(PropertyValue res: results) {
				res.setProperty(fbo.queryPropertyLabel(res.getPropRes()));
			}
			nextpvs.addAll(results);
		}
//		List<Double> scores = getRnnScores(nextpvs, 2);
		/* Now, add the followup paths, possibly including a required
		 * witness match. */
		int i = 0;
		List<List<PropertyValue>> secondPaths = new ArrayList<>();
		for (PropertyValue pv: nextpvs) {
			logger.debug("SECOND " + pv.getPropRes());
			List<String> proptoks = tokenize(pv.getProperty());
			pv.setScore(r2.probability(qtoks, proptoks));
//			pv.setScore(scores.get(i));
			List<PropertyValue> secondPath = new ArrayList<>();
			secondPath.add(pv);
			secondPaths.add(secondPath);
			i++;
		}
		return secondPaths;
	}

	protected List<List<PropertyValue>> getPotentialWitnesses(List<Concept> concepts, List<String> qtoks) {
		List<List<PropertyValue>> res = new ArrayList<>();
		logger.debug("Number of concepts: " + concepts.size());
		for(Concept c: concepts) {
			for (Concept w: concepts) {
				logger.debug("Page ids " + c.getPageID() + " to " + w.getPageID());
				if (c.getPageID() == w.getPageID()) continue;
				logger.debug("Witness path from " + c.getFullLabel() + " to " + w.getFullLabel());
				List<List<PropertyValue>> paths = fbo.queryWitnessRelations(c.getPageID(), c.getFullLabel(), w.getPageID(), logger);
				for(List<PropertyValue> path: paths) {
					path.get(1).setConcept(c);
				}
				res.addAll(paths);
			}
		}
		// Scoring...
		List<PropertyValue> witProps = new ArrayList<>();
		for(List<PropertyValue> list: res) {
			witProps.add(list.get(1));
		}
//		List<Double> scores = getRnnScores(witProps, 3);
		int i = 0;
		for(List<PropertyValue> witPath: res) {
			PropertyValue wpv = witPath.get(1);
			List<String> wproptoks = tokenize(wpv.getProperty());
			wpv.setScore(r3.probability(qtoks, wproptoks));
//			wpv.setScore(scores.get(i));
			i++;
		}
		return res;
	}

	protected void addWitnessPVPaths(List<List<PropertyValue>> pvPaths, List<PropertyValue> path,
									 List<List<PropertyValue>> potentialWitnesses) {
//		boolean added = false;
		pvPaths.add(path);
		if (path.size() == 2) {
			for(List<PropertyValue> witPath: potentialWitnesses) {
				if (!path.get(0).getPropRes().equals(witPath.get(0).getPropRes()))
					continue;
				if (path.get(1).getPropRes().equals(witPath.get(1).getPropRes()))
					continue;
				List<PropertyValue> newPath = new ArrayList<>(path);
				newPath.add(witPath.get(1));
				pvPaths.add(newPath);
				PropertyValue pv = newPath.get(2);
//				added = true;
				logger.debug("++w {} {}/<<{}>>/[{}] -> {} (etc.)",
						String.format(Locale.ENGLISH, "%.3f", pv.getScore()),
						pv.getPropRes(), pv.getProperty(), tokenize(pv.getProperty()),
						pv.getValue());
			}
		}
//		if (!added) pvPaths.add(path);
	}

	public List<List<PropertyValue>> dump() {
		return pathDump;
	}

	protected List<FBPathLogistic.PathScore> pvPathsToScores(Set<List<PropertyValue>> pvPaths, JCas questionView, int pathLimitCnt) {
		List<FBPathLogistic.PathScore> scores = new ArrayList<>();
		List<List<PropertyValue>> pathList = new ArrayList<>(pvPaths);
		List<Double> rnnScores = getFullPathRnnScores(pathList, questionView);
//		Collections.sort(pathList, new Comparator<List<PropertyValue>>() {
//			@Override
//			public int compare(List<PropertyValue> list1, List<PropertyValue> list2) {
//				// descending
//				int cmp;
//				for (int i = 0; i < Math.min(list1.size(), list2.size()); i++) {
//					cmp = list2.get(i).getScore().compareTo(list1.get(i).getScore());
//					if (cmp != 0 || Math.max(list1.size(), list2.size()) == i + 1) return cmp;
//				}
//				return Integer.valueOf(list2.size()).compareTo(Integer.valueOf(list1.size()));
//			}
//		});

//		List<List<PropertyValue>> reducedList = new ArrayList<>();
//		List<PropertyValue> prev = null;
//		for (List<PropertyValue> path: pathList) {
//			if (prev == null || !path.get(0).getPropRes().equals(prev.get(0).getPropRes())) reducedList.add(path);
//			prev = path;
//		}
		int scoreIdx = 0;
		for (List<PropertyValue> path: pathList) {
			List<String> properties = new ArrayList<>();
//			logger.debug("Logistic regression score: " + ranker.getScore(path));
			double score = 0;
			String s = "";
			for(PropertyValue pv: path) {
				properties.add(pv.getPropRes());
//				score += pv.getScore();
				s += pv.getPropRes() + " | ";
			}
			logger.debug(s);
//			score /= path.size();
//			score = ranker.getScore(path);
			score = 1 / (1 + Math.exp(-rnnScores.get(scoreIdx++)));

			PropertyPath pp = new PropertyPath(properties);
			// XXX: better way than averaging?

			FBPathLogistic.PathScore ps = new FBPathLogistic.PathScore(pp, score);
			ps.entity = path.get(0);
			if (path.size() == 3) ps.witness = path.get(2);
			logger.debug("NEW path score " + ps.entity + " " + ps.witness);
			scores.add(ps);
		}
		Collections.sort(scores, new Comparator<FBPathLogistic.PathScore>() {
			@Override
			public int compare(FBPathLogistic.PathScore ps1, FBPathLogistic.PathScore ps2) {
				// descending
				return Double.valueOf(ps2.proba).compareTo(ps1.proba);
			}
		});
		logger.debug("Limit of explorative paths " + pathLimitCnt);
		for(FBPathLogistic.PathScore s: scores) {
			String str = "";
			for (int i = 0; i < s.path.size(); i++) {
				str += s.path.get(i) + " | ";
			}
			logger.debug("Explorative paths: " + str + " " + s.proba );
		}
		if (scores.size() > pathLimitCnt)
			scores = scores.subList(0, pathLimitCnt);
		return scores;
	}
}
