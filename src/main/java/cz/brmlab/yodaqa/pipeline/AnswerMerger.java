package cz.brmlab.yodaqa.pipeline;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.component.JCasMultiplier_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.FinalAnswer.Answer;

/**
 * Take a set of per-answer CandidateAnswerCAS and merge them to
 * a FinalAnswerCAS.
 *
 * The "ranking" part is actually implicit by UIMA indexes, this
 * is mainly a merging CAS multiplier that also deduplicates answers
 * with the same text. */

public class AnswerMerger extends JCasMultiplier_ImplBase {
	final Logger logger = LoggerFactory.getLogger(AnswerMerger.class);

	Map<String, List<Answer>> answersByText;
	JCas finalCas;
	boolean isFirst, isLast;

	protected void reset() {
		answersByText = new HashMap<String, List<Answer>>();
		finalCas = null;
		isLast = false;
		isFirst = true;
	}

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		reset();
	}

	public void process(JCas canCas) throws AnalysisEngineProcessException {
		JCas canQuestion, canAnswer;
		try {
			canQuestion = canCas.getView("Question");
			canAnswer = canCas.getView("Answer");
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		if (finalCas == null)
			finalCas = getEmptyJCas();

		if (isFirst) {
			QuestionInfo qi = JCasUtil.selectSingle(canQuestion, QuestionInfo.class);
			/* Copy QuestionInfo */
			CasCopier copier = new CasCopier(canQuestion.getCas(), finalCas.getCas());
			QuestionInfo qi2 = (QuestionInfo) copier.copyFs(qi);
			qi2.addToIndexes();
			isFirst = false;
		}

		AnswerInfo ai = JCasUtil.selectSingle(canAnswer, AnswerInfo.class);
		ResultInfo ri = JCasUtil.selectSingle(canAnswer, ResultInfo.class);
		isLast = ai.getIsLast() && ri.getIsLast();

		Answer answer = new Answer(finalCas);
		String text = canAnswer.getDocumentText();
		answer.setText(text);
		answer.setConfidence(
				Math.exp(ai.getSpecificity())
				* ai.getConfidence()
				* ai.getPassageScore()
				* ri.getRelevance());

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
				logger.debug("final merging " + mainAns.getText() + "|" + answer.getText()
						+ " :: " + mainAns.getConfidence() + ", " + answer.getConfidence());
				mainAns.setConfidence(mainAns.getConfidence() + answer.getConfidence());
			}
			mainAns.addToIndexes();
		}

		JCas outputCas = finalCas;
		reset();
		return outputCas;
	}
}
