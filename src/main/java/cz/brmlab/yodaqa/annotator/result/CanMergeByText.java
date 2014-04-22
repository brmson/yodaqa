package cz.brmlab.yodaqa.annotator.result;

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

import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;

/**
 * Merge CandidateAnswers whose coveredText is the same.
 *
 * The redundant annotations will be removed and their score added
 * to the kept one. */

public class CanMergeByText extends JCasAnnotator_ImplBase {
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
			for (CandidateAnswer can : entry.getValue()) {
				if (mainCan == null) {
					mainCan = can;
					mainCan.removeFromIndexes();
					continue;
				}
				System.err.println("merging " + mainCan.getCoveredText() + "|" + can.getCoveredText()
						+ " :: " + mainCan.getConfidence() + ", " + can.getConfidence());
				mainCan.setConfidence(mainCan.getConfidence() + can.getConfidence());
				can.removeFromIndexes();
			}
			mainCan.addToIndexes();
		}
	}
}
