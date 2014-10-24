package cz.brmlab.yodaqa.analysis.question;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.Clue;

/**
 * Merge Clues whose coveredText is the same.
 *
 * The redundant annotations will be removed; the one with the highest score
 * will be kept; therefore, the resulting Clue score will be a maximum, not
 * a sum!
 *
 * When merging CandidateAnswers, independent production of each of them
 * is an additional evidence for the eventual answer, so we sum the scores.
 * But when merging Clues, they all stem from the base word and it was just
 * matched multiple ways, but these do not represent independent knowledge
 * sources, so we just do a max. */

public class CluesMergeByText extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(CluesMergeByText.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas resultView) throws AnalysisEngineProcessException {
		Map<String, List<Clue>> cluesByText = new HashMap<String, List<Clue>>();

		/* Put all Clues in a text-addressed map. */
		for (Clue clue : JCasUtil.select(resultView, Clue.class)) {
			String text = clue.getLabel();
			List<Clue> clues = cluesByText.get(text);
			if (clues == null) {
				clues = new LinkedList<Clue>();
				cluesByText.put(text, clues);
			}
			clues.add(clue);
		}

		/* Deal with map items that hold multiple answers. */
		for (Entry<String, List<Clue>> entry : cluesByText.entrySet()) {
			if (entry.getValue().size() == 1)
				continue;

			Clue mainClue = null;
			for (Clue clue : entry.getValue()) {
				clue.removeFromIndexes();
				if (mainClue == null) {
					mainClue = clue;
				} else if (mainClue.getWeight() <= clue.getWeight()) {
					subdueInfo(mainClue, clue);
					if (mainClue.getIsReliable() && !clue.getIsReliable())
						clue.setIsReliable(true);
					mainClue = clue;
				} else {
					subdueInfo(clue, mainClue);
					if (!mainClue.getIsReliable() && clue.getIsReliable())
						mainClue.setIsReliable(true);
				}
			}
			mainClue.addToIndexes();
		}
	}

	protected void subdueInfo(Clue subdued, Clue subduing) {
		logger.debug("subduing {}({}:{},{}) <| {}({}:{},{})",
			subdued.getLabel(),
			subdued.getType().getShortName(),
			subdued.getWeight(),
			subdued.getIsReliable(),
			subduing.getLabel(),
			subduing.getType().getShortName(),
			subduing.getWeight(),
			subduing.getIsReliable());
	}
}
