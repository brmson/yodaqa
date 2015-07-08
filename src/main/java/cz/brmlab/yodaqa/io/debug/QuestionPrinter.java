package cz.brmlab.yodaqa.io.debug;

import java.util.Iterator;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Math;

import cz.brmlab.yodaqa.model.Question.SV;
import cz.brmlab.yodaqa.model.TyCor.LAT;
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
 * A consumer that displays the questions with a LAT dump to json file
 * The format is:
 * {"qId": "...", "SV": ["...", "..."], "LAT" : [ {"synset" : "...", "text" : "...", "specificity" : "..." "type" : "..."}, {...}, {...}]} \n
 * Pair this with CollectionQuestionReader e.g. on data/eval/.
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
		String line = "{\"qId\": " + "\"" + qi.getQuestionId() + "\"" + ", " + "\"SV\": ";

		String SVtmp ="[";
		for (Iterator SVIterator = JCasUtil.select(jcas, SV.class).iterator(); SVIterator.hasNext(); ) {
			SV sv = (SV) SVIterator.next();
			SVtmp += "\"" + sv.getCoveredText() + "\"";
			if(SVIterator.hasNext()){
				SVtmp += ", ";
			}
		}
		SVtmp += "], ";
		line += SVtmp;
		line += "\"LAT\": [";
		String LATtmp = "";
		for (Iterator iterator = JCasUtil.select(jcas, LAT.class).iterator(); iterator.hasNext(); ) {
			LAT l = (LAT) iterator.next();
			/*{"synset" : "...", "text" : "...", "specificity" : "..." "type" : "..."}*/
			LATtmp += "{";
			if (l.getSynset() != 0) { //only add synset when it is not zero
				LATtmp += "\"synset\": " + "\"" + l.getSynset() + "\", ";
			}
			//add the rest
			LATtmp += "\"text\": \"" + l.getText() + "\"," + " \"specificity\": \"" + l.getSpecificity() + "\", " + "\"type\": " +
				"\"" + l.getClass().getSimpleName() + "\"}";
			//not last, add comma
			if (iterator.hasNext()) {
				LATtmp += ", ";
			}

		}
		LATtmp += "]}";
		line += LATtmp;
		output(line);
		//Question q = QuestionDashboard.getInstance().get(qi.getQuestionId());
		//QuestionDashboard.getInstance().finishQuestion(q);
	}
}
