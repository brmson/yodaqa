package cz.brmlab.yodaqa.analysis.question;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

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
		for (Clue clue : JCasUtil.select(resultView, ClueSubjectPhrase.class))
			cluesByLen.add(clue);
		for (Clue clue : JCasUtil.select(resultView, ClueSubjectNE.class))
			cluesByLen.add(clue);

		/* Check the clues in turn, starting by the longest - do they
		 * correspond to enwiki articles? */
		for (Clue clue; (clue = cluesByLen.poll()) != null; ) {
			String clueLabel = clue.getLabel();
			double weight = clue.getWeight();
			List<Concept> concepts = new ArrayList<>();
			Set<String> labels = new TreeSet<>(); // stable ordering (?)

			/* Generate Concepts and gather ConceptClue labels. */

			List<DBpediaTitles.Article> results = dbp.query(clueLabel, logger);

			for (DBpediaTitles.Article a : results) {
				logger.debug("Canon label: " + a.getCanonLabel() + " name: " + a.getName() + " matched queries: " + a.getCount() + " pageID: " + a.getPageID());
			}

			Collections.sort(results, new Comparator<DBpediaTitles.Article>() {
				@Override
				public int compare(DBpediaTitles.Article a1, DBpediaTitles.Article a2) {
					return Integer.compare(a2.getCount(), a1.getCount());
				}
			} );

			//look for top three or less
			for (DBpediaTitles.Article a : results.subList(0, Math.min(3, results.size()))) {
				String cookedLabel = a.getCanonLabel();
				/* But in case of "list of...", keep the original label
				 * (but still generate a conceptclue since we have
				 * a confirmed named entity and we want to include
				 * the list in our document set). */
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

				/* Start constructing the annotation. */
				Concept concept = new Concept(resultView);
				concept.setBegin(clue.getBegin());
				concept.setEnd(clue.getEnd());
				concept.setFullLabel(a.getCanonLabel());
				concept.setCookedLabel(cookedLabel);
				concept.setPageID(a.getPageID());

				/* Also remove all the covered sub-clues. */
				/* TODO: Mark the clues as alternatives so that we
				 * don't require both during full-text search. */
				clue.removeFromIndexes();
				cluesByLen.remove(clue);
				for (Clue clueSub : JCasUtil.selectCovered(Clue.class, clue)) {
					logger.debug("Concept {} subduing {} {}", cookedLabel, clueSub.getType().getShortName(), clueSub.getLabel());
					if (clueSub instanceof ClueSubject)
						concept.setBySubject(true);
					else if (clueSub instanceof ClueLAT)
						concept.setByLAT(true);
					else if (clueSub instanceof ClueNE)
						concept.setByNE(true);
					if (clueSub.getWeight() > weight)
						weight = clueSub.getWeight();
					clueSub.removeFromIndexes();
					cluesByLen.remove(clueSub);
				}

				concept.addToIndexes();
				concepts.add(concept);
				labels.add(cookedLabel);
			}

			/* Generate ClueConcepts. */

			boolean originalClueNEd = false; // guard for single ClueNE generation
			for (String cookedLabel : labels) {
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
				boolean reworded = ! clueLabel.toLowerCase().equals(cookedLabel.toLowerCase());

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
}
