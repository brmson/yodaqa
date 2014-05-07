package cz.brmlab.yodaqa.analysis.question;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.model.Question.SV;
import cz.brmlab.yodaqa.model.Question.Clue;

/**
 * Generate Clue annotations in a QuestionCAS. These represent key information
 * stored in the question that is then used in primary search.  E.g. "What was
 * the first book written by Terry Pratchett?" should generate clues "first",
 * "book", "first book", "write" and "Terry Pratchett".
 *
 * This just generates clues from SVs (selecting verbs). So it generates
 * "write" for the above. */

public class ClueBySV extends JCasAnnotator_ImplBase {
	protected String SVBLACKLIST = "be|have|do";

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		for (SV sv : JCasUtil.select(jcas, SV.class)) {
			/* What was the name... -> "was" is useless as clue. */
			if (sv.getBase().getLemma().getValue().matches(SVBLACKLIST))
				continue;
			addClue(jcas, sv.getBegin(), sv.getEnd(), sv);
		}
	}

	protected void addClue(JCas jcas, int begin, int end, Annotation base) {
		Clue clue = new Clue(jcas);
		clue.setBegin(begin);
		clue.setEnd(end);
		clue.setBase(base);
		clue.addToIndexes();
	}
}
