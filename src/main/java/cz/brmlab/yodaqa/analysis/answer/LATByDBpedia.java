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

import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATDBpType;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.DBpLAT;
import cz.brmlab.yodaqa.provider.rdf.DBpediaTypes;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.NN;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

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
 *   expands to "Inventors"... which expands to "Inventions", oops? */

public class LATByDBpedia extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATByDBpedia.class);

	final DBpediaTypes dbt = new DBpediaTypes();

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
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

	protected void addLATByLabel(JCas jcas, Focus focus, String label) throws AnalysisEngineProcessException {
		StringBuilder typelist = new StringBuilder();

		List<String> types = dbt.query(label, logger);
		for (String type : types) {
			addTypeLAT(jcas, focus, type, typelist);
		}

		if (typelist.length() > 0)
			logger.debug(".. Focus {} => DBpedia LATs/0 {}", focus.getCoveredText(), typelist);
	}

	protected void addTypeLAT(JCas jcas, Focus focus, String type, StringBuilder typelist) throws AnalysisEngineProcessException {
		addLATFeature(jcas, AF_LATDBpType.class, 1.0);

		String ntype = type.toLowerCase();

		/* We have a synthetic noun(-ish), synthetize
		 * a POS tag for it. */
		POS pos = new NN(jcas);
		pos.setBegin(focus.getBegin());
		pos.setEnd(focus.getEnd());
		pos.setPosValue("NNS");
		pos.addToIndexes();

		addLAT(new DBpLAT(jcas), focus.getBegin(), focus.getEnd(), focus, ntype, pos, 0, 0.0);

		typelist.append(" | " + ntype);
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
