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
import cz.brmlab.yodaqa.model.Question.ClueFocus;
import cz.brmlab.yodaqa.model.Question.Focus;

/**
 * Generate Clue annotations in a QuestionCAS. These represent key information
 * stored in the question that is then used in primary search.  E.g. "What was
 * the first book written by Terry Pratchett?" should generate clues "first",
 * "book", "first book", "write" and "Terry Pratchett".
 *
 * This generates clues from the question focus, i.e. "book"; TODO maybe we
 * should also generate more complex clue system from the focus, e.g. "first
 * book written by Terry Pratchett". */

public class ClueByFocus extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(ClueByFocus.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		for (Focus f : JCasUtil.select(jcas, Focus.class)) {
			/* Skip question word focuses (e.g. "Where"). */
			if (f.getToken().getPos().getPosValue().matches("^W.*"))
				continue;
			addClue(jcas, f.getBegin(), f.getEnd(), f);
		}
	}

	protected void addClue(JCas jcas, int begin, int end, Annotation base) {
		Clue clue = new ClueFocus(jcas);
		clue.setBegin(begin);
		clue.setEnd(end);
		clue.setBase(base);
		clue.setWeight(1.5);
		clue.setLabel(clue.getCoveredText());
		clue.setIsReliable(true);
		clue.addToIndexes();
		logger.debug("new by {}: {}", base.getType().getShortName(), clue.getLabel());
	}
}
