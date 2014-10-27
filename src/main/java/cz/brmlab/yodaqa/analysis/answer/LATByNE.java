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

import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATNE;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.NELAT;
import cz.brmlab.yodaqa.provider.OpenNlpNamedEntities;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.NN;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Generate LAT annotations in a CandidateAnswerCAS. If a NamedEntity
 * covers the answer focus, the type of that named entity (as not just
 * a word but a wordnet synset) is generated as the LAT. */

public class LATByNE extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATByNE.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		for (Focus focus : JCasUtil.select(jcas, Focus.class)) {
			addNELAT(jcas, focus);
		}
	}

	protected boolean addNELAT(JCas jcas, Focus focus) throws AnalysisEngineProcessException {
		boolean ne_found = false;
		for (NamedEntity ne : JCasUtil.selectCovering(NamedEntity.class, focus)) {
			ne_found = true;
			long synset = OpenNlpNamedEntities.neValueToSynset(ne.getValue());
			addLAT(new NELAT(jcas), ne.getBegin(), ne.getEnd(), ne, ne.getValue(), focus.getToken().getPos(), synset, 0.0);
			logger.debug(".. LAT {}/{} by NE {}", ne.getValue(), synset, ne.getCoveredText());
			addLATFeature(jcas, AF_LATNE.class, 1.0);
		}
		return ne_found;
	}

	protected void addLAT(LAT lat, int begin, int end, Annotation base, String text, POS pos, long synset, double spec) {
		lat.setBegin(begin);
		lat.setEnd(end);
		lat.setBase(base);
		lat.setPos(pos);
		lat.setText(text);
		lat.setSpecificity(spec);
		lat.setSynset(synset);
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
