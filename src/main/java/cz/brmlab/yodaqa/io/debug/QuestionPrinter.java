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
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.Question.ClueConcept;
import cz.brmlab.yodaqa.model.Question.ClueLAT;
import cz.brmlab.yodaqa.model.Question.ClueSV;
import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.Question.SV;
import cz.brmlab.yodaqa.model.Question.Subject;
import cz.brmlab.yodaqa.model.TyCor.LAT;

/**
 * A consumer that displays the questions with a LAT dump to json file.
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
		String SVtmp = "\"SV\":  [";
		for (Iterator SVIterator = JCasUtil.select(jcas, SV.class).iterator(); SVIterator.hasNext(); ) {
			SV sv = (SV) SVIterator.next();
			SVtmp += "\"" + sv.getCoveredText() + "\"";
			if(SVIterator.hasNext()){
				SVtmp += ", ";
			}
		}
		SVtmp += "], ";
		line += SVtmp;

		String lemmaSVtmp = "\"lemmaSV\":  [";
		for (Iterator SVIterator = JCasUtil.select(jcas, SV.class).iterator(); SVIterator.hasNext(); ) {
			SV sv = (SV) SVIterator.next();
			lemmaSVtmp += "\"" + sv.getBase().getLemma().getValue() + "\"";
			if(SVIterator.hasNext()){
				lemmaSVtmp += ", ";
			}
		}
		lemmaSVtmp += "], ";
		line += lemmaSVtmp;

		line += "\"LAT\": ";
		String LATtmp = "[";
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
		LATtmp += "], ";
		line += LATtmp;

		String Subjecttmp = "\"Subject\":  [";
		for (Iterator SubjectIterator = JCasUtil.select(jcas, Subject.class).iterator(); SubjectIterator.hasNext(); ) {
			Subject subj = (Subject) SubjectIterator.next();
			Subjecttmp += "{\"text\": \"" + subj.getCoveredText() + "\", \"type\": \"" + subj.getBase().getClass().getSimpleName() + "\"}";
			if (SubjectIterator.hasNext()){
				Subjecttmp += ", ";
			}
		}
		Subjecttmp += "], ";
		line += Subjecttmp;

		line += "\"Concept\": ";
		String Concepttmp = "[";
		for (Iterator iterator = JCasUtil.select(jcas, Concept.class).iterator(); iterator.hasNext(); ) {
			Concept c = (Concept) iterator.next();
			Concepttmp += "{";
			Concepttmp += "\"fullLabel\": \"" + c.getFullLabel().replaceAll("\"", "\\\\\"") + "\", ";
			Concepttmp += "\"cookedLabel\": \"" + c.getCookedLabel().replaceAll("\"", "\\\\\"") + "\", ";
			Concepttmp += "\"pageID\": \"" + c.getPageID() + "\", ";
			Concepttmp += "\"editDist\": " + c.getEditDistance() + ", ";
			Concepttmp += "\"labelProbability\": " + c.getLabelProbability() + ", ";
			Concepttmp += "\"logPopularity\": " + c.getLogPopularity() + ", ";
			Concepttmp += "\"score\": " + c.getScore() + ", ";
			if (c.getDescription() != null){
				Concepttmp += "\"description\": \"" + c.getDescription().replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\", ";
			}
			Concepttmp += "\"relatedness\": " + c.getRelatedness() + ", ";
			Concepttmp += "\"getByLAT\": " + (c.getByLAT() ? 1 : 0) + ", ";
			Concepttmp += "\"getByNE\": " + (c.getByNE() ? 1 : 0) + ", ";
			Concepttmp += "\"getBySubject\": " + (c.getBySubject() ? 1 : 0) + ", ";
			Concepttmp += "\"getByNgram\": " + (c.getByNgram() ? 1 : 0) + ", ";
			Concepttmp += "\"getByFuzzyLookup\": " + (c.getByFuzzyLookup() ? 1 : 0) + ", ";
			Concepttmp += "\"getByCWLookup\": " + (c.getByCWLookup() ? 1 : 0) + "";
			Concepttmp += "}";
			if (iterator.hasNext()) {
				//not last, add comma
				Concepttmp += ", ";
			}
		}
		Concepttmp += "], ";
		line += Concepttmp;

		line += "\"Clue\": ";
		String Cluetmp = "[";
		boolean first = true;
		for (Iterator iterator = JCasUtil.select(jcas, Clue.class).iterator(); iterator.hasNext(); ) {
			Clue c = (Clue) iterator.next();
			if (c instanceof ClueConcept || c instanceof ClueSV || c instanceof ClueLAT)
				continue; // covered earlier
			if (!first) {
				Cluetmp += ", ";
			} else {
				first = false;
			}
			Cluetmp += "{";
			Cluetmp += "\"label\": \"" + c.getLabel().replaceAll("\"", "\\\\\"") + "\", ";
			Cluetmp += "\"type\": \"" + c.getClass().getSimpleName() + "\", ";
			Cluetmp += "\"weight\": " + c.getWeight() + "";
			Cluetmp += "}";
		}
		Cluetmp += "] ";
		line += Cluetmp;

		line += "}";
		output(line);
		//Question q = QuestionDashboard.getInstance().get(qi.getQuestionId());
		//QuestionDashboard.getInstance().finishQuestion(q);
	}
}
