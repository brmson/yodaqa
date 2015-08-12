package cz.brmlab.yodaqa.analysis.passage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;

/**
 * Merge CandidateAnswers whose coveredText is the same.
 *
 * The redundant annotations will be removed and their score added
 * to the kept one. */

public class CanMergeByText extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(CanMergeByText.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas resultView) throws AnalysisEngineProcessException {
		Map<String, List<CandidateAnswer>> cansByText = new HashMap<String, List<CandidateAnswer>>();

		/* Put all CandidateAnswers in a text-addressed map. */
		for (CandidateAnswer can : JCasUtil.select(resultView, CandidateAnswer.class)) {
			String text = can.getCoveredText();
			List<CandidateAnswer> cans = cansByText.get(text);
			if (cans == null) {
				cans = new LinkedList<CandidateAnswer>();
				cansByText.put(text, cans);
			}
			cans.add(can);
		}

		/* Deal with map items that hold multiple answers. */
		for (Entry<String, List<CandidateAnswer>> entry : cansByText.entrySet()) {
			if (entry.getValue().size() == 1)
				continue;

			CandidateAnswer mainCan = null;
			AnswerFV mainCanFV = null;
			for (CandidateAnswer can : entry.getValue()) {
				if (mainCan == null) {
					mainCan = can;
					mainCanFV = new AnswerFV(mainCan);
					for (FeatureStructure af : mainCan.getFeatures().toArray())
						((AnswerFeature) af).removeFromIndexes();
					mainCan.removeFromIndexes();
					continue;
				}
				//we use Set to ignore duplicates
				Set<Integer> snippetIDs= new LinkedHashSet<>();
				for (int ID : can.getSnippetIDs().toArray()) {
					snippetIDs.add(ID);
				}
				for (int ID : mainCan.getSnippetIDs().toArray()) {
					snippetIDs.add(ID);
				}
				//resize the snippetIDs array in mainCan and fill it in a for cycle
				mainCan.setSnippetIDs(new IntegerArray(resultView, snippetIDs.size()));
				int index = 0;
				for (Integer i: snippetIDs) {
					mainCan.setSnippetIDs(index, i);
					index++;
 				}
 				logger.debug("merging " + mainCan.getCoveredText() + "|" + can.getCoveredText());
				mainCanFV.merge(new AnswerFV(can));
				for (FeatureStructure af : can.getFeatures().toArray())
					((AnswerFeature) af).removeFromIndexes();
				can.removeFromIndexes();
			}
			mainCan.setFeatures(mainCanFV.toFSArray(resultView));
			mainCan.addToIndexes();
		}
	}
}
