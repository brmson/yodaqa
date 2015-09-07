package cz.brmlab.yodaqa.analysis.passage.biotagger;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.passage.CandidateGenerator;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.SearchResult.AnswerBioMention;
import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;

/**
 * Create CandidateAnswer based on AnswerMention pre-annotations.
 * We split this to a separate annotator and use the AnswerMention
 * intermediate annotations to ease integration with ClearTK. */

@SofaCapability(
	inputSofas = { "Question", "Passage" },
	outputSofas = { "Passage" }
)

public class CanByAnsBioMention extends CandidateGenerator {
	public CanByAnsBioMention() {
		logger = LoggerFactory.getLogger(CanByAnsBioMention.class);
	}


	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas passagesView;
		try {
			passagesView = jcas.getView("Passage");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		ResultInfo ri = JCasUtil.selectSingle(passagesView, ResultInfo.class);

		for (AnswerBioMention abm : JCasUtil.select(passagesView, AnswerBioMention.class))
			processMention(passagesView, ri, abm);
	}

	protected void processMention(JCas passagesView, ResultInfo ri, AnswerBioMention abm)
			throws AnalysisEngineProcessException {
		Passage p = JCasUtil.selectCovering(Passage.class, abm).get(0);

		AnswerFV fv = new AnswerFV(ri.getAnsfeatures());
		fv.merge(new AnswerFV(p.getAnsfeatures()));
		fv.setFeature(AF.OriginPsgBIO, 1.0);
		fv.setFeature(AF.BIOScore, abm.getScore());
		logger.debug("can <<{}>> score <<{}>>", abm.getCoveredText(), abm.getScore());

		addCandidateAnswer(passagesView, p, abm, fv);
	}
}
