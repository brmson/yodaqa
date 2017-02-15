package cz.brmlab.yodaqa.io.debug;

import cz.brmlab.yodaqa.analysis.rdf.FBPathGloVeScoring;
import cz.brmlab.yodaqa.analysis.rdf.FBPathLogistic;
import cz.brmlab.yodaqa.model.Question.*;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.provider.rdf.PropertyValue;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

/**
 * Created by honza on 18.2.16.
 */
public class ExploringPathPrinter extends JCasConsumer_ImplBase {

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
		String line = "{\"qId\": " + "\"" + qi.getQuestionId() + "\"" + ", ";
		line += "\"exploringPaths\":  [";
		FBPathGloVeScoring.getInstance().getPaths(questionView, Integer.MAX_VALUE);
		List<List<PropertyValue>> paths = FBPathGloVeScoring.getInstance().dump();
		int j = 0;
		for(List<PropertyValue> path: paths) {
			int i = 0;
			line += "[";
			for(PropertyValue pv: path) {
				line += "{";
				line += "\"property\": \"" + pv.getPropRes() + "\", ";
				line += "\"label\": \"" + pv.getProperty() + "\", ";
				line += "\"score\": " + pv.getScore();
				if (i + 1 == path.size()) line += "}";
				else line += "}, ";
				i++;
			}
			if (j + 1 == paths.size()) line += "]";
			else line += "], ";
			j++;
		}
		line += "]}";
		output(line);
		//Question q = QuestionDashboard.getInstance().get(qi.getQuestionId());
		//QuestionDashboard.getInstance().finishQuestion(q);
	}
}
