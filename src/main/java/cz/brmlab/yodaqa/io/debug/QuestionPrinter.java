package cz.brmlab.yodaqa.io.debug;

import java.util.Iterator;
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
import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.Question.SV;
import cz.brmlab.yodaqa.model.TyCor.LAT;

/**
 * A consumer that displays the questions with a LAT dump to json file
 * The format is:
 * {"qId": "...", "SV": ["...", "..."], "LAT" : [ {"synset" : "...", "text" : "...", "specificity" : "..." "type" : "..."}, {...}, {...}]} \n
 * Pair this with TSVQuestionReader or JSONQuestionReader e.g. on data/eval/.
 */

public class QuestionPrinter extends JCasConsumer_ImplBase {

	public static final String PARAM_JSONFILE = "JSONFILE";
	@ConfigurationParameter(name = PARAM_JSONFILE, mandatory = true)
		private String JSONFile;
	PrintWriter JSONOutput;


	public synchronized void initialize(UimaContext context)
		throws ResourceInitializationException {
		super.initialize(context);

		try {
			JSONOutput = new PrintWriter(JSONFile);
		} catch (IOException io) {
			throw new ResourceInitializationException(io);
		}
	}

	protected void output(String res)
	{
		//System.out.println(res);
		JSONOutput.println(res);
		JSONOutput.flush();
	}

	public synchronized void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView;
		try {
			questionView = jcas;
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		QuestionInfo qi = JCasUtil.selectSingle(questionView, QuestionInfo.class);
		/*{"qId": "...", "sv": "...", "LAT" : [ {...}, {...}, {...}]} */
		String line = "{\"qId\": " + "\"" + qi.getQuestionId() + "\"" + ", ";
		line += "\"qText\": " + "\"" +qi.getQuestionText() + "\"" +", ";
		line += "\"Concept\": ";
		String Concepttmp = "[";
		for (Iterator iterator = JCasUtil.select(jcas, Concept.class).iterator(); iterator.hasNext(); ) {
			Concept c = (Concept) iterator.next();
			Concepttmp += "{";
			Concepttmp += "\"fullLabel\": \"" + c.getFullLabel().replaceAll("\"", "\\\\\"") + "\", ";
			Concepttmp += "\"cookedLabel\": \"" + c.getCookedLabel().replaceAll("\"", "\\\\\"") + "\", ";
			Concepttmp += "\"pageID\": \"" + c.getPageID() + "\", ";
			Concepttmp += "\"editDist\": \"" + c.getEditDistance() + "\", ";
			Concepttmp += "\"probability\": \"" + c.getProbability() + "\", ";
			Concepttmp += "\"score\": \"" + c.getScore() + "\", ";
			Concepttmp += "\"getByLAT\": \"" + (c.getByLAT() ? 1 : 0) + "\", ";
			Concepttmp += "\"getByNE\": \"" + (c.getByNE() ? 1 : 0) + "\", ";
			Concepttmp += "\"getBySubject\": \"" + (c.getBySubject() ? 1 : 0) + "\"";
			Concepttmp += "}";
			//not last, add comma
			if (iterator.hasNext()) {
				Concepttmp += ", ";
			}
		}
		Concepttmp += "] ";
		line += Concepttmp;

		line += "}";
		output(line);
		//Question q = QuestionDashboard.getInstance().get(qi.getQuestionId());
		//QuestionDashboard.getInstance().finishQuestion(q);
	}
}
