package cz.brmlab.yodaqa.pipeline;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasMultiplier_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.FinalAnswer.Answer;

/**
 * Take a set of per-answer CAS and merge them to a final result CAS.
 *
 * So far, this answer ranker is super-primitive, just to showcase
 * a merging CAS multiplier behavior. But it actually does the job.
 * It also deduplicates answers with the same text. */

public class AnswerRanker extends JCasMultiplier_ImplBase {
	Map<String, List<Answer>> answersByText;
	JCas finalCas;
	boolean isLast;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		answersByText = new HashMap<String, List<Answer>>();
		isLast = false;
	}

	public void process(JCas candCas) throws AnalysisEngineProcessException {
		if (finalCas == null)
			finalCas = getEmptyJCas();

		Answer answer = new Answer(finalCas);
		String text = candCas.getDocumentText();
		answer.setText(text);

		AnswerInfo ai;
		ResultInfo ri;
		FSIterator infos;

		ai = (AnswerInfo) candCas.getJFSIndexRepository().getAllIndexedFS(AnswerInfo.type).next();
		ri = (ResultInfo) candCas.getJFSIndexRepository().getAllIndexedFS(ResultInfo.type).next();

		answer.setConfidence(ai.getConfidence() * ri.getRelevance());
		isLast = ai.getIsLast() && ri.getIsLast();

		// System.err.println("AR process: " + answer.getText() + " c " + answer.getConfidence());

		List<Answer> answers = answersByText.get(text);
		if (answers == null) {
			answers = new LinkedList<Answer>();
			answersByText.put(text, answers);
		}
		answers.add(answer);
	}

	public boolean hasNext() throws AnalysisEngineProcessException {
		return isLast;
	}

	public AbstractCas next() throws AnalysisEngineProcessException {
		if (!isLast)
			throw new AnalysisEngineProcessException();

		/* Deduplicate Answer objects and index them. */
		for (Entry<String, List<Answer>> entry : answersByText.entrySet()) {
			Answer mainAns = null;
			for (Answer answer : entry.getValue()) {
				if (mainAns == null) {
					mainAns = answer;
					continue;
				}
				System.err.println("final merging " + mainAns.getText() + "|" + answer.getText()
						+ " :: " + mainAns.getConfidence() + ", " + answer.getConfidence());
				mainAns.setConfidence(mainAns.getConfidence() + answer.getConfidence());
			}
			mainAns.addToIndexes();
		}

		JCas outputCas = finalCas;
		finalCas = null;
		answersByText = null;
		isLast = false;
		return outputCas;
	}
}
