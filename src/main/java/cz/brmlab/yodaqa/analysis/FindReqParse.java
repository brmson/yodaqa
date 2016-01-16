package cz.brmlab.yodaqa.analysis;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.CandidateAnswer.PassageForParsing;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * An annotator that will create sofa-wide annotation if parsing is required.
 * That is, they aren't annotated even by POS annotations yet. Then, by aiming
 * StanfordParser at this annotation (PassageForParsing), we can make sure the
 * CAS is annotated with parsing tags iff it wasn't parsed yet. */

public class FindReqParse extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(FindReqParse.class);

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		for (POS pos : JCasUtil.select(jcas, POS.class))
			return; // ok, already parser
		if (JCasUtil.select(jcas, Token.class).isEmpty())
			return; // XXX can happen with crf, bug
		if (jcas.getDocumentText().length() == 0)
			return; // nothing to parse either

		PassageForParsing pfp = new PassageForParsing(jcas);
		pfp.setBegin(0);
		pfp.setEnd(jcas.getDocumentText().length());
		pfp.addToIndexes();
		logger.debug("requesting additional parse: {}", pfp.getCoveredText());
	}
}
