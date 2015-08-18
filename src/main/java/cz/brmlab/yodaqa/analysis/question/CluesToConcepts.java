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

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas resultView) throws AnalysisEngineProcessException {
		/* Put all relevant Clues in a length-ordered list. */
		PriorityQueue<Clue> cluesByLen = new PriorityQueue<Clue>(32,
			new Comparator<Clue>(){ @Override
				public int compare(Clue c1, Clue c2) {
					int l1 = c1.getEnd() - c1.getBegin();
					int l2 = c2.getEnd() - c2.getBegin();
					return -(l1 - l2); // from largest length
				}
			});
		for (Clue clue : JCasUtil.select(resultView, CluePhrase.class))
			cluesByLen.add(clue);
		for (Clue clue : JCasUtil.select(resultView, ClueNE.class))
			cluesByLen.add(clue);
		for (Clue clue : JCasUtil.select(resultView, ClueSubjectPhrase.class))
			cluesByLen.add(clue);
		for (Clue clue : JCasUtil.select(resultView, ClueSubjectNE.class))
			cluesByLen.add(clue);

		/* Check the clues in turn, starting by the longest - do they
		 * correspond to enwiki articles? */
		HashMap<Clue, List<DBpediaTitles.Article>> cluesAndArticles = new HashMap<>();
		PriorityQueue<ClueAndArticle> clueAndArticleQueue = new PriorityQueue<>(32, new ClueAndArticleLengthComparator());
		for (Clue clue; (clue = cluesByLen.poll()) != null; ) {
			String clueLabel = clue.getLabel();
			cluesAndArticles.put(clue, new ArrayList<DBpediaTitles.Article>());
			/* Execute entity linking from clue text to
			 * a corresponding enwiki article.  This internally
			 * involves also some fuzzy lookups and such. */
			List<DBpediaTitles.Article> results = dbp.query(clueLabel, logger);
			for (DBpediaTitles.Article a : results) {
				cluesAndArticles.get(clue).add(a);
				clueAndArticleQueue.add(new ClueAndArticle(a, clue));
			}
		}

		/* If we match multiple overlapping clues, prefer the
		 * longest one, and construct a final list of labels. */
		Map<String, List<Concept>> labels = new TreeMap<>(); // stable ordering (?)
		List<ClueAndArticle> keptClues = new ArrayList<>(); // final list of clues, without subdued ones
		for (ClueAndArticle c; (c = clueAndArticleQueue.poll()) != null; ) {
			Clue clue = c.getClue();
			String clueLabel = clue.getLabel();
			boolean foundBetter = false;
			DBpediaTitles.Article a = c.getArticle();
			String cookedLabel = a.getCanonLabel();
			double weight = clue.getWeight();

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

			logger.debug("creating concept <<{}>>, cooked <<{}>>, d={}",
					a.getCanonLabel(), cookedLabel, a.getDist());
			Concept concept = new Concept(resultView);
			concept.setBegin(clue.getBegin());
			concept.setEnd(clue.getEnd());
			concept.setFullLabel(a.getCanonLabel());
			concept.setCookedLabel(cookedLabel);
			concept.setPageID(a.getPageID());
			concept.setScore(a.getScore());

			for (Clue clueSub : JCasUtil.selectCovered(Clue.class, clue)) {
				List<DBpediaTitles.Article> l = cluesAndArticles.get(clueSub);  // get covered articles
				if (l == null || l.size() == 0)
					continue;

				DBpediaTitles.Article curr = l.get(0); // XXX should iterate through articles too?
				double distance = curr.getDist();
				if (!(a.getDist() - distance <= 1.0)) { //we found a shorter article with better edit distance
					logger.debug("Concept <<{}>> subduing {} {}>>",
							clueSub.getLabel(), clue.getType().getShortName(), cookedLabel);
					foundBetter = true;
					clue.removeFromIndexes();
					break;

				} else if (!(a.getDist() - distance > 1.0)) { //the longer article won
					logger.debug("Concept <<{}>> subduing {} <<{}>>",
							cookedLabel, clueSub.getType().getShortName(), clueSub.getLabel());
					cluesAndArticles.remove(clueSub);
					if (clueSub instanceof ClueSubject)
						concept.setBySubject(true);
					else if (clueSub instanceof ClueLAT)
						concept.setByLAT(true);
					else if (clueSub instanceof ClueNE)
						concept.setByNE(true);
					if (clueSub.getWeight() > weight)
						weight = clueSub.getWeight();

					if (clueAndArticleQueue.remove(new ClueAndArticle(curr, clueSub)) == false)
						logger.error("removing {} from queue not successfull", clueSub.getLabel());
					clueSub.removeFromIndexes();
				}
			}

			if (foundBetter)
				continue;

			keptClues.add(c);
			if (labels.containsKey(cookedLabel)) { //XXX This is awkward since each ClueAndArticle contains exactly one Article instead of a list
				logger.debug("adding {} to label list", concept.getCookedLabel()); //labels is now a global map instead of a local one
				labels.get(cookedLabel).add(concept);
			} else {
				logger.debug("adding unique {} to label list", concept.getCookedLabel());
				labels.put(cookedLabel, new ArrayList<>(Arrays.asList(concept)));
			}
		}

		/* Sort labels by their score (editDist-based) and generate
		 * new clues from them. */
		Collections.sort(keptClues, new ClueAndArticleScoreComparator());
		boolean originalClueNEd = false; // guard for single ClueNE generation
		int rank = 1;
		for(int i = 0; i < keptClues.size(); i++) {
			Clue clue = keptClues.get(i).getClue();
			DBpediaTitles.Article a = keptClues.get(i).getArticle();
			String clueLabel = clue.getLabel();

			double weight = clue.getWeight();
			String cookedLabel = a.getCanonLabel();
			if (cookedLabel.toLowerCase().matches("^list of .*")) {
				cookedLabel = new String(clueLabel);
			}
			cookedLabel = cookedLabel.replaceAll("\\s+\\([^)]*\\)\\s*$", "");
			clue.removeFromIndexes();

			List<Concept> concepts = labels.get(cookedLabel);
			for (int j = 0; j < concepts.size(); j++) {
				Concept concept = concepts.get(j);
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
					clue.getBase(), weight,
					FSCollectionFactory.createFSList(resultView, concepts),
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
							clue, clue.getLabel(), weight);
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

	private class ClueAndArticle {
		protected DBpediaTitles.Article result;
		protected Clue clue;

		public ClueAndArticle(DBpediaTitles.Article result, Clue clue) {
			this.result = result;
			this.clue = clue;
		}
		public DBpediaTitles.Article getArticle() {
			return result;
		}
		public Clue getClue() {
			return clue;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			ClueAndArticle that = (ClueAndArticle) o;

			if (result != null ? !result.equals(that.result) : that.result != null) return false;
			return !(clue != null ? !clue.equals(that.clue) : that.clue != null);

		}

		@Override
		public int hashCode() {
			int result1 = result != null ? result.hashCode() : 0;
			result1 = 31 * result1 + (clue != null ? clue.hashCode() : 0);
			return result1;
		}
	}

	/* Compares using the String length */
	private class ClueAndArticleLengthComparator implements Comparator<ClueAndArticle> {
		@Override
		public int compare(ClueAndArticle t1, ClueAndArticle t2) {
			int l1 = t1.getClue().getEnd() - t1.getClue().getBegin();
			int l2 = t1.getClue().getEnd() - t2.getClue().getBegin();
			return -Integer.compare(l1, l2); // longest first
		}
	}
	/* Compares using the Article Score */
	private class ClueAndArticleScoreComparator implements Comparator<ClueAndArticle> {
		@Override
		public int compare(ClueAndArticle t1, ClueAndArticle t2) {
			return -Double.compare(t1.getArticle().getScore(), t2.getArticle().getScore()); // highest first
		}
	}

}
