package cz.brmlab.yodaqa.io.collection;

import java.util.regex.Pattern;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Math;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.flow.dashboard.Question;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;

/**
 * A consumer that displays the top answers in context of the asked
 * question and expected true answer provided as gold standard.
 *
 * Pair this with TSVQuestionReader or JSONQuestionReader e.g. on data/eval/.
 *
 * The output format is, tab separated
 * 	ID TIME QUESTION SCORE RANK NRANKS ANSWERPCRE CORRECTANSWER TOPANSWERS...
 * where TIME is the processing time in seconds (fractional),
 * SCORE is (1.0 - log(correctrank)/log(#answers)), RANK is
 * the corretrank and NRANKS is #answers.
 */

public class GoldStandardAnswerPrinter extends JCasConsumer_ImplBase {
	/**
	 * Number of top answers to show.
	 */
	public static final String PARAM_TOPLISTLEN = "TOPLISTLEN";
	@ConfigurationParameter(name = PARAM_TOPLISTLEN, mandatory = false, defaultValue = "15")
	private int topListLen;

	/**
	 * Number of top answers to show.
	 */
	public static final String PARAM_TSVFILE = "TSVFILE";
	@ConfigurationParameter(name = PARAM_TSVFILE, mandatory = true)
	private String TSVFile;
	PrintWriter TSVOutput;


	public synchronized void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);

		try {
			TSVOutput = new PrintWriter(TSVFile);
		} catch (IOException io) {
			throw new ResourceInitializationException(io);
		}
	}

	protected void output(QuestionInfo qi, double procTime,
			double score, int rank, int nranks,
			String aMatch, String... toplist)
	{
		String[] columns = new String[] {
			qi.getQuestionId(), Double.toString(procTime),
			qi.getQuestionText(),
			Double.toString(score),
			Integer.toString(rank), Integer.toString(nranks),
			qi.getAnswerPattern(), aMatch,
			Integer.toString(qi.getPassE_scored()),
			Integer.toString(qi.getPassE_gsscored()),
			Integer.toString(qi.getPassE_picked()),
			Integer.toString(qi.getPassE_gspicked()),
		};
		columns = (String[]) ArrayUtils.addAll(columns, toplist);

		String output = StringUtils.join(columns, "\t");
		/* Make sure no newlines were in the questions. */
		output = output.replace("\n", " ");

		System.out.println(output);
		TSVOutput.println(output);
		TSVOutput.flush();
	}

	public synchronized void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, answerHitlist;
		try {
			questionView = jcas.getView("Question");
			answerHitlist = jcas.getView("AnswerHitlist");
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		QuestionInfo qi = JCasUtil.selectSingle(questionView, QuestionInfo.class);
		Pattern ap = Pattern.compile(qi.getAnswerPattern(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		double procTime = (System.currentTimeMillis() - qi.getProcBeginTime()) / 1000.0;

		FSIndex idx = answerHitlist.getJFSIndexRepository().getIndex("SortedAnswers");
		FSIterator answers = idx.iterator();
		if (answers.hasNext()) {
			String[] toplist = new String[topListLen];
			int match = -1;
			String matchText = ".";

			int i = 0;
			while (answers.hasNext()) {
				Answer answer = (Answer) answers.next();
				String text = answer.getText();
				if (i < topListLen) {
					toplist[i] = text + ":" + answer.getConfidence();
				}
				if (match < 0 && ap.matcher(text).find()) {
					match = i;
					matchText = text;
				}
				i++;
			}

			double score = 0.0;
			if (match >= 0)
				score = 1.0 - Math.log(1 + match) / Math.log(i);

			output(qi, procTime, score, match, i, matchText, toplist);

		} else {
			/* Special case, no answer found. */
			output(qi, procTime, 0.0, 0, 0, ".");
		}

		Question q = QuestionDashboard.getInstance().get(qi.getQuestionId());
		// q.setAnswers(answers); XXX
		QuestionDashboard.getInstance().finishQuestion(q);
	}
}
