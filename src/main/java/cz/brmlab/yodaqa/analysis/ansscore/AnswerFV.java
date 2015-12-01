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
 * TODO: We should instead build a feature _map_.  That'd simplify a lot
 * of cruft.
 *
 * Originally, this class has been intended mainly as an interface between
 * answer features and machine learned models. However, we also use it
 * whenever we need to manipulate a set of features at once - when initializing
 * a feature set and when merging feature sets of different answers. */

public class AnswerFV {
	/* XXX: This actually still prevents auto-generated features. */
	public static String labels[] = {
		"occurences", "resultRR", "resultLogScore", "passageLogScore",
			"propertyScore", "propertyGloVeScore",
		"originPsg", "originPsgFirst",
		"originPsgByClueToken", "originPsgByCluePhrase", "originPsgByClueSV",
			"originPsgByClueNE", "originPsgByClueLAT",
			"originPsgByClueSubject", "originPsgByClueSubjectNE",
			"originPsgByClueSubjectToken", "originPsgByClueSubjectPhrase",
			"originPsgByClueNgram", "originPsgByClueConcept",
			"originPsgByConceptSourceRR", "originPsgByConceptLabelRR", "originPsgByConceptScore",
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
			"originDBpOClueNgram", "originDBpOClueConcept",
			"originDBpOConceptSourceRR", "originDBpOConceptLabelRR", "originDBpOConceptScore",
		"originDBpProperty", "originDBpPNoClue",
		"originDBpPClueToken", "originDBpPCluePhrase", "originDBpPClueSV",
			"originDBpPClueNE", "originDBpPClueLAT",
			"originDBpPClueSubject", "originDBpPClueSubjectNE",
			"originDBpPClueSubjectToken", "originDBpPClueSubjectPhrase",
			"originDBpPClueNgram", "originDBpPClueConcept",
			"originDBpPConceptSourceRR", "originDBpPConceptLabelRR", "originDBpPConceptScore",
		"originFreebaseOntology",
			"originFreebaseSpecific", "originFreebaseBranched",
			"originFreebaseWitnessMid", "originFreebaseWitnessLabel",
			"originFBONoClue",
		"originFBOClueToken", "originFBOCluePhrase", "originFBOClueSV",
			"originFBOClueNE", "originFBOClueLAT",
			"originFBOClueSubject", "originFBOClueSubjectNE",
			"originFBOClueSubjectToken", "originFBOClueSubjectPhrase",
			"originFBOClueNgram", "originFBOClueConcept",
			"originFBOConceptSourceRR", "originFBOConceptLabelRR", "originFBOConceptScore",
		"originConcept",
			"originConceptBySubject", "originConceptByLAT", "originConceptByNE",
			"originConceptSourceRR", "originConceptLabelRR", "originConceptScore",
		"originMultiple",
		"psgDistClueToken", "psgDistCluePhrase", "psgDistClueSV",
			"psgDistClueNE", "psgDistClueLAT",
			"psgDistClueSubject", "psgDistClueSubjectNE",
			"psgDistClueSubjectToken", "psgDistClueSubjectPhrase",
			"psgDistClueNgram", "psgDistClueConcept",
			"psgDistConceptSourceRR", "psgDistConceptLabelRR", "psgDistConceptScore",
		"bioScore",
		"noTyCor", "LATANone",
                "spWordNet", "LATQNoWordNet", "LATANoWordNet",
		"tyCorPassageSp", "tyCorPassageDist", "tyCorPassageInside",
		"simpleScore",
		"answerFocusNone", "answerFocusWhole",
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
		"questionClassABBR", "questionClassDESC", "questionClassENTY",
	    "questionClassHUM", "questionClassLOC", "questionClassNUM"
	};

	protected double values[]; // the feature value
	protected boolean isSet[]; // whether the feature is set
	protected AnswerStats astats;

	public AnswerFV() {
		values = new double[labels.length];
		isSet = new boolean[labels.length];
	}

	public AnswerFV(FSArray fsarray) {
		this();

		if (fsarray == null)
			return;
		for (FeatureStructure fs : fsarray.toArray()) {
			AnswerFeature af = (AnswerFeature) fs;
			if (setFeature(af.getName(), af.getValue())) {
				int index = featureIndex(af.getName());
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

	public static String[] getFeatures() {
		// XXX: Rename to getLabels()
		return labels;
	}

	public static int featureIndex(String label) throws ArrayIndexOutOfBoundsException {
		int idx = ArrayUtils.indexOf(labels, label);
		if (idx == -1) {
			throw new ArrayIndexOutOfBoundsException(label);
		}
		return idx;
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
	public boolean setFeature(String f, double value) {
		int index = featureIndex(f);
		assert(index >= 0);

		boolean hasBeenSet = isSet[index];

		values[index] = value;
		isSet[index] = true;

		return hasBeenSet;
	}

	public double getFeatureValue(String f) {
		return values[featureIndex(f)];
	}
	public boolean isFeatureSet(String f) {
		return isSet[featureIndex(f)];
	}

	/** Merge the a2fv AnswerFV to this AnswerFV.  Individual features
	 * may have various policy of how this is done - averaging, max,
	 * sum, ...  By default, features are maxed, though. */
	public void merge(AnswerFV a2fv) {
		double[] a2values = a2fv.getValues();
		boolean[] a2isSet = a2fv.getIsSet();

		for (int i = 0; i < labels.length; i++) {
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
			if (labels[i].equals(AF.Occurences)) {
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

		for (int i = 0; i < labels.length; i++) {
			if (!isSet[i])
				continue;

			AnswerFeature f = new AnswerFeature(jcas);
			f.setName(labels[i]);
			f.setValue(values[i]);
			f.addToIndexes();
			aflist.add(f);
		}

		return FSCollectionFactory.createFSArray(jcas, aflist);
	}
}
