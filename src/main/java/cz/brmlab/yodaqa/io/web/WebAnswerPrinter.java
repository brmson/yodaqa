package cz.brmlab.yodaqa.io.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.flow.dashboard.Question;
import cz.brmlab.yodaqa.flow.dashboard.QuestionAnswer;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;

/**
 * A consumer that records answers back in WebInterface.
 * <p/>
 * Pair this with WebQuestionReader.
 */

public class WebAnswerPrinter extends JCasConsumer_ImplBase {
	final Logger logger = LoggerFactory.getLogger(WebAnswerPrinter.class);

	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, answerHitlist;
		try {
			questionView = jcas.getView("Question");
			answerHitlist = jcas.getView("AnswerHitlist");
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
		QuestionInfo qi = JCasUtil.selectSingle(questionView, QuestionInfo.class);
		FSIndex idx = answerHitlist.getJFSIndexRepository().getIndex("SortedAnswers");
		FSIterator answerit = idx.iterator();
		List<QuestionAnswer> answers = new ArrayList<>();
		while (answerit.hasNext()) {
			Answer a = ((Answer) answerit.next());
			QuestionAnswer qa = new QuestionAnswer(a.getText(), a.getConfidence(), a.getAnswerID());
			if (a.getSnippetIDs() != null) { //AnsweringSnippet ID should never be null
				for (Integer PassageID : a.getSnippetIDs().toArray()) {
					qa.addToSnippetIDList(PassageID);
				}
			}
			answers.add(qa);
		}
		Question q = QuestionDashboard.getInstance().get(qi.getQuestionId());
		q.setAnswers(answers);
		QuestionDashboard.getInstance().finishQuestion(q);
		logger.debug("FINAL: {}", q.toJson());
	}
}
