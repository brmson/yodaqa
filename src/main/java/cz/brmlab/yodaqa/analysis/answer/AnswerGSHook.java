package cz.brmlab.yodaqa.analysis.answer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;

/**
 * A GoldStandard hook in the process of answer extraction.  We scan all the
 * answers, match them against the answerPattern and dump model training data
 * if that is enabled on the commandline (see data/ml/README.md). */

@SofaCapability(
	inputSofas = { "Question", "Answer" }
)


public class AnswerGSHook extends JCasAnnotator_ImplBase {
	protected static String trainFileName = "training-answer.tsv";
	PrintWriter trainFile;

	final Logger logger = LoggerFactory.getLogger(AnswerGSHook.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, answerView;
		try {
			questionView = jcas.getView("Question");
			answerView = jcas.getView("Answer");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		for (AnswerInfo ai : JCasUtil.select(answerView, AnswerInfo.class)) {
			logger.debug(answerView.getDocumentText() + ":" + ai.getConfidence() + " -- " + Arrays.toString((new AnswerFV(ai)).getValues()));
		}

		QuestionInfo qi = JCasUtil.selectSingle(questionView, QuestionInfo.class);
		if (qi.getAnswerPattern() == null)
			return; // nothing to do, no gold standard
		Pattern ap = Pattern.compile(qi.getAnswerPattern(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

		/* Possibly dump model training data. */
		String mltraining = System.getProperty("cz.brmlab.yodaqa.mltraining");
		if (mltraining != null && mltraining.equals("1")) {
			for (AnswerInfo ai : JCasUtil.select(answerView, AnswerInfo.class)) {
				dumpAnswerFV(qi.getQuestionId(), ai, ap.matcher(answerView.getDocumentText()).find());
			}
		}
	}

	protected void dumpAnswerFV(String qid, AnswerInfo ai, boolean isMatch) {
		/* First, open the output file. */
		if (trainFile == null) {
			try {
				trainFile = new PrintWriter(trainFileName);
			} catch (IOException io) {
				io.printStackTrace();
			}
		}

		AnswerFV fv = new AnswerFV(ai);

		StringBuilder sb = new StringBuilder();
		sb.append(qid);
		sb.append("\t");
		for (double value : fv.getFV()) {
			sb.append(value);
			sb.append("\t");
		}

		sb.append(isMatch ? 1 : 0);
		trainFile.println(sb.toString());
		trainFile.flush();
	}
}
