package cz.brmlab.yodaqa.analysis.answer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
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
 * if that is enabled on the commandline (see data/ml/README.md).
 *
 * If cz.brmlab.yodaqa.csv_answer property is set, we also create (in given
 * directory) one CSV file per question with a list of all answers and their
 * features.
 *
 * Append "1" to the property name (e.g. cz.brmlab.yodaqa.train_answer1)
 * for scoring in the second scoring phrase (after evidence gathering). */

public class AnswerGSHook extends JCasAnnotator_ImplBase {
	PrintWriter trainFile;

	final Logger logger = LoggerFactory.getLogger(AnswerGSHook.class);

	/**
	 * Pipeline phase in which we are scoring.  We may be scoring
	 * multiple times and will use different property names to
	 * differentiate the models.
	 */
	public static final String PARAM_SCORING_PHASE = "SCORING_PHASE";
	@ConfigurationParameter(name = PARAM_SCORING_PHASE, mandatory = true)
	protected String scoringPhase;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, answerHitlist;
		try {
			questionView = jcas.getView("Question");
			answerHitlist = jcas.getView("AnswerHitlist");
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		AnswerStats astats = new AnswerStats(answerHitlist);

		logger.debug("------------------------------------------------");
		for (Answer a : JCasUtil.select(answerHitlist, Answer.class)) {
			logger.debug(a.getText() + ":" + a.getConfidence() + " -- " + Arrays.toString((new AnswerFV(a, astats)).getValues()));
		}

		QuestionInfo qi = JCasUtil.selectSingle(questionView, QuestionInfo.class);
		Pattern ap = null;
		if (qi.getAnswerPattern() != null) {
			ap = Pattern.compile(qi.getAnswerPattern(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		}

		/* Possibly dump CSV data on answers, one file per question. */
		String csvDirName = System.getProperty("cz.brmlab.yodaqa.csv_answer" + scoringPhase);
		if (csvDirName != null && !csvDirName.isEmpty()) {
			(new File(csvDirName)).mkdir();
			String csvFileName = csvDirName + "/" + qi.getQuestionId() + ".csv";
			PrintWriter csvFile = openAnswersCSV(csvFileName);
			for (Answer a : JCasUtil.select(answerHitlist, Answer.class)) {
				dumpAnswerCSV(csvFile, a, ap != null ? ap.matcher(a.getText()).find() : false, astats);
			}
		}

		/* Possibly dump model training data.  We also require gold
		 * standard for this, otherwise there is no training to do. */
		String trainFileName = System.getProperty("cz.brmlab.yodaqa.train_answer" + scoringPhase);
		if (ap != null && trainFileName != null && !trainFileName.isEmpty()) {
			for (Answer a : JCasUtil.select(answerHitlist, Answer.class)) {
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

	protected PrintWriter openAnswersCSV(String csvFileName)
			throws AnalysisEngineProcessException {
		/* First, open the output file. */
		PrintWriter csvFile = null;
		try {
			csvFile = new PrintWriter(csvFileName);
		} catch (IOException io) {
			throw new AnalysisEngineProcessException(io);
		}

		/* Write out the header. */
		StringBuilder sb = new StringBuilder();
		sb.append("answer,");
		sb.append("iM,");
		int i = 0;
		for (String label : AnswerFV.getFVLabels()) {
			/* Consider only primary values in the FV. */
			if (i % 3 == 0) {
				sb.append(label);
				sb.append(",");
			}
			i++;
		}
		csvFile.println(sb.toString());
		csvFile.flush();

		return csvFile;
	}

	protected void dumpAnswerCSV(PrintWriter csvFile, Answer a, boolean isMatch, AnswerStats astats) {
		AnswerFV fv = new AnswerFV(a, astats);

		StringBuilder sb = new StringBuilder();
		sb.append("\"");
		sb.append(a.getText().replaceAll("\"", "\\\""));
		sb.append("\"");
		sb.append(",");
		sb.append(isMatch ? "+" : "-");
		sb.append(",");
		int i = 0;
		for (double value : fv.getFV()) {
			if (i % 3 == 0) {
				sb.append(value);
				sb.append(",");
			}
			i++;
		}

		csvFile.println(sb.toString());
		csvFile.flush();
	}
}
