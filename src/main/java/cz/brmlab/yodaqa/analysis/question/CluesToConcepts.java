package cz.brmlab.yodaqa.analysis.question;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.Question.ClueConcept;
import cz.brmlab.yodaqa.model.Question.ClueNE;
import cz.brmlab.yodaqa.model.Question.CluePhrase;
import cz.brmlab.yodaqa.provider.rdf.DBpediaTitles;

/**
 * Potentially convert CluePhrase and ClueNE instances to ClueConcept
 * annotations.
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

		/* Check the clues in turn, starting by the longest - do they
		 * correspond to enwiki articles? */
		for (Clue clue; (clue = cluesByLen.poll()) != null; ) {
			List<DBpediaTitles.Article> results = dbp.query(clue.getLabel(), logger);
			if (results.isEmpty())
				continue;

			/* Yay, got one! */
			DBpediaTitles.Article a = results.get(0);

			/* Now remove all the covered sub-clues. */
			/* TODO: Mark the clues as alternatives so that we
			 * don't require both during full-text search. */
			clue.removeFromIndexes();
			cluesByLen.remove(clue);
			for (Clue clueSub : JCasUtil.selectCovered(Clue.class, clue)) {
				logger.debug("Concept {} subduing {} {}", a.getLabel(), clueSub.getType().getShortName(), clueSub.getLabel());
				clueSub.removeFromIndexes();
				cluesByLen.remove(clueSub);
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
			boolean reworded = ! clue.getLabel().toLowerCase().equals(a.getLabel().toLowerCase());

			/* Make a fresh concept clue. */
			addClue(resultView, clue.getBegin(), clue.getEnd(),
				clue.getBase(), clue.getWeight(),
				a.getPageID(), a.getLabel(), !reworded);

			/* Make also an NE clue with always the original text
			 * as label. */
			/* A presence of wiki page with the text as a title is
			 * a fair evidence that this is actually a named
			 * entity. And we need a new clue since we removed all
			 * sub-clues and the original clue might have been just
			 * a CluePhrase that gets ignored during search. */
			if (reworded)
				addNEClue(resultView, clue.getBegin(), clue.getEnd(),
					clue, clue.getWeight());
		}
	}

	protected void addClue(JCas jcas, int begin, int end, Annotation base, double weight, int pageID, String label, boolean isReliable) {
		ClueConcept clue = new ClueConcept(jcas);
		clue.setBegin(begin);
		clue.setEnd(end);
		clue.setBase(base);
		clue.setWeight(weight + 0.1); // ensure precedence during merge
		clue.setPageID(pageID);
		clue.setLabel(label);
		clue.setIsReliable(isReliable);
		clue.addToIndexes();
		logger.debug("new by {}: {} <| {}", base.getType().getShortName(), clue.getLabel(), clue.getCoveredText());
	}

	protected void addNEClue(JCas jcas, int begin, int end, Annotation base, double weight) {
		ClueNE clue = new ClueNE(jcas);
		clue.setBegin(begin);
		clue.setEnd(end);
		clue.setBase(base);
		clue.setWeight(weight + 0.1); // ensure precedence during merge
		clue.setLabel(clue.getCoveredText());
		clue.setIsReliable(true);
		clue.addToIndexes();
		logger.debug("new(NE) by {}: {} <| {}", base.getType().getShortName(), clue.getLabel(), clue.getCoveredText());
	}
}
