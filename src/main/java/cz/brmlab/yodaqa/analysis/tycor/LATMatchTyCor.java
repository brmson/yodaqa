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

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATANone;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATANoWordNet;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATQNoWordNet;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_NoTyCor;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_SpWordNet;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorADBp;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorADBpRelation;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorANE;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorAQuantity;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorAQuantityCD;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorAWnInstance;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorPassageDist;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorSpAHit;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorSpQHit;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.TyCor.DBpLAT;
import cz.brmlab.yodaqa.model.TyCor.DBpRelationLAT;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.NELAT;
import cz.brmlab.yodaqa.model.TyCor.QuantityLAT;
import cz.brmlab.yodaqa.model.TyCor.QuantityCDLAT;
import cz.brmlab.yodaqa.model.TyCor.WnInstanceLAT;
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

		public LAT getBaseLat1() {
			return _getBaseLat(getLat1());
		}
		public LAT getBaseLat2() {
			return _getBaseLat(getLat2());
		}

		protected LAT _getBaseLat(LAT lat) {
			while (lat.getBaseLAT() != null)
				lat = lat.getBaseLAT();
			return lat;
		}
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

		boolean qNoWordnetLAT = JCasUtil.select(questionView, WordnetLAT.class).isEmpty();
		boolean aNoWordnetLAT = JCasUtil.select(answerView, WordnetLAT.class).isEmpty();
		boolean aNoLAT = JCasUtil.select(answerView, LAT.class).isEmpty();
		LATMatch match = matchLATs(questionView, answerView);

		AnswerInfo ai = JCasUtil.selectSingle(answerView, AnswerInfo.class);
		AnswerFV fv = new AnswerFV(ai);

		if (match != null) {
			fv.setFeature(AF_SpWordNet.class, Math.exp(match.getSpecificity()));
			if (match.lat1.getSpecificity() == 0)
				fv.setFeature(AF_TyCorSpQHit.class, 1.0);

			if (match.lat2.getSpecificity() == 0) {
				fv.setFeature(AF_TyCorSpAHit.class, 1.0);

				LAT baselat2 = match.getBaseLat2();
				if (baselat2 instanceof NELAT)
					fv.setFeature(AF_TyCorANE.class, 1.0);
				else if (baselat2 instanceof DBpLAT)
					fv.setFeature(AF_TyCorADBp.class, 1.0);
				else if (baselat2 instanceof QuantityLAT)
					fv.setFeature(AF_TyCorAQuantity.class, 1.0);
				else if (baselat2 instanceof QuantityCDLAT)
					fv.setFeature(AF_TyCorAQuantityCD.class, 1.0);
				else if (baselat2 instanceof WnInstanceLAT)
					fv.setFeature(AF_TyCorAWnInstance.class, 1.0);
				else if (baselat2 instanceof DBpRelationLAT)
					fv.setFeature(AF_TyCorADBpRelation.class, 1.0);
				else assert(false);
			}

		/* We were the only ones doing type coercion here. */
		} else if (!fv.isFeatureSet(AF_TyCorPassageDist.class)) {
			/* XXX: Make the following a separate annotator?
			 * When we get another type coercion stage. */

			/* There is no LAT generated for this answer at all;
			 * a pretty interesting negative feature on its own? */
			if (aNoLAT) {
				logger.debug("no LAT for <<{}>>", answerView.getDocumentText());
				fv.setFeature(AF_LATANone.class, -1.0);

			/* There is no type coercion match, but wordnet LATs
			 * generated for both question and answer.  This means
			 * we are fairly sure this answer is of a wrong type. */
			} else if (!qNoWordnetLAT && !aNoWordnetLAT
				   && !fv.isFeatureSet(AF_TyCorPassageDist.class)) {
				logger.debug("failed TyCor for <<{}>>", answerView.getDocumentText());
				fv.setFeature(AF_NoTyCor.class, -1.0);
			}
		}

		if (qNoWordnetLAT)
			fv.setFeature(AF_LATQNoWordNet.class, 1.0);
		if (!aNoLAT && aNoWordnetLAT)
			fv.setFeature(AF_LATANoWordNet.class, 1.0);

		if (ai.getFeatures() != null)
			for (FeatureStructure af : ai.getFeatures().toArray())
				((AnswerFeature) af).removeFromIndexes();
		ai.removeFromIndexes();

		ai.setFeatures(fv.toFSArray(answerView));
		ai.addToIndexes();
	}

	protected LATMatch matchLATs(JCas questionView, JCas answerView) throws AnalysisEngineProcessException {
		Map<String, LAT> answerLats = new HashMap<String, LAT>();
		LATMatch bestMatch = null;

		/* FIXME: Allow matching LATs that have same text but
		 * different senses. */

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
			if (lq.getSynset() != 0 && la.getSynset() != 0 && lq.getSynset() != la.getSynset())
				continue;
			LATMatch match = new LATMatch(lq, la);
			if (bestMatch == null || match.getSpecificity() > bestMatch.getSpecificity())
				bestMatch = match;
		}

		if (bestMatch != null) {
			logger.debug(".. TyCor "
					+ bestMatch.getBaseLat1().getText() + "-" + bestMatch.getBaseLat2().getText()
					+ " match " + bestMatch.getLat1().getText() /* == LAT2 text */
					+ "/" + bestMatch.getLat1().getSynset()
					+ " sp. " + bestMatch.getSpecificity());
		}
		return bestMatch;
	}
}
