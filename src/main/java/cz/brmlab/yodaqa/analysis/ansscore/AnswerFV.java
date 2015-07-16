package cz.brmlab.yodaqa.analysis.ansscore;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import cz.brmlab.yodaqa.model.CandidateAnswer.*;
import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;
import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;

/**
 * A CandidateAnswer feature vector.  I.e. a single, fixed array where each
 * index represents one feature and the indices are stable between answers.
 *
 * Originally, this class has been intended mainly as an interface between
 * answer features and machine learned models. However, we also use it
 * whenever we need to manipulate a set of features at once - when initializing
 * a feature set and when merging feature sets of different answers. */


public class AnswerFV {
	public static ArrayList<Class<? extends AnswerFeature>> features;
	public static String labels[] = {
		"occurences", "resultRR", "resultLogScore", "passageLogScore",
		"originPsg", "originPsgFirst",
		"originPsgByClueToken", "originPsgByCluePhrase", "originPsgByClueSV",
			"originPsgByClueNE", "originPsgByClueLAT",
			"originPsgByClueSubject", "originPsgByClueSubjectNE",
			"originPsgByClueSubjectToken", "originPsgByClueSubjectPhrase",
			"originPsgByClueConcept",
		"originPsgNP", "originPsgNE", "originPsgNPByLATSubj",
			"originPsgBIO",
		"originPsgSurprise",
		"originDocTitle",
		"originBingSnippet",
		"originDBpOntology", "originDBpONoClue",
		"originDBpOClueToken", "originDBpOCluePhrase", "originDBpOClueSV",
			"originDBpOClueNE", "originDBpOClueLAT",
			"originDBpOClueSubject", "originDBpOClueSubjectNE",
			"originDBpOClueSubjectToken", "originDBpOClueSubjectPhrase",
			"originDBpOClueConcept",
		"originDBpProperty", "originDBpPNoClue",
		"originDBpPClueToken", "originDBpPCluePhrase", "originDBpPClueSV",
			"originDBpPClueNE", "originDBpPClueLAT",
			"originDBpPClueSubject", "originDBpPClueSubjectNE",
			"originDBpPClueSubjectToken", "originDBpPClueSubjectPhrase",
			"originDBpPClueConcept",
		"originFreebaseOntology", "originFBONoClue",
		"originFBOClueToken", "originFBOCluePhrase", "originFBOClueSV",
			"originFBOClueNE", "originFBOClueLAT",
			"originFBOClueSubject", "originFBOClueSubjectNE",
			"originFBOClueSubjectToken", "originFBOClueSubjectPhrase",
			"originFBOClueConcept",
		"originConcept", "originConceptBySubject", "originConceptByLAT", "originConceptByNE",
		"originMultiple",
		"AF_PsgDistClueToken", "AF_PsgDistCluePhrase", "AF_PsgDistClueSV",
			"AF_PsgDistClueNE", "AF_PsgDistClueLAT",
			"AF_PsgDistClueSubject", "AF_PsgDistClueSubjectNE",
			"AF_PsgDistClueSubjectToken", "AF_PsgDistClueSubjectPhrase",
			"AF_PsgDistClueConcept",
		"bioScore",
		"noTyCor", "LATANone",
                "spWordNet", "LATQNoWordNet", "LATANoWordNet",
		"tyCorPassageSp", "tyCorPassageDist", "tyCorPassageInside",
		"simpleScore",
		"LATNE", "LATDBpType", "LATDBpWNType", "LATQuantity", "LATQuantityCD",
			"LATWnInstance", "LATDBpOntology", "LATDBpProperty", "LATFBOntology",
		"tyCorSpQHit", "tyCorSpAHit", "tyCorSpNoHit", "tyCorSpQAHit", "tyCorSpMultiHit",
		"tyCorANE", "tyCorADBp", "tyCorADBpWN", "tyCorAQuantity", "tyCorAQuantityCD",
			"tyCorAWnInstance", "tyCorADBpOntology", "tyCorADBpProperty", "tyCorAFBOntology",
		"clOCMatchScore", "clOCPrefixedScore", "clOCPrefixingScore", "clOCSuffixedScore",
			"clOCSuffixingScore", "clOCSubstredScore", "clOCSubstringScore",
			"clOCMetaMatchScore",
		"clOMatchScore", "clOPrefixedScore", "clOPrefixingScore", "clOSuffixedScore",
			"clOSuffixingScore", "clOSubstredScore", "clOSubstringScore",
			"clOMetaMatchScore",
		"evdPrefixedScore", "evdPrefixingScore", "evdSuffixedScore", "evdSuffixingScore",
			"evdSubstredScore", "evdSubstringScore",
		"topAnswer", "solrHitsEv", "solrAHitsEv", "solrHitsANormEv", "solrMaxScoreEv",
			"solrHitsMaxScoreEv",
		"phase0Score", "phase1Score",
	};

	protected double values[]; // the feature value
	protected boolean isSet[]; // whether the feature is set
	protected AnswerStats astats;

	public AnswerFV() {
		/* This is a huge mess - seems like static initializer of
		 * the array list is impossible to do. */
		if (features == null) {
			features = new ArrayList<Class<? extends AnswerFeature>>();
			features.add(AF_Occurences.class);
			features.add(AF_ResultRR.class);
			features.add(AF_ResultLogScore.class);
			features.add(AF_PassageLogScore.class);
			features.add(AF_OriginPsg.class);
			features.add(AF_OriginPsgFirst.class);
			features.add(AF_OriginPsgByClueToken.class);
			features.add(AF_OriginPsgByCluePhrase.class);
			features.add(AF_OriginPsgByClueSV.class);
			features.add(AF_OriginPsgByClueNE.class);
			features.add(AF_OriginPsgByClueLAT.class);
			features.add(AF_OriginPsgByClueSubject.class);
			features.add(AF_OriginPsgByClueSubjectNE.class);
			features.add(AF_OriginPsgByClueSubjectToken.class);
			features.add(AF_OriginPsgByClueSubjectPhrase.class);
			features.add(AF_OriginPsgByClueConcept.class);
			features.add(AF_OriginPsgNP.class);
			features.add(AF_OriginPsgNE.class);
			features.add(AF_OriginPsgNPByLATSubj.class);
			features.add(AF_OriginPsgBIO.class);
			features.add(AF_OriginPsgSurprise.class);
			features.add(AF_OriginDocTitle.class);
			features.add(AF_OriginBingSnippet.class);
			features.add(AF_OriginDBpOntology.class);
			features.add(AF_OriginDBpONoClue.class);
			features.add(AF_OriginDBpOClueToken.class);
			features.add(AF_OriginDBpOCluePhrase.class);
			features.add(AF_OriginDBpOClueSV.class);
			features.add(AF_OriginDBpOClueNE.class);
			features.add(AF_OriginDBpOClueLAT.class);
			features.add(AF_OriginDBpOClueSubject.class);
			features.add(AF_OriginDBpOClueSubjectNE.class);
			features.add(AF_OriginDBpOClueSubjectToken.class);
			features.add(AF_OriginDBpOClueSubjectPhrase.class);
			features.add(AF_OriginDBpOClueConcept.class);
			features.add(AF_OriginDBpProperty.class);
			features.add(AF_OriginDBpPNoClue.class);
			features.add(AF_OriginDBpPClueToken.class);
			features.add(AF_OriginDBpPCluePhrase.class);
			features.add(AF_OriginDBpPClueSV.class);
			features.add(AF_OriginDBpPClueNE.class);
			features.add(AF_OriginDBpPClueLAT.class);
			features.add(AF_OriginDBpPClueSubject.class);
			features.add(AF_OriginDBpPClueSubjectNE.class);
			features.add(AF_OriginDBpPClueSubjectToken.class);
			features.add(AF_OriginDBpPClueSubjectPhrase.class);
			features.add(AF_OriginDBpPClueConcept.class);
			features.add(AF_OriginFreebaseOntology.class);
			features.add(AF_OriginFBONoClue.class);
			features.add(AF_OriginFBOClueToken.class);
			features.add(AF_OriginFBOCluePhrase.class);
			features.add(AF_OriginFBOClueSV.class);
			features.add(AF_OriginFBOClueNE.class);
			features.add(AF_OriginFBOClueLAT.class);
			features.add(AF_OriginFBOClueSubject.class);
			features.add(AF_OriginFBOClueSubjectNE.class);
			features.add(AF_OriginFBOClueSubjectToken.class);
			features.add(AF_OriginFBOClueSubjectPhrase.class);
			features.add(AF_OriginFBOClueConcept.class);
			features.add(AF_OriginConcept.class);
			features.add(AF_OriginConceptBySubject.class);
			features.add(AF_OriginConceptByLAT.class);
			features.add(AF_OriginConceptByNE.class);
			features.add(AF_OriginMultiple.class);
			features.add(AF_PsgDistClueToken.class);
			features.add(AF_PsgDistCluePhrase.class);
			features.add(AF_PsgDistClueSV.class);
			features.add(AF_PsgDistClueNE.class);
			features.add(AF_PsgDistClueLAT.class);
			features.add(AF_PsgDistClueSubject.class);
			features.add(AF_PsgDistClueSubjectNE.class);
			features.add(AF_PsgDistClueSubjectToken.class);
			features.add(AF_PsgDistClueSubjectPhrase.class);
			features.add(AF_PsgDistClueConcept.class);
			features.add(AF_BIOScore.class);
			features.add(AF_NoTyCor.class);
			features.add(AF_LATANone.class);
			features.add(AF_SpWordNet.class);
			features.add(AF_LATQNoWordNet.class);
			features.add(AF_LATANoWordNet.class);
			features.add(AF_TyCorPassageSp.class);
			features.add(AF_TyCorPassageDist.class);
			features.add(AF_TyCorPassageInside.class);
			features.add(AF_SimpleScore.class);
			features.add(AF_LATNE.class);
			features.add(AF_LATDBpType.class);
			features.add(AF_LATDBpWNType.class);
			features.add(AF_LATQuantity.class);
			features.add(AF_LATQuantityCD.class);
			features.add(AF_LATWnInstance.class);
			features.add(AF_LATDBpOntology.class);
			features.add(AF_LATDBpProperty.class);
			features.add(AF_LATFBOntology.class);
			features.add(AF_TyCorSpQHit.class);
			features.add(AF_TyCorSpAHit.class);
			features.add(AF_TyCorSpNoHit.class);
			features.add(AF_TyCorSpQAHit.class);
			features.add(AF_TyCorSpMultiHit.class);
			features.add(AF_TyCorANE.class);
			features.add(AF_TyCorADBp.class);
			features.add(AF_TyCorADBpWN.class);
			features.add(AF_TyCorAQuantity.class);
			features.add(AF_TyCorAQuantityCD.class);
			features.add(AF_TyCorAWnInstance.class);
			features.add(AF_TyCorADBpOntology.class);
			features.add(AF_TyCorADBpProperty.class);
			features.add(AF_TyCorAFBOntology.class);
			features.add(AF_ClOCMatchScore.class);
			features.add(AF_ClOCPrefixedScore.class);
			features.add(AF_ClOCPrefixingScore.class);
			features.add(AF_ClOCSuffixedScore.class);
			features.add(AF_ClOCSuffixingScore.class);
			features.add(AF_ClOCSubstredScore.class);
			features.add(AF_ClOCSubstringScore.class);
			features.add(AF_ClOCMetaMatchScore.class);
			features.add(AF_ClOMatchScore.class);
			features.add(AF_ClOPrefixedScore.class);
			features.add(AF_ClOPrefixingScore.class);
			features.add(AF_ClOSuffixedScore.class);
			features.add(AF_ClOSuffixingScore.class);
			features.add(AF_ClOSubstredScore.class);
			features.add(AF_ClOSubstringScore.class);
			features.add(AF_ClOMetaMatchScore.class);
			features.add(AF_EvDPrefixedScore.class);
			features.add(AF_EvDPrefixingScore.class);
			features.add(AF_EvDSuffixedScore.class);
			features.add(AF_EvDSuffixingScore.class);
			features.add(AF_EvDSubstredScore.class);
			features.add(AF_EvDSubstringScore.class);
			features.add(AF_TopAnswer.class);
			features.add(AF_SolrHitsEv.class);
			features.add(AF_SolrAHitsEv.class);
			features.add(AF_SolrHitsANormEv.class);
			features.add(AF_SolrMaxScoreEv.class);
			features.add(AF_SolrHitsMaxScoreEv.class);
			features.add(AF_Phase0Score.class);
			features.add(AF_Phase1Score.class);
		}

		values = new double[labels.length];
		isSet = new boolean[labels.length];
	}

	public AnswerFV(FSArray fsarray) {
		this();

		if (fsarray == null)
			return;
		for (FeatureStructure fs : fsarray.toArray()) {
			AnswerFeature af = (AnswerFeature) fs;
			if (setFeature(af.getClass(), af.getValue())) {
				int index = features.indexOf(af.getClass());
				throw new RuntimeException("Duplicate answer feature " + labels[index] + "=" + values[index] + " in " + fsarray);
			}
		}
	}

	public AnswerFV(AnswerInfo ai) {
		this(ai.getFeatures());
	}

	public AnswerFV(CandidateAnswer ca) {
		this(ca.getFeatures());
	}

	public AnswerFV(Answer a) {
		this(a.getFeatures());
	}

	public AnswerFV(Answer a, AnswerStats astats_) {
		this(a);
		astats = astats_;
	}


	/** Return a list of values. Note that for model input, you should
	 * rather use getFV(). */
	public double[] getValues() {
		return values;
	}

	public boolean[] getIsSet() {
		return isSet;
	}

	public static ArrayList<Class<? extends AnswerFeature>> getFeatures() {
		return features;
	}

	public static int featureIndex(Class<? extends AnswerFeature> f) {
		return features.indexOf(f);
	}

	public static int featureIndex(String label) {
		return ArrayUtils.indexOf(labels, label);
	}


	/** Produce a feature vector. For each value, this vector includes:
	 *
	 * - the feature value, as set
	 * - the feature value, normalized (centered to mean at zero, rescaled
	 *   to unit SD)
	 * - indicator element that is 0 if the feature is set, 1 otherwise
	 *   (so that omission of a feature can be taken as a signal by itself)
	 *
	 * More may appear in the future. */
	public double[] getFV() {
		double[] fv = new double[labels.length * 3];
		for (int i = 0; i < labels.length; i++) {
			fv[i*3] = values[i];
			if (astats != null && astats.sd[i] != 0) {
				fv[i*3 + 1] = (values[i] - astats.mean[i]) / astats.sd[i];
			} else {
				fv[i*3 + 1] = 0.0;
			}
			fv[i*3 + 2] = isSet[i] ? 0.0 : 1.0;
		}
		return fv;
	}

	/** Produce a label vector corresponding to the feature vector,
	 * as returned by getFV(). */
	public static String[] getFVLabels() {
		String[] FVlabels = new String[labels.length * 3];
		for (int i = 0; i < labels.length; i++) {
			FVlabels[i*3] = "@" + labels[i];
			FVlabels[i*3 + 1] = "%" + labels[i];
			FVlabels[i*3 + 2] = "!" + labels[i];
		}
		return FVlabels;
	}


	/** Set a given feature to a value.  Return whether the feature
	 * has already been set before. */
	public boolean setFeature(Class<? extends AnswerFeature> f, double value) {
		int index = features.indexOf(f);
		assert(index >= 0);

		boolean hasBeenSet = isSet[index];

		values[index] = value;
		isSet[index] = true;

		return hasBeenSet;
	}

	public double getFeatureValue(Class<? extends AnswerFeature> f) {
		return values[featureIndex(f)];
	}
	public boolean isFeatureSet(Class<? extends AnswerFeature> f) {
		return isSet[featureIndex(f)];
	}

	/** Merge the a2fv AnswerFV to this AnswerFV.  Individual features
	 * may have various policy of how this is done - averaging, max,
	 * sum, ...  By default, features are maxed, though. */
	public void merge(AnswerFV a2fv) {
		double[] a2values = a2fv.getValues();
		boolean[] a2isSet = a2fv.getIsSet();

		for (int i = 0; i < features.size(); i++) {
			if (!a2isSet[i]) {
				if (isSet[i] && values[i] < 0) {
					/* Unset value wins over
					 * negative value. */
					isSet[i] = false;
					values[i] = 0;
				}
				continue;
			}
			if (!isSet[i]) {
				if (a2values[i] >= 0) {
					/* Set the value only
					 * if non-negative. */
					isSet[i] = true;
					values[i] = a2values[i];
				}
				continue;
			}
			/* Actual feature merge.  XXX: Fancier mechanism
			 * than a brute-force if. */
			if (features.get(i) == AF_Occurences.class) {
				values[i] += a2values[i];
			} else {
				if (a2values[i] > values[i])
					values[i] = a2values[i];
			}
		}
	}

	/** Produce a FSArray of UIMA objects representing the set FV
	 * features. */
	public FSArray toFSArray(JCas jcas)
			throws AnalysisEngineProcessException {
		List<AnswerFeature> aflist = new LinkedList<AnswerFeature>();

		for (int i = 0; i < features.size(); i++) {
			if (!isSet[i])
				continue;

			Class<? extends AnswerFeature> cl = features.get(i);
			AnswerFeature f;
			try {
				f = cl.getConstructor(JCas.class).newInstance(jcas);
			} catch (InstantiationException e) {
				throw new AnalysisEngineProcessException(e);
			} catch (IllegalAccessException e) {
				throw new AnalysisEngineProcessException(e);
			} catch (InvocationTargetException e) {
				throw new AnalysisEngineProcessException(e);
			} catch (NoSuchMethodException e) {
				throw new AnalysisEngineProcessException(e);
			}

			f.setValue(values[i]);
			f.addToIndexes();
			aflist.add(f);
		}

		return FSCollectionFactory.createFSArray(jcas, aflist);
	}
}
