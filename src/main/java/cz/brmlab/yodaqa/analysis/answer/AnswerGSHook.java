package cz.brmlab.yodaqa.analysis.answer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;

/**
 * A GoldStandard hook in the process of answer extraction.  We scan all the
 * answers, match them against the answerPattern and dump model training data
 * if that is enabled on the commandline (see data/ml/README.md). */

public class AnswerGSHook extends JCasAnnotator_ImplBase {
	PrintWriter trainFile;

	final Logger logger = LoggerFactory.getLogger(AnswerGSHook.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		AnswerStats astats = new AnswerStats(jcas);

		for (Answer a : JCasUtil.select(jcas, Answer.class)) {
			logger.debug(a.getText() + ":" + a.getConfidence() + " -- " + Arrays.toString((new AnswerFV(a, astats)).getValues()));
		}

		QuestionInfo qi = JCasUtil.selectSingle(jcas, QuestionInfo.class);
		if (qi.getAnswerPattern() == null)
			return; // nothing to do, no gold standard
		Pattern ap = Pattern.compile(qi.getAnswerPattern(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

		/* Possibly dump model training data. */
		String trainFileName = System.getProperty("cz.brmlab.yodaqa.train_answer");
		if (trainFileName != null && !trainFileName.isEmpty()) {
			for (Answer a : JCasUtil.select(jcas, Answer.class)) {
				dumpAnswerFV(trainFileName, qi.getQuestionId(), a, ap.matcher(a.getText()).find(), astats);
			}
		}
	}

	protected void dumpAnswerFV(String trainFileName, String qid, Answer a, boolean isMatch, AnswerStats astats) {
		/* First, open the output file. */
		if (trainFile == null) {
			try {
				trainFile = new PrintWriter(trainFileName);
			} catch (IOException io) {
				io.printStackTrace();
			}

			StringBuilder sb = new StringBuilder();
			sb.append("qid");
			sb.append("\t");
			for (String label : AnswerFV.getFVLabels()) {
				sb.append(label);
				sb.append("\t");
			}
			sb.append("isMatch");
			trainFile.println(sb.toString());
			trainFile.flush();
		}

		AnswerFV fv = new AnswerFV(a, astats);

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
