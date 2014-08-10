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
import cz.brmlab.yodaqa.model.Question.ClueToken;
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
		for (Clue clue : JCasUtil.select(resultView, ClueToken.class))
			cluesByLen.add(clue);
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

			/* Now remove all the covered clues. */
			clue.removeFromIndexes();
			for (Clue clueSub : JCasUtil.selectCovered(Clue.class, clue)) {
				clueSub.removeFromIndexes();
				cluesByLen.remove(clueSub);
			}

			/* Make a fresh clue. */
			DBpediaTitles.Article a = results.get(0);
			addClue(resultView, clue.getBegin(), clue.getEnd(),
				clue.getBase(), clue.getWeight(),
				a.getPageID(), a.getLabel());
		}
	}

	protected void addClue(JCas jcas, int begin, int end, Annotation base, double weight, int pageID, String label) {
		ClueConcept clue = new ClueConcept(jcas);
		clue.setBegin(begin);
		clue.setEnd(end);
		clue.setBase(base);
		clue.setWeight(weight + 0.1); // ensure precedence during merge
		clue.setPageID(pageID);
		clue.setLabel(label);
		clue.addToIndexes();
		logger.debug("new by {}: {} <| {}", base.getType().getShortName(), clue.getLabel(), clue.getCoveredText());
	}
}
