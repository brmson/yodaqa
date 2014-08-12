package cz.brmlab.yodaqa.analysis.question;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.Question.ClueSV;
import cz.brmlab.yodaqa.model.Question.SV;

/**
 * Generate Clue annotations in a QuestionCAS. These represent key information
 * stored in the question that is then used in primary search.  E.g. "What was
 * the first book written by Terry Pratchett?" should generate clues "first",
 * "book", "first book", "write" and "Terry Pratchett".
 *
 * This just generates clues from SVs (selecting verbs). So it generates
 * "write" for the above. */

public class ClueBySV extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(ClueBySV.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		for (SV sv : JCasUtil.select(jcas, SV.class))
			addClue(jcas, sv.getBegin(), sv.getEnd(), sv);
	}

	protected void addClue(JCas jcas, int begin, int end, Annotation base) {
		Clue clue = new ClueSV(jcas);
		clue.setBegin(begin);
		clue.setEnd(end);
		clue.setBase(base);
		clue.setWeight(1.0);
		clue.setLabel(clue.getCoveredText());
		clue.setIsReliable(true);
		clue.addToIndexes();
		logger.debug("new by {}: {}", base.getType().getShortName(), clue.getLabel());
	}
}
