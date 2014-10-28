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

import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATQuantity;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATQuantityCD;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.QuantityLAT;
import cz.brmlab.yodaqa.model.TyCor.QuantityCDLAT;
import cz.brmlab.yodaqa.model.TyCor.NELAT;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.NN;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NUM;

/**
 * Generate LAT annotations in a CandidateAnswerCAS when the candidate answer
 * contains some sort of quantity.
 *
 * TODO: We should try to figure out what kind of quantity it actually is
 * based on the unit. */

public class LATByQuantity extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATByQuantity.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* A Focus is also an LAT. */
		for (Focus focus : JCasUtil.select(jcas, Focus.class)) {
			for (NUM num : JCasUtil.select(jcas, NUM.class)) {
				if (num.getGovernor().equals(focus.getToken()))
					addQuantityLAT(jcas, num);
			}
		}
	}

	protected void addQuantityLAT(JCas jcas, NUM num) throws AnalysisEngineProcessException {
		// XXX: "quantity" is not the primary label for this wordnet sense
		String text = "measure";
		double spec = 0.0;

		/* We have a synthetic LAT, synthetize a POS tag for it. */
		POS pos = new NN(jcas);
		pos.setBegin(num.getBegin());
		pos.setEnd(num.getEnd());
		pos.setPosValue("NNS");
		pos.addToIndexes();

		/* Also set a feature when the quantity is an actual number
		 * (as opposed to e.g. "many"). */
		if (num.getDependent().getPos().getPosValue().equals("CD")) {
			addLATFeature(jcas, AF_LATQuantityCD.class, 1.0);
			addLAT(new QuantityCDLAT(jcas), num.getBegin(), num.getEnd(), num, text, pos, spec);
		} else {
			addLATFeature(jcas, AF_LATQuantity.class, 1.0);
			addLAT(new QuantityLAT(jcas), num.getBegin(), num.getEnd(), num, text, pos, spec);
		}

		logger.debug(".. Quantity LAT {} by NUM {}", text, num.getCoveredText());
	}

	protected void addLAT(LAT lat, int begin, int end, Annotation base, String text, POS pos, double spec) {
		lat.setBegin(begin);
		lat.setEnd(end);
		lat.setBase(base);
		lat.setPos(pos);
		lat.setText(text);
		lat.setSpecificity(spec);
		lat.addToIndexes();
	}

	protected void addLATFeature(JCas jcas, Class<? extends AnswerFeature> f, double value) throws AnalysisEngineProcessException {
		AnswerInfo ai = JCasUtil.selectSingle(jcas, AnswerInfo.class);
		AnswerFV fv = new AnswerFV(ai);
		fv.setFeature(f, fv.getFeatureValue(f) + value);

		for (FeatureStructure af : ai.getFeatures().toArray())
			((AnswerFeature) af).removeFromIndexes();
		ai.removeFromIndexes();

		ai.setFeatures(fv.toFSArray(jcas));
		ai.addToIndexes();
	}
}
