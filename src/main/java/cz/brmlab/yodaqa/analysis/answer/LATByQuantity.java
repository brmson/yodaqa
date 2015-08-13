package cz.brmlab.yodaqa.analysis.answer;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.QuantityLAT;
import cz.brmlab.yodaqa.model.TyCor.QuantityCDLAT;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.NN;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NUM;

/**
 * Generate LAT annotations in a CandidateAnswerCAS when the candidate answer
 * contains some sort of quantity.
 *
 * TODO: We should try to figure out what kind of quantity it actually is
 * based on the unit. */

public class LATByQuantity extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATByQuantity.class);

	protected static String texts[] = {
		// XXX: "quantity" is not the primary label for this wordnet sense
		"measure",
		// quantitative relation, e.g. speed:
		"magnitude relation",
		// positions and distances, e.g. altitude:
		"magnitude",
	};
	protected static long synsets[] = {
		33914,
		13837364,
		5097645,
	};

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* First, check if the answer simply *is* a number.
		 * E.g. just "593.0", "2014" or some such. */
		if (answerIsNumber(jcas.getDocumentText())) {
			addQuantityLAT(jcas, null, true);
			return;
		}

		/* Otherwise, it may be the defining part of sentence.
		 * E.g. "20 C", "190 meters" or "over 90 films".
		 * The stuff after the number typically ends up
		 * being the Focus. */
		for (Focus focus : JCasUtil.select(jcas, Focus.class)) {
			for (NUM num : JCasUtil.select(jcas, NUM.class)) {
				/* Ignore if we are part of a named entity,
				 * that's a better LAT determinator. */
				if (isNERelated(jcas, num))
					continue;
				if (!num.getGovernor().equals(focus.getToken()))
					continue;
				boolean isCD = num.getDependent().getPos().getPosValue().equals("CD");
				addQuantityLAT(jcas, num, isCD);
			}
		}
	}

	protected void addQuantityLAT(JCas jcas, Annotation LATbase, boolean isCD) throws AnalysisEngineProcessException {
		double spec = 0.0;

		/* We have a synthetic LAT, synthetize a POS tag for it. */
		POS pos = new NN(jcas);
		if (LATbase != null) {
			pos.setBegin(LATbase.getBegin());
			pos.setEnd(LATbase.getEnd());
		} else {
			LATbase = pos;
			pos.setBegin(0);
			pos.setEnd(jcas.getDocumentText().length());
		}
		pos.setPosValue("NNS");
		pos.addToIndexes();

		/* Also set a feature when the quantity is an actual number
		 * (as opposed to e.g. "many"). */
		if (isCD) {
			addLATFeature(jcas, AF.LATQuantityCD);
			addLAT(new QuantityCDLAT(jcas), LATbase.getBegin(), LATbase.getEnd(), LATbase, texts[0], synsets[0], pos, spec);
			addLAT(new QuantityCDLAT(jcas), LATbase.getBegin(), LATbase.getEnd(), LATbase, texts[1], synsets[1], pos, spec);
			addLAT(new QuantityCDLAT(jcas), LATbase.getBegin(), LATbase.getEnd(), LATbase, texts[2], synsets[2], pos, spec);
		} else {
			addLATFeature(jcas, AF.LATQuantity);
			addLAT(new QuantityLAT(jcas), LATbase.getBegin(), LATbase.getEnd(), LATbase, texts[0], synsets[0], pos, spec);
			addLAT(new QuantityLAT(jcas), LATbase.getBegin(), LATbase.getEnd(), LATbase, texts[1], synsets[1], pos, spec);
			addLAT(new QuantityLAT(jcas), LATbase.getBegin(), LATbase.getEnd(), LATbase, texts[2], synsets[2], pos, spec);
		}

		logger.debug(".. Quantity {} LAT {}/{}, {}/{}, {}/{} based on [{}] <<{}>>",
			isCD ? "CD" : "noCD",
			texts[0], synsets[0], texts[1], synsets[1], texts[2], synsets[2],
			LATbase.getClass().getSimpleName(), LATbase.getCoveredText());
	}

	/** Whether the annotation is covered by a named entity
	 * or touching one at most across \\W. */
	protected boolean isNERelated(JCas jcas, Annotation ann) {
		for (NamedEntity ne : JCasUtil.select(jcas, NamedEntity.class)) {
			/* If there is an overlap, return right away. */
			if ((ne.getBegin() >= ann.getBegin() && ne.getEnd() <= ann.getEnd())
			    || (ne.getBegin() < ann.getBegin() && ne.getEnd() >= ann.getBegin()))
				return true;

			// arbitrary fuzz factor for maximum displacement
			int displ_limit = 6;
			// only non-word characters between NamedEntity and ann?
			if (ne.getEnd() < ann.getBegin() && ne.getEnd() + displ_limit >= ann.getBegin()
			    && jcas.getDocumentText().substring(ne.getEnd(), ann.getBegin()).matches("^\\W*$"))
				return true;
			// only non-word characters between ann and NamedEntity?
			if (ne.getBegin() >= ann.getEnd() && ann.getEnd() + displ_limit < ne.getBegin()
			    && jcas.getDocumentText().substring(ann.getEnd(), ne.getBegin()).matches("^\\W*$"))
				return true;
		}
		return false;
	}

	protected void addLAT(LAT lat, int begin, int end, Annotation base, String text, long synset, POS pos, double spec) {
		lat.setBegin(begin);
		lat.setEnd(end);
		lat.setBase(base);
		lat.setPos(pos);
		lat.setText(text);
		lat.setSynset(synset);
		lat.setSpecificity(spec);
		lat.addToIndexes();
	}

	protected void addLATFeature(JCas jcas, String f) throws AnalysisEngineProcessException {
		AnswerInfo ai = JCasUtil.selectSingle(jcas, AnswerInfo.class);
		AnswerFV fv = new AnswerFV(ai);
		fv.setFeature(f, 1.0);

		for (FeatureStructure af : ai.getFeatures().toArray())
			((AnswerFeature) af).removeFromIndexes();
		ai.removeFromIndexes();

		ai.setFeatures(fv.toFSArray(jcas));
		ai.addToIndexes();
	}

	/** Determine whether a given answer is simply numeric as a whole.
	 * This is often the case for structured data sources. */
	public static boolean answerIsNumber(String text) {
		// return text.matches("^\\s*[-+]?[0-9]+([.,][0-9]+)?([eEx^][0-9]+)*\\s*$");
		try {
			Double.parseDouble(text);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	/** Check if a given LAT could be coerced to a quantity LAT. */
	public static boolean latIsQuantity(LAT lat) {
		long latSynset = lat.getSynset();
		return latSynset == synsets[0] || latSynset == synsets[1] || latSynset == synsets[2];
	}
}
