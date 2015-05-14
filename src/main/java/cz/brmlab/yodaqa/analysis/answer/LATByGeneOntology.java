package cz.brmlab.yodaqa.analysis.answer;

import java.util.Collection;

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
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATGeneOntology;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.TyCor.GeneOntologyLAT;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.provider.GeneOntologyCall;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.NN;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;

/**
 * Generate LAT annotations in a CandidateAnswerCAS. This is based on the
 * answer focus (or NamedEntity) which is looked up in GeneOntology and
 * LATs based on the type field are generated. */
/* XXX: Inherit from an abc common to LATByDBpedia. */

public class LATByGeneOntology extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATByGeneOntology.class);

	final GeneOntologyCall go = new GeneOntologyCall();

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* Skip an empty answer. */
		if (jcas.getDocumentText().matches("^\\s*$"))
			return;

		/* First, try to look up the whole answer - that's ideal,
		 * after all! */
		if (addLATByLabel(jcas, null, jcas.getDocumentText()))
			return;

		/* A Focus is a good LAT query source. */
		/* (XXX: N.B. in bioasq this is dead code.) */
		for (Focus focus : JCasUtil.select(jcas, Focus.class)) {
			/* ...however, prefer an overlapping named entity. */
			boolean ne_found = false;
			for (NamedEntity ne : JCasUtil.selectCovering(NamedEntity.class, focus)) {
				addLATByLabel(jcas, focus, ne.getCoveredText());
				ne_found = true;
			}

			if (!ne_found) {
				addLATByLabel(jcas, focus, focus.getToken().getLemma().getValue());
			}
		}
	}

	protected boolean addLATByLabel(JCas jcas, Focus focus, String label) throws AnalysisEngineProcessException {
		StringBuilder typelist = new StringBuilder();

		Collection<String> types = go.getTypes(label);
		for (String type : types) {
			addLATFeature(jcas, AF_LATGeneOntology.class);
			addTypeLAT(jcas, new GeneOntologyLAT(jcas), focus, type, 0, typelist);
		}

		if (typelist.length() > 0) {
			if (focus != null)
				logger.debug(".. Focus {} => GeneOntology LATs/0 {}", focus.getCoveredText(), typelist);
			else
				logger.debug(".. Ans {} => GeneOntology LATs/0 {}", label, typelist);
			return true;
		} else {
			return false;
		}
	}

	protected void addTypeLAT(JCas jcas, LAT lat, Focus focus, String type, long synset, StringBuilder typelist) throws AnalysisEngineProcessException {
		String ntype = type.toLowerCase();

		/* We have a synthetic noun(-ish), synthetize
		 * a POS tag for it. */
		Annotation LATBase;
		POS pos = new NN(jcas);
		if (focus != null) {
			LATBase = focus;
			pos.setBegin(focus.getBegin());
			pos.setEnd(focus.getEnd());
		} else {
			LATBase = pos;
			pos.setBegin(0);
			pos.setEnd(jcas.getDocumentText().length());
		}
		pos.setPosValue("NNS");
		pos.addToIndexes();

		addLAT(lat, LATBase.getBegin(), LATBase.getEnd(), LATBase, ntype, pos, synset, 0.0);

		if (synset == 0) {
			typelist.append(" | " + ntype);
		} else {
			typelist.append(" | " + ntype + "/" + Long.toString(synset));
		}
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

	protected void addLATFeature(JCas jcas, Class<? extends AnswerFeature> f) throws AnalysisEngineProcessException {
		AnswerInfo ai = JCasUtil.selectSingle(jcas, AnswerInfo.class);
		AnswerFV fv = new AnswerFV(ai);
		fv.setFeature(f, 1.0);

		for (FeatureStructure af : ai.getFeatures().toArray())
			((AnswerFeature) af).removeFromIndexes();
		ai.removeFromIndexes();

		ai.setFeatures(fv.toFSArray(jcas));
		ai.addToIndexes();
	}
}
