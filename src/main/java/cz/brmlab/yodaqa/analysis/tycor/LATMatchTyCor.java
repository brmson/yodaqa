package cz.brmlab.yodaqa.analysis.tycor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.TyCor.DBpLAT;
import cz.brmlab.yodaqa.model.TyCor.DBpOntologyLAT;
import cz.brmlab.yodaqa.model.TyCor.DBpPropertyLAT;
import cz.brmlab.yodaqa.model.TyCor.FBOntologyLAT;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.NELAT;
import cz.brmlab.yodaqa.model.TyCor.QuantityLAT;
import cz.brmlab.yodaqa.model.TyCor.QuantityCDLAT;
import cz.brmlab.yodaqa.model.TyCor.WnInstanceLAT;
import cz.brmlab.yodaqa.model.TyCor.WordnetLAT;

/**
 * Estimate answer specificity in CandidateAnswerCAS via type coercion
 * by question LAT to answer LAT matching.
 *
 * Basically, we try to find any matches of answer LAT set with the
 * question LAT set, and generate AFs accordingly.  The crucial aspect
 * is the mesh of LATWordnets that we pre-generated and that represent
 * various generalizations of the (hopefully specific) LATs we
 * generated earlier.
 *
 * There are many ways to approach this.  We allow generalizations
 * of both the question and answer LATs to match, because e.g. when
 * asking about a "site" of Lindbergh's flight, "New York" will be
 * generated as a "city" and the match is "location".  But clearly
 * direct ('hit') matches are better than 'sibling' matches. */

public class LATMatchTyCor extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATMatchTyCor.class);

	/** A single match of a (question, answer) LAT tuple.  The match
	 * has a total specificity (sum of constituent specificities). */
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

		public void logMatch(Logger logger, String prefix) {
			logger.debug(prefix + " "
					+ getBaseLat1().getText() + "-" + getBaseLat2().getText()
					+ " match " + getLat1().getText() /* == LAT2 text */
					+ "/" + getLat1().getSynset()
					+ " sp. " + getSpecificity()
					+ " (q " + getLat1().getSpecificity() + ", a " + getLat2().getSpecificity() + ")");
		}

		/** Record details on this LATMatch in the given AnswerFV
		 * (and log it too).  This is worth calling for all LATMatches
		 * that are hits for at least one side. */
		public void record(AnswerFV fv, String ansText) {
			if (lat1.getSpecificity() == 0)
				fv.setFeature(AF.TyCorSpQHit, 1.0);

			if (lat2.getSpecificity() == 0)
				fv.setFeature(AF.TyCorSpAHit, 1.0);

			if (lat1.getSpecificity() == 0 || lat2.getSpecificity() == 0) {
				/* Generate a TyCor if this has been a direct
				 * match from at least one direction (i.e. not
				 * just a "semantic sibling"). */
				LAT baselat2 = getBaseLat2();
				if (baselat2 instanceof NELAT)
					fv.setFeature(AF.TyCorANE, 1.0);
				else if (baselat2 instanceof DBpLAT)
					fv.setFeature(AF.TyCorADBp, 1.0);
				else if (baselat2 instanceof QuantityLAT)
					fv.setFeature(AF.TyCorAQuantity, 1.0);
				else if (baselat2 instanceof QuantityCDLAT)
					fv.setFeature(AF.TyCorAQuantityCD, 1.0);
				else if (baselat2 instanceof WnInstanceLAT)
					fv.setFeature(AF.TyCorAWnInstance, 1.0);
				else if (baselat2 instanceof DBpOntologyLAT)
					fv.setFeature(AF.TyCorADBpOntology, 1.0);
				else if (baselat2 instanceof DBpPropertyLAT)
					fv.setFeature(AF.TyCorADBpProperty, 1.0);
				else if (baselat2 instanceof FBOntologyLAT)
					fv.setFeature(AF.TyCorAFBOntology, 1.0);
				else assert(false);
			}

			if (lat1.getSpecificity() == 0 && lat2.getSpecificity() == 0) {
				/* If both LATs match sharp, that's a good
				 * sign OTOH. */
				logger.debug("sharp LAT match for <<{}>>", ansText);
				fv.setFeature(AF.TyCorSpQAHit, 1.0);
			} else {
				/* Fuzzy match, just produce a debug print
				 * as well for grep's sake. */
				if (lat1.getSpecificity() == 0) {
					logger.debug("q-hit LAT match for <<{}>>", ansText);
				} else if (lat2.getSpecificity() == 0) {
					logger.debug("a-hit LAT match for <<{}>>", ansText);
				}
			}
		}
	}

	/* Synset blacklist - this blacklist will not permit using these
	 * LATWordnet LATs for generalization.  Therefore, "language"
	 * will not match "area" through "cognition" and "time" will
	 * not match "region" via "measure".
	 *
	 * This covers only matching LATWordnet pairs!  So when
	 * LATByQuantity generates "measure" answer LAT, this will still
	 * match all measure-derived question LATs.
	 *
	 * N.B. this is a generalization limit applied in addition to the
	 * LATByWordnet Tops synset list.
	 *
	 * XXX: Compiled manually by cursory logs investigation.  We should
	 * build a TyCor dataset and train it by that. */
	protected static Long wnwn_synsetbl_list[] = {
		/* communication/ */ 33319L,
		/* cognition/ */ 23451L,
		/* ability/ */ 5624029L,
		/* higher cognitive process/ */ 5778661L,
		/* relation/ */ 32220L,
		/* ability/ */ 5624029L,
		/* measure/ */ 33914L,
		/* instrumentality/ */ 3580409L,
		/* artifact/ */ 22119L,
		/* fundamental quantity/ */ 13597072L,
		/* organization/ */ 8024893L,
		/* group/ */ 31563L,
		/* unit/ */ 8206589L,
		/* attribute/ */ 24444L,
		/* trait/ */ 4623416L,
		/* device/ */ 3187746L,
		/* social group/ */ 7967506L,
		/* act/ */ 30657L,
		/* activity/ */ 408356L,
		/* state/ */ 24900L,
		/* extent/ */ 5130681L,
		/* magnitude/ */ 5097645L,
		/* organism/ */ 4475L,
		/* creation/ */ 3133774L,
		/* product/ */ 4014270L,
		/* abstraction/ */ 2137L,
		/* medium/ */ 6264799L,
		/* gathering/ */ 7991473L,
		/* idea/ */ 5842164L,
		/* kind/ */ 5847533L,
		/* property/ */ 4923519L,
		/* quality/ */ 4731092L,
		/* concept/ */ 5844071L,
		/* thing/ */ 2452L,
		/* possession/ */ 32912L,
		/* happening/ */ 7298313L,
		/* signal/ */ 6804229L,
		/* auditory communication/ */ 7123727L,
		/* event/ */ 29677L,
		/* group/ */ 31563L,
	};
	protected Set<Long> wnwn_synsetbl;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		if (wnwn_synsetbl == null)
			wnwn_synsetbl = new HashSet<Long>(Arrays.asList(wnwn_synsetbl_list));

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

		boolean qNoWordnetLAT = !hasWordnetLAT(questionView);
		boolean aNoWordnetLAT = !hasWordnetLAT(answerView);
		boolean aNoLAT = JCasUtil.select(answerView, LAT.class).isEmpty();

		AnswerInfo ai = JCasUtil.selectSingle(answerView, AnswerInfo.class);
		AnswerFV fv = new AnswerFV(ai);

		/* Find the best match.  Note that this will also
		 * process and generate features for other nice
		 * (hit) matches encountered on the way. */
		LATMatch match = matchLATs(questionView, answerView, fv);

		if (match != null) {
			fv.setFeature(AF.SpWordNet, Math.exp(match.getSpecificity()));

		/* We were the only ones doing type coercion here. */
		} else if (!fv.isFeatureSet(AF.TyCorPassageDist)) {
			/* XXX: Make the following a separate annotator?
			 * When we get another type coercion stage. */

			/* There is no LAT generated for this answer at all;
			 * a pretty interesting negative feature on its own? */
			if (aNoLAT) {
				logger.debug("no LAT for <<{}>>", answerView.getDocumentText());
				fv.setFeature(AF.LATANone, -1.0);

			/* There is no type coercion match, but wordnet LATs
			 * generated for both question and answer.  This means
			 * we are fairly sure this answer is of a wrong type. */
			} else if (!qNoWordnetLAT && !aNoWordnetLAT
				   && !fv.isFeatureSet(AF.TyCorPassageDist)) {
				logger.debug("failed TyCor for <<{}>>", answerView.getDocumentText());
				fv.setFeature(AF.NoTyCor, -1.0);
			}
		}

		if (qNoWordnetLAT)
			fv.setFeature(AF.LATQNoWordNet, -1.0);
		if (!aNoLAT && aNoWordnetLAT)
			fv.setFeature(AF.LATANoWordNet, -1.0);

		if (ai.getFeatures() != null)
			for (FeatureStructure af : ai.getFeatures().toArray())
				((AnswerFeature) af).removeFromIndexes();
		ai.removeFromIndexes();

		ai.setFeatures(fv.toFSArray(answerView));
		ai.addToIndexes();
	}

	/** Check whether a given CAS (view) contains an LAT featureset
	 * associated with a WordNet object.  This is an indicator that
	 * there is some "non-weird" LAT generated. */
	protected boolean hasWordnetLAT(JCas jcas) {
		/* This is not a simple matter of checking for WordnetLAT
		 * as we may have some other LAT with synset associated and
		 * non-expanded to WordnetLATs. */
		for (LAT lat : JCasUtil.select(jcas, LAT.class))
			if (lat.getSynset() != 0)
				return true;
		return false;
	}

	protected LATMatch matchLATs(JCas questionView, JCas answerView, AnswerFV fv)
			throws AnalysisEngineProcessException {
		Map<String, LAT> answerLats = new HashMap<String, LAT>();
		LATMatch bestMatch = null;
		int hits = 0;

		/* FIXME: Allow matching LATs that have same text but
		 * different senses.
		 * XXX: For now, we just prefer the numerically lowest
		 * synset, which doesn't make much sense but at least
		 * keeps the results stable across runs (LAT select order
		 * is very random). */

		/* Load LATs from answerView. */
		for (LAT la : JCasUtil.select(answerView, LAT.class)) {
			if (la.getIsHierarchical() && !(la instanceof WordnetLAT))
				continue;
			LAT la0 = answerLats.get(la.getText());
			if (la0 == null
			    || la.getSpecificity() > la0.getSpecificity()
			    || (la.getSpecificity() == la0.getSpecificity() && la.getSynset() < la0.getSynset()))
				answerLats.put(la.getText(), la);
		}
		if (answerLats.isEmpty())
			return null;

		/* Match LATs from questionView. */
		for (LAT lq : JCasUtil.select(questionView, LAT.class)) {
			if (lq.getIsHierarchical() && !(lq instanceof WordnetLAT)) {
				continue;
			}
			LAT la = answerLats.get(lq.getText());
			if (la == null) {
				continue;
			}
			if (lq.getSynset() != 0 && la.getSynset() != 0 && lq.getSynset() != la.getSynset()) {
				continue;
			}

			/* We have a match! */
			LATMatch match = new LATMatch(lq, la);
			// match.logMatch(logger, " maybe ");

			if (match.getLat1().getSpecificity() == 0 || match.getLat2().getSpecificity() == 0) {
				/* A hit match too!  Record it right away.
				 * (We may encounter a variety of these. */
				match.logMatch(logger, ".. TyCor hit");
				match.record(fv, answerView.getDocumentText());
				hits++;
			}

			/* N.B. Even with specificity equal, we sort by answer
			 * specificity, because the order of LATs may be random
			 * and these LAT specificities decide hit features;
			 * this turns out to be important for experiment
			 * reproducibility... */
			if (bestMatch == null
			    || match.getSpecificity() > bestMatch.getSpecificity()
			    || (match.getSpecificity() == bestMatch.getSpecificity() && match.getLat2().getSpecificity() > bestMatch.getLat2().getSpecificity())) {
				/* XXX: Technically, we should apply the
				 * blacklist check for match.record() above
				 * as well; in practice, we should never get
				 * hit matches on the WordnetLATs. */
				if (match.getLat1() instanceof WordnetLAT
				    && match.getLat2() instanceof WordnetLAT
				    && wnwn_synsetbl.contains(match.getLat1().getSynset())) {
					match.logMatch(logger, ".. ignoring blacklisted TyCor");
					continue;
				}
				bestMatch = match;
			}
		}

		if (bestMatch != null) {
			bestMatch.logMatch(logger, ".. TyCor best");
			if (hits == 0) {
				/* If we had to generalize both LATs, that
				 * seems to be a negative signal that the
				 * answer is less specifit than we want. */
				logger.debug("generalizing both LATs for <<{}>>", answerView.getDocumentText());
				fv.setFeature(AF.TyCorSpNoHit, -1.0);
			} else if (hits > 0) {
				/* Multiple hits, that's a positive signal
				 * hopefully indicating strong evidence. */
				logger.debug("multi-hit LAT match for <<{}>>", answerView.getDocumentText());
				fv.setFeature(AF.TyCorSpMultiHit, 1.0);
			}
		}
		return bestMatch;
	}
}
