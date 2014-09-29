package cz.brmlab.yodaqa.analysis.tycor;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.answer.AnswerFV;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_SpWordNet;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.WordnetLAT;

/**
 * Estimate answer specificity in CandidateAnswerCAS via type coercion
 * by question LAT to answer LAT matching. We simply try to find the
 * most specific LAT match. */

public class LATMatchTyCor extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATMatchTyCor.class);

	protected class LATMatch {
		public LAT lat1, lat2;
		public double specificity;

		public LATMatch(LAT lat1_, LAT lat2_) {
			lat1 = lat1_;
			lat2 = lat2_;
			specificity = lat1.getSpecificity() + lat2.getSpecificity();
		}

		public LAT getLat1() { return lat1; }
		public LAT getLat2() { return lat2; }
		public double getSpecificity() { return specificity; }
	}

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, answerView;
		try {
			questionView = jcas.getView("Question");
			answerView = jcas.getView("Answer");
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		LATMatch match = matchLATs(questionView, answerView);
		if (match != null) {
			AnswerInfo ai = JCasUtil.selectSingle(answerView, AnswerInfo.class);
			AnswerFV fv = new AnswerFV(ai);
			fv.setFeature(AF_SpWordNet.class, Math.exp(match.getSpecificity()));

			for (FeatureStructure af : ai.getFeatures().toArray())
				((AnswerFeature) af).removeFromIndexes();
			ai.removeFromIndexes();

			ai.setFeatures(fv.toFSArray(answerView));
			ai.addToIndexes();
		}
	}

	protected LATMatch matchLATs(JCas questionView, JCas answerView) throws AnalysisEngineProcessException {
		Map<String, LAT> answerLats = new HashMap<String, LAT>();
		LATMatch bestMatch = null;

		/* Load LATs from answerView. */
		for (LAT la : JCasUtil.select(answerView, LAT.class)) {
			if (la.getIsHierarchical() && !(la instanceof WordnetLAT))
				continue;
			LAT la0 = answerLats.get(la.getText());
			if (la0 == null || la0.getSpecificity() < la.getSpecificity())
				answerLats.put(la.getText(), la);
		}
		if (answerLats.isEmpty())
			return null;

		/* Match LATs from questionView. */
		for (LAT lq : JCasUtil.select(questionView, LAT.class)) {
			if (lq.getIsHierarchical() && !(lq instanceof WordnetLAT))
				continue;
			LAT la = answerLats.get(lq.getText());
			if (la == null)
				continue;
			LATMatch match = new LATMatch(lq, la);
			if (bestMatch == null || match.getSpecificity() > bestMatch.getSpecificity())
				bestMatch = match;
		}

		if (bestMatch != null)
			logger.debug(".. TyCor "
					+ bestMatch.getLat1().getBase().getCoveredText()
					+ "-" + bestMatch.getLat2().getBase().getCoveredText()
					+ " LAT " + bestMatch.getLat1().getText()
					+ " sp. " + bestMatch.getSpecificity());
		return bestMatch;
	}
}
