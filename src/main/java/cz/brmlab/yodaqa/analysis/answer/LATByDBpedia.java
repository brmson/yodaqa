package cz.brmlab.yodaqa.analysis.answer;

import java.util.List;

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
import cz.brmlab.yodaqa.model.TyCor.DBpLAT;
import cz.brmlab.yodaqa.provider.rdf.DBpediaTypes;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.NN;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;

/**
 * Generate LAT annotations in a CandidateAnswerCAS. This is based on the
 * answer focus (or NamedEntity) which is looked up in DBpedia and LATs
 * based on rdf:type entries are generated.
 *
 * We intended to simply use the categories at first (as in
 * http://purl.org/dc/terms/subject properties).  However,
 * - converting "Serbian Inventors" to LAT inventor or dealing with
 *   "Cultural Concepts" or "Peninsulas of Asia" is actually really tricky.
 * - recursive expansion of categories is problematic; "Serbian Invetors"
 *   expands to "Inventors"... which expands to "Inventions", oops?
 *
 * We also take care to use only leaf (most specific) type entries
 * and rely oo wordnet abstractions.
 *
 * This is only a fallback LAT source and is inhibited for any answers
 * that already got an LAT from elsewhere. */
/* XXX: LATByDBpediaWN inehrits from this class. Instead, have an abc
 * LATByLookup or something. */

public class LATByDBpedia extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATByDBpedia.class);

	// XXX: For override by LATByDBpediaWN; FIXME cleanup class hiearchy
	protected boolean fallbackOnly = true;

	final DBpediaTypes dbt = new DBpediaTypes();

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* Skip an empty answer. */
		if (jcas.getDocumentText().matches("^\\s*$"))
			return;

		/* Skip an answer that already carries any LAT, as we assume
		 * basically anything is going to be sharply better than what
		 * we generate here.  (XXX: There may be special cases like
		 * years.) */
		if (fallbackOnly && !JCasUtil.select(jcas, LAT.class).isEmpty())
			return;

		/* First, try to look up the whole answer - that's ideal,
		 * after all! */
		if (addLATByLabel(jcas, null, jcas.getDocumentText()))
			return;

		/* A Focus is a good LAT query source. */
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

		List<String> types = dbt.query(label, logger);
		for (String type : types) {
			if (type.toLowerCase().equals("function word")) {
				/* Ignore function words: she, them, ... */
				logger.debug(".. skipping function word <<{}>>", label);
				return false;
			}
		}

		for (String type : types) {
			addLATFeature(jcas, AF.LATDBpType);
			addTypeLAT(jcas, new DBpLAT(jcas), focus, type, 0, typelist);
		}

		if (typelist.length() > 0) {
			if (focus != null)
				logger.debug(".. Focus {} => DBpedia LATs/0 {}", focus.getCoveredText(), typelist);
			else
				logger.debug(".. Ans {} => DBpedia LATs/0 {}", label, typelist);
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
}
