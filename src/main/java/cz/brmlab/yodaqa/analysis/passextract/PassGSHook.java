package cz.brmlab.yodaqa.analysis.passextract;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;

import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.flow.dashboard.snippet.AnsweringPassage;
import cz.brmlab.yodaqa.flow.dashboard.snippet.AnsweringSnippet;
import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;
import cz.brmlab.yodaqa.model.SearchResult.PF_AboutClueWeight;
import cz.brmlab.yodaqa.model.SearchResult.PF_ClueWeight;
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
import cz.brmlab.yodaqa.model.SearchResult.Passage;

/**
 * A GoldStandard hook in the process of passage extraction.  We scan all the
 * passages, match them against the answerPattern and collect statistics.
 * Furthermore, we dump model training data if that is enabled on the
 * commandline (see data/ml/README.md). */

@SofaCapability(
	inputSofas = { "Question", "Passages", "PickedPassages" },
	outputSofas = { "Question" }
)


public class PassGSHook extends JCasAnnotator_ImplBase {
	PrintWriter trainFile;

	final Logger logger = LoggerFactory.getLogger(PassGSHook.class);

	public synchronized void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public synchronized void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, passagesView, pickedPassagesView;
		try {
			questionView = jcas.getView("Question");
			passagesView = jcas.getView("Passages");
			pickedPassagesView = jcas.getView("PickedPassages");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		QuestionInfo qi = JCasUtil.selectSingle(questionView, QuestionInfo.class);
		if (qi.getAnswerPattern() == null)
			return; // nothing to do, no gold standard
		Pattern ap = Pattern.compile(qi.getAnswerPattern(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

		/* Collect statistics on passages. */
		int n_scored = 0, n_gsscored = 0;
		for (Passage passage : JCasUtil.select(passagesView, Passage.class)) {
			n_scored += 1;
			if (ap.matcher(passage.getCoveredText()).find())
				n_gsscored += 1;
		}

		/* Collect statistics on picked passages. */
		int n_picked = 0, n_gspicked = 0;
		for (Passage passage : JCasUtil.select(pickedPassagesView, Passage.class)) {
			n_picked += 1;
			if (ap.matcher(passage.getCoveredText()).find())
				n_gspicked += 1;
		}

		/* Store the statistics in QuestionInfo. */
		qi.removeFromIndexes();
		qi.setPassE_scored(qi.getPassE_scored() + n_scored);
		qi.setPassE_gsscored(qi.getPassE_gsscored() + n_gsscored);
		qi.setPassE_picked(qi.getPassE_picked() + n_picked);
		qi.setPassE_gspicked(qi.getPassE_gspicked() + n_gspicked);
		qi.addToIndexes();

		/* Possibly dump model training data. */
		String trainFileName = System.getProperty("cz.brmlab.yodaqa.train_passextract");
		if (trainFileName != null && !trainFileName.isEmpty()) {
			for (Passage passage : JCasUtil.select(passagesView, Passage.class)) {
				dumpPassageFV(trainFileName, passage, ap.matcher(passage.getCoveredText()).find());
			}
			if (trainFile != null) {
				trainFile.println("");
				trainFile.flush();
			}
		}
		createJacanaFiles(passagesView,qi,ap);
	}


	/** Possibly create files for python training, one file per question. */
	protected synchronized void createJacanaFiles(JCas passagesView, QuestionInfo qi, Pattern ap){
//		System.setProperty("cz.brmlab.yodaqa.jacana","data/jacana");
		String jacana = System.getProperty("cz.brmlab.yodaqa.jacana");
		if (jacana == null || jacana.isEmpty())
			return;
		Writer w=Writer.getInstance();
		w.write(jacana + "/" + qi.getQuestionId() + ".txt", "<Q> "+qi.getQuestionText());
		for (Passage p : JCasUtil.select(passagesView, Passage.class)) {
			PassageFV fv = new PassageFV(p);
			int clueWeight_i = PassageFV.featureIndex(PF_ClueWeight.class);
			int aboutClueWeight_i = PassageFV.featureIndex(PF_AboutClueWeight.class);
			String clues=fv.getValues()[clueWeight_i]+" "+fv.getValues()[aboutClueWeight_i];
			String anspassage=p.getCoveredText();
				if(ap.matcher(anspassage).find()){
					w.write(jacana + "/" + qi.getQuestionId() + ".txt", "1 " +clues+" "+ anspassage);
				} else {
					w.write(jacana+"/"+qi.getQuestionId()+".txt","0 "+clues+" "+anspassage);
				}
			}
	}

	protected void dumpPassageFV(String trainFileName, Passage passage, boolean isMatch) {
		/* First, open the output file. */
		if (trainFile == null) {
			try {
				trainFile = new PrintWriter(trainFileName);
			} catch (IOException io) {
				io.printStackTrace();
			}
		}

		PassageFV fv = new PassageFV(passage);

		StringBuilder sb = new StringBuilder();
		for (double value : fv.getValues()) {
			sb.append(value);
			sb.append("\t");
		}

		sb.append(isMatch ? 1 : 0);
		trainFile.println(sb.toString());
		trainFile.flush();
	}
}

class Writer{
	private static Writer instance = new Writer();
	static Writer getInstance() {return instance;}
	synchronized void write(String filepath,String s){
		try {
			PrintWriter pw=new PrintWriter(new BufferedWriter(new FileWriter(filepath,true)));
			pw.println(s);
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}