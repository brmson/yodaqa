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

import cz.brmlab.yodaqa.model.FinalAnswer.Answer;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;

/**
 * A consumer that displays the top answers in context of the asked
 * question and expected true answer provided as gold standard.
 *
 * Pair this with CollectionQuestionReader e.g. on data/trec/.
 *
 * The output format is, tab separated
 * 	ID QUESTION SCORE ANSWERPCRE CORRECTANSWER TOPANSWERS...
 * where SCORE is (1.0 - log(correctrank)/log(#answers))
 */

public class GoldStandardAnswerPrinter extends JCasConsumer_ImplBase {
	/**
	 * Number of top answers to show.
	 */
	public static final String PARAM_TOPLISTLEN = "TOPLISTLEN";
	@ConfigurationParameter(name = PARAM_TOPLISTLEN, mandatory = false, defaultValue = "5")
	private int topListLen;

	/**
	 * Number of top answers to show.
	 */
	public static final String PARAM_TSVFILE = "TSVFILE";
	@ConfigurationParameter(name = PARAM_TSVFILE, mandatory = true)
	private String TSVFile;
	PrintWriter TSVOutput;


	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);

		try {
			TSVOutput = new PrintWriter(TSVFile);
		} catch (IOException io) {
			throw new ResourceInitializationException(io);
		}
	}

	protected void output(String id, String qText, double score,
			String aPattern, String aMatch, String... toplist)
	{
		String[] columns = new String[] { id, qText, Double.toString(score), aPattern, aMatch };
		columns = (String[]) ArrayUtils.addAll(columns, toplist);

		System.out.println(StringUtils.join(columns, "\t"));
		TSVOutput.println(StringUtils.join(columns, "\t"));
		TSVOutput.flush();
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		QuestionInfo qi = JCasUtil.selectSingle(jcas, QuestionInfo.class);
		Pattern ap = Pattern.compile(qi.getAnswerPattern());

		FSIndex idx = jcas.getJFSIndexRepository().getIndex("SortedAnswers");
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

			output(qi.getQuestionId(), qi.getQuestionText(), score,
				qi.getAnswerPattern(), matchText, toplist);

		} else {
			/* Special case, no answer found. */
			output(qi.getQuestionId(), qi.getQuestionText(), 0.0,
				qi.getAnswerPattern(), ".");
		}
	}
}
