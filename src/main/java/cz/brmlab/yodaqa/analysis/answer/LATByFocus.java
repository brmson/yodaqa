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

import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATFocus;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATFocusProxy;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATNE;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.TyCor.LAT;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.NN;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Generate LAT annotations in a CandidateAnswerCAS. This is based on the
 * answer focus and the result LAT texts should be compatible with Question.LAT
 * but the process of their generation might be different in details. */

public class LATByFocus extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATByFocus.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* A Focus is also an LAT. */
		for (Focus focus : JCasUtil.select(jcas, Focus.class)) {
			/* ...however, prefer an overlapping named entity. */
			if (!addNELAT(jcas, focus))
				addFocusLAT(jcas, focus);
		}
	}

	protected void addFocusLAT(JCas jcas, Focus focus) throws AnalysisEngineProcessException {
		/* Convert focus to its lemma. */
		Token ftok = focus.getToken();
		String text = ftok.getLemma().getValue().toLowerCase();
		double spec = 0.0;
		POS pos = ftok.getPos();

		/* Focus may be a number... */
		if (ftok.getPos().getPosValue().matches("^CD")) {
			text = "quantity";
			pos = null;
			addLATFeature(jcas, AF_LATFocusProxy.class, 1.0);
		} else {
			addLATFeature(jcas, AF_LATFocus.class, 1.0);
		}

		if (pos == null) {
			/* We have a synthetic focus noun, synthetize
			 * a POS tag for it. */
			pos = new NN(jcas);
			pos.setBegin(focus.getBegin());
			pos.setEnd(focus.getEnd());
			pos.setPosValue("NNS");
			pos.addToIndexes();
		}

		addLAT(jcas, focus.getBegin(), focus.getEnd(), focus, text, pos, spec);
		logger.debug(".. LAT {} by Focus {}", text, focus.getCoveredText());
	}

	protected boolean addNELAT(JCas jcas, Focus focus) throws AnalysisEngineProcessException {
		boolean ne_found = false;
		for (NamedEntity ne : JCasUtil.selectCovering(NamedEntity.class, focus)) {
			ne_found = true;
			addLAT(jcas, ne.getBegin(), ne.getEnd(), ne, ne.getValue(), focus.getToken().getPos(), 0.0);
			logger.debug(".. LAT {} by NE {}", ne.getValue(), ne.getCoveredText());
			addLATFeature(jcas, AF_LATNE.class, 1.0);
		}
		return ne_found;
	}

	protected void addLAT(JCas jcas, int begin, int end, Annotation base, String text, POS pos, double spec) {
		LAT lat = new LAT(jcas);
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
