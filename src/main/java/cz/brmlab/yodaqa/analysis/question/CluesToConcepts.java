package cz.brmlab.yodaqa.analysis.question;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

import cz.brmlab.yodaqa.model.Question.ClueSubjectNE;
import cz.brmlab.yodaqa.model.Question.ClueSubjectPhrase;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.Question.ClueConcept;
import cz.brmlab.yodaqa.model.Question.ClueLAT;
import cz.brmlab.yodaqa.model.Question.ClueNE;
import cz.brmlab.yodaqa.model.Question.CluePhrase;
import cz.brmlab.yodaqa.model.Question.ClueSubject;
import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.provider.rdf.DBpediaTitles;

/**
 * Potentially convert CluePhrase and ClueNE instances to ClueConcept
 * annotations.  This is basically an **Entity Linking** task execution.
 *
 * ClueConcept instances are much more powerful than CluePhrase and ClueNE
 * since they carry semantic information about correspondence between
 * a string and a certain concept.  This is used chiefly in two ways:
 *
 * (i) This clue is not further sub-divided.  I.e. "Moby Dick" stays that
 * way and isn't further split to "Moby" and "Dick" too.
 *
 * (ii) The page corresponding to this concept bypasses full-text solr
 * search and is directly considered for passage extraction.
 *
 * To achieve (i), we will also delete all clues covered by ClueConcept,
 * and we will of course consider ClueConcept candidates from the longest
 * to the shortest. */

public class CluesToConcepts extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(CluesToConcepts.class);

	final DBpediaTitles dbp = new DBpediaTitles();

	final ConceptClassifier classifier = new ConceptClassifier();

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas resultView) throws AnalysisEngineProcessException {
		List<Clue> clues = cluesToCheck(resultView);

		/* Try to generate more canonical labels for the clues
		 * by linking them to enwiki articles and creating
		 * a length-ordered label list. */
		HashMap<Clue, LinkedClue> linkedClues = new HashMap<>();  // auxiliary mapping, not canonical list!
		PriorityQueue<LinkedClue> cluesByLen = new PriorityQueue<>(32, new LinkedClueLengthComparator());
		for (Clue clue : clues) {
			String clueLabel = clue.getLabel();

			/* Execute entity linking from clue text to
			 * a corresponding enwiki article.  This internally
			 * involves also some fuzzy lookups and such. */
			List<DBpediaTitles.Article> results = dbp.query(clueLabel, logger);
			if (results.size() == 0)
				continue; // no linkage

			LinkedClue lc = new LinkedClue(clue, results);
			linkedClues.put(clue, lc);
			cluesByLen.add(lc);
		}

		/* If our linked clues cover other shorter clues, drop these. */
		List<LinkedClue> keptClues = new ArrayList<>(); // final list of clues
		for (LinkedClue c; (c = cluesByLen.poll()) != null; ) {
			if (subdueCoveredClues(c, linkedClues, cluesByLen)) {
				/* In fact, the covered clue subdued *us*.
				 * Typically, we have higher edit distance
				 * than the covered clue is a crisper match. */
				continue;
			}
			keptClues.add(c);
		}

		/* Build Concept annotations out of the linked clues,
		 * aggregated by their labels.  (For example, the clue
		 * "Madonna" would generate many concepts labelled
		 * "Madonna", then one concept "Madonna, Maryland", etc.) */
		/* XXX: We assume no two different (non-subdued) clues
		 * ever produce the same label. */
		Map<String, ClueLabel> labels = new TreeMap<>(); // stable ordering (?)
		for (LinkedClue c : keptClues) {
			Clue clue = c.getClue();
			for (DBpediaTitles.Article a : c.getArticles()) {
				String cookedLabel = cookLabel(clue.getLabel(), a.getCanonLabel());

				logger.debug("creating concept <<{}>>, cooked <<{}>>, d={}",
						a.getCanonLabel(), cookedLabel, a.getDist());
				Concept concept = new Concept(resultView);
				concept.setBegin(clue.getBegin());
				concept.setEnd(clue.getEnd());
				concept.setFullLabel(a.getCanonLabel());
				concept.setCookedLabel(cookedLabel);
				concept.setProbability(a.getProb());
				concept.setPageID(a.getPageID());
				concept.setEditDistance(a.getDist());
				concept.setScore(a.getScore());
				concept.setBySubject(c.isBySubject());
				concept.setByLAT(c.isByLAT());
				concept.setByNE(c.isByNE());
				concept.setByFuzzyLookup(a.isByFuzzyLookup());
				concept.setByCWLookup(a.isByCWLookup());

				if (!labels.containsKey(cookedLabel)) {
					/* First time for this particular label. */
					ClueLabel cl = new ClueLabel(clue, cookedLabel, new ArrayList<>(Arrays.asList(concept)));
					labels.put(cookedLabel, cl);
				} else {
					labels.get(cookedLabel).add(concept);
				}
			}
		}

		/* Sort ClueLabels by their score (editDist-based), pick top N
		 * and generate new clues from them.
		 * FIXME: Rather pick top N per label? */
		List<ClueLabel> labelList = new ArrayList<>(labels.values());
		Collections.sort(labelList, new ClueLabelClassifierComparator());
		List<ClueLabel> resList = labelList.subList(0, Math.min(5, labelList.size()));

		addCluesForLabels(resultView, resList);
	}

	/** Get a set of clues to check for concept links. */
	protected List<Clue> cluesToCheck(JCas resultView) {
		List<Clue> clues = new ArrayList<>();
		for (Clue clue : JCasUtil.select(resultView, CluePhrase.class))
			clues.add(clue);
		for (Clue clue : JCasUtil.select(resultView, ClueNE.class))
			clues.add(clue);
		for (Clue clue : JCasUtil.select(resultView, ClueSubjectPhrase.class))
			clues.add(clue);
		for (Clue clue : JCasUtil.select(resultView, ClueSubjectNE.class))
			clues.add(clue);
		return clues;
	}

	/** Produce a pretty label from sometimes-unwieldy enwiki article
	 * name. */
	protected String cookLabel(String clueLabel, String canonLabel) {
		String cookedLabel = new String(canonLabel);
		if (cookedLabel.toLowerCase().matches("^list of .*")) {
			logger.debug("ignoring label <<{}>> for <<{}>>", cookedLabel, clueLabel);
			cookedLabel = new String(clueLabel);
		}

		/* Remove trailing (...) (e.g. (disambiguation)). */
		/* TODO: We should model topicality of the
		 * concept; when asking about the director of
		 * "Frozen", the (film)-suffixed concepts should
		 * be preferred over e.g. the (House) suffix. */
		cookedLabel = cookedLabel.replaceAll("\\s+\\([^)]*\\)\\s*$", "");

		return cookedLabel;
	}

	/** Check overlapping clues and subdue them.  Subduing means
	 * that they are removed from the clue set, and also not considered
	 * for concept creation anymore.
	 *
	 * However, the method returns true if it's the passed @clue
	 * that has been subdued - in case it has high edit distance
	 * relative to the covered clues, i.e. typically a false positive
	 * (relying solely on that clue is typically disastrous). */
	protected boolean subdueCoveredClues(LinkedClue lc,
			HashMap<Clue, LinkedClue> linkedClues,
			PriorityQueue<LinkedClue> cluesByLen) {
		Clue clue = lc.getClue();
		for (Clue clueSub : JCasUtil.selectCovered(Clue.class, clue)) {
			LinkedClue linkedClueSub = linkedClues.get(clueSub);

			if (linkedClueSub != null && lc.getDist() - linkedClueSub.getDist() > 1.0) {
				// we found a shorter clue with (significantly) better edit distance
				logger.debug("Concept <<{}>> resisting subdue by Concept <<{}>>",
						clueSub.getLabel(), lc.getClue().getLabel());

			} else { // the longer clue wins
				logger.debug("Concept <<{}>> subduing {} <<{}>>",
						lc.getClue().getLabel(), clueSub.getType().getShortName(), clueSub.getLabel());
				if (clueSub instanceof ClueSubject)
					lc.setBySubject(true);
				else if (clueSub instanceof ClueLAT)
					lc.setByLAT(true);
				else if (clueSub instanceof ClueNE)
					lc.setByNE(true);
				if (clueSub.getWeight() > clue.getWeight()) {
					clue.setWeight(clueSub.getWeight());
				}

				clueSub.removeFromIndexes();
				removeLinkedClue(linkedClues, cluesByLen, clueSub);
			}
		}

		return false;
	}

	/** Remove the entity link records of a given clue. */
	protected void removeLinkedClue(Map<Clue, LinkedClue> linkedClues, PriorityQueue<LinkedClue> clueQueue, Clue clue) {
		List<LinkedClue> toRemove = new ArrayList<>();
		for (LinkedClue c : clueQueue)
			if (c.getClue() == clue)
				toRemove.add(c);
		for (LinkedClue c : toRemove)
			clueQueue.remove(c);

		/* XXX: This way, we might maybe keep some stale entries
		 * in linkedClues. */
		for (LinkedClue c : toRemove)
			linkedClues.remove(c);
	}

	/** Add clue(s) and register concepts for each label
	 * in the given list. */
	protected void addCluesForLabels(JCas resultView, List<ClueLabel> labelList) {
		boolean originalClueNEd = false; // guard for single ClueNE generation
		int rank = 1;
		for (ClueLabel cl : labelList) {
			Clue clue = cl.getClue();
			String clueLabel = clue.getLabel();
			String cookedLabel = cl.getCookedLabel();

			/* Remove the original clue, but do not worry, it shall
			 * be reborn stronger than ever before! */
			clue.removeFromIndexes();

			for (Concept concept : cl.getConcepts()) {
				concept.setRr(1 / ((double) rank));
				concept.addToIndexes();
				rank++;
			}

			/* Maybe the concept clue has a different label than
			 * the original wording in question text.  That can
			 * be a useful hint, but is also pretty unreliable;
			 * be it for suffixes in parentheses " (band)" or
			 * that "The ancient city" resolves to "King's Field
			 * IV" etc.
			 *
			 * Therefore, in that case we will still create the
			 * concept clue with redirect target, but also set
			 * the flag @reworded which will make this reworded
			 * text *optional* during full-text search and keep
			 * the original text (required during search) within
			 * a new ClueNE annotation. */
			boolean reworded = !clueLabel.toLowerCase().equals(cookedLabel.toLowerCase());
			/* Make a fresh concept clue. */
			addClue(resultView, clue.getBegin(), clue.getEnd(),
					clue.getBase(), clue.getWeight(),
					FSCollectionFactory.createFSList(resultView, cl.getConcepts()),
					cookedLabel, !reworded);

			/* Make also an NE clue with always the original text
			 * as label. */
			/* A presence of wiki page with the text as a title is
			 * a fair evidence that this is actually a named
			 * entity. And we need a new clue since we removed all
			 * sub-clues and the original clue might have been just
			 * a CluePhrase that gets ignored during search. */
			if (reworded && !originalClueNEd) {
					addNEClue(resultView, clue.getBegin(), clue.getEnd(),
							clue, clue.getLabel(), clue.getWeight());
				originalClueNEd = true; // once is enough
			}
		}
	}

	protected void addClue(JCas jcas, int begin, int end, Annotation base,
			double weight, FSList concepts, String label, boolean isReliable) {
		ClueConcept clue = new ClueConcept(jcas);
		clue.setBegin(begin);
		clue.setEnd(end);
		clue.setBase(base);
		clue.setWeight(weight + 0.1); // ensure precedence during merge
		clue.setConcepts(concepts);
		clue.setLabel(label);
		clue.setIsReliable(isReliable);
		clue.addToIndexes();
		logger.debug("new by {}: {} <| {}", base.getType().getShortName(), clue.getLabel(), clue.getCoveredText());
	}

	protected void addNEClue(JCas jcas, int begin, int end, Annotation base,
			String label, double weight) {
		ClueNE clue = new ClueNE(jcas);
		clue.setBegin(begin);
		clue.setEnd(end);
		clue.setBase(base);
		clue.setWeight(weight + 0.1); // ensure precedence during merge
		clue.setLabel(label);
		clue.setIsReliable(true);
		clue.addToIndexes();
		logger.debug("new(NE) by {}: {} <| {}", base.getType().getShortName(), clue.getLabel(), clue.getCoveredText());
	}

	/** A container that holds a clue with all its linked articles
	 * (sorted by edit distance). */
	private class LinkedClue {
		protected Clue clue;
		protected List<DBpediaTitles.Article> articles;

		/** Edit distance of the best linked match. */
		protected double dist;
		/** Length of the clue, for subduing purposes. */
		protected int len;

		/** Whether this linked clue subdued another clue
		 * of a certain kind. */
		protected boolean bySubject, byLAT, byNE;

		public LinkedClue(Clue clue, List<DBpediaTitles.Article> articles) {
			this.clue = clue;
			this.articles = articles;

			this.dist = articles.get(0).getDist();
			this.len = clue.getEnd() - clue.getBegin();
		}

		public Clue getClue() { return clue; }
		public List<DBpediaTitles.Article> getArticles() { return articles; }
		public double getDist() { return dist; }
		public int getLen() { return len; }

		public boolean isBySubject() { return bySubject; }
		public void setBySubject(boolean bySubject) { this.bySubject = bySubject; }
		public boolean isByLAT() { return byLAT; }
		public void setByLAT(boolean byLAT) { this.byLAT = byLAT; }
		public boolean isByNE() { return byNE; }
		public void setByNE(boolean byNE) { this.byNE = byNE; }

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			LinkedClue that = (LinkedClue) o;
			return that.getClue() == this.getClue();
		}

		@Override
		public int hashCode() {
			return this.getClue().hashCode();
		}
	}

	/** A clue label corresponds to a (Clue, Concepts[]) tuple
	 * holding all concepts sharing a particular label.
	 *
	 * The concepts are assumed to be sorted by edit distance. */
	private class ClueLabel {
		protected Clue clue;
		protected String cookedLabel;
		protected List<Concept> concepts;

		public ClueLabel(Clue clue, String cookedLabel, List<Concept> concepts) {
			this.clue = clue;
			this.cookedLabel = cookedLabel;
			this.concepts = concepts;
		}

		public Clue getClue() { return clue; }
		public String getCookedLabel() { return cookedLabel; }
		public List<Concept> getConcepts() { return concepts; }
		public void add(Concept concept) { concepts.add(concept); }
	}

	/* Compares LinkedClues using the clue length */
	private class LinkedClueLengthComparator implements Comparator<LinkedClue> {
		@Override
		public int compare(LinkedClue t1, LinkedClue t2) {
			return -Integer.compare(t1.getLen(), t2.getLen()); // longest first
		}
	}
	/* Compares ClueLabels using the concept score */
	private class ClueLabelScoreComparator implements Comparator<ClueLabel> {
		@Override
		public int compare(ClueLabel t1, ClueLabel t2) {
			return -Double.compare(t1.getConcepts().get(0).getScore(),
						t2.getConcepts().get(0).getScore()); // highest first
		}
	}
	/* Compares ClueLabels using the classifier probability*/
	private class ClueLabelClassifierComparator implements Comparator<ClueLabel> {
		@Override
		public int compare(ClueLabel t1, ClueLabel t2) {
			double cl1 = classifier.calculateProbability(t1.getConcepts().get(0));
			double cl2 = classifier.calculateProbability(t2.getConcepts().get(0));
			return -Double.compare(cl1,
					cl2); // highest first
		}
	}
}
