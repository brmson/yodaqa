package cz.brmlab.yodaqa.analysis.ansscore;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;
import java.util.Locale;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.TyCor.DBpLAT;
import cz.brmlab.yodaqa.model.TyCor.DBpOntologyLAT;
import cz.brmlab.yodaqa.model.TyCor.DBpPropertyLAT;
import cz.brmlab.yodaqa.model.TyCor.FBOntologyLAT;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.NELAT;
import cz.brmlab.yodaqa.model.TyCor.QuantityCDLAT;
import cz.brmlab.yodaqa.model.TyCor.QuantityLAT;
import cz.brmlab.yodaqa.model.TyCor.WnInstanceLAT;
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

	/**
	 * List of LAT categories for CSV dumping purposes.
	 * FIXME: Deduplicate with other places where we list these. */
	protected static final String[] LATLabels = {
		"NELAT", "DBpLAT",
		"QuantityLAT", "QuantityCDLAT",
		"WnInstanceLAT",
		"DBpOntologyLAT", "DBpPropertyLAT", "FBOntologyLAT",
	};
	protected static final Class[] LATClasses = {
		NELAT.class, DBpLAT.class,
		QuantityLAT.class, QuantityCDLAT.class,
		WnInstanceLAT.class,
		DBpOntologyLAT.class, DBpPropertyLAT.class, FBOntologyLAT.class,
	};

	public synchronized void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public synchronized void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, answerHitlist;
		try {
			questionView = jcas.getView("Question");
			answerHitlist = jcas.getView("AnswerHitlist");
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		AnswerStats astats = new AnswerStats(answerHitlist);

		logger.debug("------------------------------------------------");
		/*
		for (Answer a : JCasUtil.select(answerHitlist, Answer.class)) {
			logger.debug(a.getText() + ":" + a.getConfidence() + " -- " + Arrays.toString((new AnswerFV(a, astats)).getValues()));
		}
		*/

		QuestionInfo qi = JCasUtil.selectSingle(questionView, QuestionInfo.class);
		Pattern ap = null;
		if (qi.getAnswerPattern() != null) {
			ap = Pattern.compile(qi.getAnswerPattern(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		}

		dumpQuestionCSV(answerHitlist, qi, ap, astats);
		dumpQuestionFV(answerHitlist, qi, ap, astats);
	}

	/** Possibly dump CSV data on answers, one file per question. */
	protected void dumpQuestionCSV(JCas answerHitlist, QuestionInfo qi, Pattern ap, AnswerStats astats)
			throws AnalysisEngineProcessException {
		String csvDirName = System.getProperty("cz.brmlab.yodaqa.csv_answer" + scoringPhase);
		if (csvDirName == null || csvDirName.isEmpty())
			return;

		(new File(csvDirName)).mkdir();
		String csvFileName = csvDirName + "/" + qi.getQuestionId() + ".csv";
		PrintWriter csvFile = openAnswersCSV(csvFileName);
		for (Answer a : JCasUtil.select(answerHitlist, Answer.class)) {
			dumpAnswerCSV(csvFile, a, ap != null ? ap.matcher(a.getText()).find() : false, astats);
		}
	}

	/** Possibly dump model training data.  We also require gold
	 * standard for this, otherwise there is no training to do. */
	protected void dumpQuestionFV(JCas answerHitlist, QuestionInfo qi, Pattern ap, AnswerStats astats)
			throws AnalysisEngineProcessException {
		String trainFileName = System.getProperty("cz.brmlab.yodaqa.train_answer" + scoringPhase);
		if (ap == null || trainFileName == null || trainFileName.isEmpty())
			return;

		/* It turns out to make sense to include only a single best
		 * correct answer in the training set.  We find and store this
		 * in refAnswer and skip all correct (matching) answers that
		 * are not refAnswer.
		 *
		 * To decide between multiple possible answers, we use the one
		 * with the highest score from the previous phase; use all
		 * correct answers in the initial phase.  If we used the
		 * current model, it would not be stable across re-trainings on
		 * identical program version as the selected correct answers
		 * would keep changing; it turns out this introduces
		 * a significant instability and (i) the performance degrades
		 * (ii) it is very tricky to measure improvements.
		 *
		 * We also prefer correct answers which have lower score but
		 * are contained in other correct answers.  Basically, we first
		 * pick the answer with the highest score, then try to reduce
		 * this answer (by substring relations) even at the cost of
		 * lowering the score too.  The motivation is that shorter
		 * answers are more precise and will therefore have a more
		 * determining set of features to take for training. */
		String refAnswer = getReferenceAnswer(answerHitlist, ap, astats);

		/* We also do not train our model on any answer of a question
		 * that yields no correct answer - because should we really
		 * give a negative bias to all of the answer candidates? */
		if (refAnswer == null) {
			logger.debug("not including any answers, no correct answer");
			return;
		}

		/* Pass all correct answers in the initial scoring phase. */
		if (scoringPhase.equals(""))
			refAnswer = null;

		FSIndex idx = answerHitlist.getJFSIndexRepository().getIndex("SortedAnswers");
		FSIterator answers = idx.iterator();
		while (answers.hasNext()) {
			Answer a = (Answer) answers.next();
			boolean isMatch = ap.matcher(a.getText()).find();
			if (isMatch && refAnswer != null && !refAnswer.equals(a.getText())) {
				logger.debug("not including correct answer: {} < {}", a.getText(), refAnswer);
				continue; // output only the top positive match
			}
			dumpAnswerFV(trainFileName, qi.getQuestionId(), a, isMatch, astats);
		}
	}

	protected String getReferenceAnswer(JCas answerHitlist, Pattern ap, AnswerStats astats) {
		/* First, pick the best scored answer. */
		Answer bestA = null;
		double bestAScore = 0;
		for (Answer a : JCasUtil.select(answerHitlist, Answer.class)) {
			if (!ap.matcher(a.getText()).find())
				continue;

			double score = a.getConfidence();
			if (score > bestAScore) {
				bestA = a;
				bestAScore = score;
			}
		}
		if (bestA == null)
			return null;  // no luck

		/* Now, check again if we can find a shorter but still
		 * correct version of the best answer.  If so, take
		 * the shortest one. */
		Answer bestShorterA = bestA;
		for (Answer a : JCasUtil.select(answerHitlist, Answer.class)) {
			if (!ap.matcher(a.getText()).find())
				continue;
			if (!bestA.getText().contains(a.getText()))
				continue;
			if (bestShorterA.getText().length() < a.getText().length())
				continue;
			if (bestShorterA.getText().length() == a.getText().length()
			    && bestShorterA.getConfidence() > a.getConfidence())
				continue;  // tie-breaker is score
			bestShorterA = a;
		}

		return bestShorterA.getText();
	}

	protected void dumpAnswerFV(String trainFileName, String qid, Answer a, boolean isMatch, AnswerStats astats) {
		String[] labels = AnswerFV.getFVLabels();
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
			for (String label : labels) {
				if (featureBlacklisted(label))
					continue;
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
		int i = -1;
		for (double value : fv.getFV()) {
			i++;
			if (featureBlacklisted(labels[i]))
				continue;
			sb.append(String.format(Locale.ENGLISH, "%.4f", value));
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
		sb.append("confidence,");
		sb.append("focus,");
		for (String label : LATLabels) {
			sb.append(label);
			sb.append(",");
		}
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
		sb.append(a.getText().replaceAll("\"", "\"\""));
		sb.append("\"");
		sb.append(",");
		sb.append(isMatch ? "+" : "-");
		sb.append(",");
		sb.append(a.getConfidence());
		sb.append(",");
		if (a.getFocus() != null) {
			sb.append("\"");
			sb.append(a.getFocus().replaceAll("\"", "\"\""));
			sb.append("\"");
		}
		sb.append(",");
		for (Class LATClass : LATClasses) {
			boolean isFirst = true;
			for (FeatureStructure latfs : a.getLats().toArray()) {
				if (!LATClass.isInstance(latfs))
					continue;
				LAT lat = (LAT) latfs;
				if (isFirst) {
					sb.append("\"");
					isFirst = false;
				} else {
					sb.append(";");
				}
				sb.append(lat.getText().replaceAll("\"", "\"\""));
				if (lat.getSynset() != 0) {
					sb.append("/");
					sb.append(lat.getSynset());
				}
			}
			if (!isFirst)
				sb.append("\"");
			sb.append(",");
		}
		int i = 0;
		for (double value : fv.getFV()) {
			if (i % 3 == 0) {
				sb.append(String.format(Locale.ENGLISH, "%.4f", value));
				sb.append(",");
			}
			i++;
		}

		csvFile.println(sb.toString());
		csvFile.flush();
	}

	/* Do not include some features in the dumped feature vector.
	 * These features are blacklisted because they occur too rarely
	 * and therefore are liable to get overfitted.
	 *
	 * This list has been obtained by:
	 *
	 * data/ml/answer-countfv.py data/eval/curated-train.tsv data/eval/answer-csv/ffd67ef/ >/tmp/ffd67ef.fts
	 * egrep '!! .$|!. !$' /tmp/ffd67ef.fts | tr -d @ | awk '{print$1}' | egrep -v '^evd|^topAnswer|^phase|^solr' | sed 's/^.*$/"&",/'
	 * (the first egrep filters for at least two !s (second ! implies
	 * first !), the second egrep prevents removal of non-phase0 features)
	 */
	/* XXX: This is a gross hack the way this is done now.
	 * TODO: Also, this should be automated. */
	String featureBlacklist[] = {
		"originPsgByClueSubjectNE",
		"originDBpOClueToken",
		"originDBpOCluePhrase",
		"originDBpOClueSV",
		"originDBpOClueNE",
		"originDBpOClueSubjectNE",
		"originDBpOClueSubjectPhrase",
		"originDBpPCluePhrase",
		"originDBpPClueSV",
		"originDBpPClueNE",
		"originDBpPClueSubjectNE",
		"originDBpPClueSubjectPhrase",
		"originFBOCluePhrase",
		"originFBOClueNE",
		"originFBOClueSubjectNE",
		"originFBOClueSubjectPhrase",
		"psgDistClueSubjectNE",
		"tyCorPassageSp",
		"tyCorPassageDist",
		"tyCorPassageInside",
		"tyCorADBpWN",
		"tyCorAQuantity",
	};

	protected boolean featureBlacklisted(String label) {
		String fName = label.substring(1); // drop the prefix char
		for (String f : featureBlacklist)
			if (f.equals(fName))
				return true;
		return false;
	}
}
