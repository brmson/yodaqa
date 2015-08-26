package cz.brmlab.yodaqa.analysis.answer;

import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.Question.QuestionClass;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates features according to the question class. There is one feature for each possible class.
 * If the question belongs to the corresponding class, the feature is set to 1, otherwise 0.
 * Each question belongs exactly to one class so only one feature is set to 1.
 */
public class QuestionClassFeatures extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(QuestionClassFeatures.class);

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		JCas questionView, answerView;
		try {
			questionView = jCas.getView("Question");
			answerView = jCas.getView("Answer");
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		QuestionClass qc = JCasUtil.selectSingle(questionView, QuestionClass.class);
		String cls = qc.getQuestionClass();

		AnswerInfo ai = JCasUtil.selectSingle(answerView, AnswerInfo.class);
		AnswerFV fv = new AnswerFV(ai);
		fv.setFeature(AF.QuestionClass_ClassName + cls, 1.0);

		if (ai.getFeatures() != null)
			for (FeatureStructure af : ai.getFeatures().toArray())
				((AnswerFeature) af).removeFromIndexes();
		ai.removeFromIndexes();
		ai.setFeatures(fv.toFSArray(jCas));
		ai.addToIndexes();
	}
}
