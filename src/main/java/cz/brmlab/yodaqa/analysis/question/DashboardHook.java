package cz.brmlab.yodaqa.analysis.question;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.flow.dashboard.QuestionConcept;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.flow.dashboard.QuestionSummary;
import cz.brmlab.yodaqa.model.Question.ClueConcept;
import cz.brmlab.yodaqa.model.TyCor.LAT;

/**
 * Summarize question analysis results in the question dashboard.
 * This is used to report ongoing progress through user interfaces. */

public class DashboardHook extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(DashboardHook.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas resultView) throws AnalysisEngineProcessException {
		List<String> lats = new ArrayList<>();
		for (LAT lat : JCasUtil.select(resultView, LAT.class)) {
			lats.add(lat.getText());
		}

		List<QuestionConcept> concepts = new ArrayList<>();
		for (ClueConcept conceptClue : JCasUtil.select(resultView, ClueConcept.class)) {
			QuestionConcept c = new QuestionConcept(conceptClue.getLabel(), conceptClue.getPageID());
			concepts.add(c);
		}

		QuestionSummary qs = new QuestionSummary(lats, concepts);
		QuestionDashboard.getInstance().get(resultView).setSummary(qs);
	}
}
