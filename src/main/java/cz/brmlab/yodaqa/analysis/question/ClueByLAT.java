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
import cz.brmlab.yodaqa.model.Question.ClueLAT;
import cz.brmlab.yodaqa.model.TyCor.ImplicitQLAT;
import cz.brmlab.yodaqa.model.TyCor.LAT;

/**
 * Generate Clue annotations in a QuestionCAS. These represent key information
 * stored in the question that is then used in primary search.  E.g. "What was
 * the first book written by Terry Pratchett?" should generate clues "first",
 * "book", "first book", "write" and "Terry Pratchett".
 *
 * This generates clues from the question focus, i.e. "book"; TODO maybe we
 * should also generate more complex clue system from the focus, e.g. "first
 * book written by Terry Pratchett". */

public class ClueByLAT extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(ClueByLAT.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		for (LAT l : JCasUtil.select(jcas, LAT.class)) {
			/* Use only the most specific LATs (there still may
			 * be mutliple, e.g. noun forms, etc.)  Also do not
			 * generate ClueLAT for question word LATs. */
			if (l.getSpecificity() < 0 || l instanceof ImplicitQLAT)
				continue;
			addClue(jcas, l.getBegin(), l.getEnd(), l, l.getText());
		}
	}

	protected void addClue(JCas jcas, int begin, int end, Annotation base, String label) {
		Clue clue = new ClueLAT(jcas);
		clue.setBegin(begin);
		clue.setEnd(end);
		clue.setBase(base);
		clue.setWeight(1.5);
		clue.setLabel(label);
		clue.setIsReliable(false);
		clue.addToIndexes();
		logger.debug("new by {}: {}", base.getType().getShortName(), clue.getLabel());
	}
}
