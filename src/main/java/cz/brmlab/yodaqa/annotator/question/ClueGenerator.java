package cz.brmlab.yodaqa.annotator.question;

import java.util.LinkedList;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
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
 * Prospectively, we will want to add multiple diverse clue annotators. This
 * one is based on interesting Constituent annotations + SV. */

public class ClueGenerator extends JCasAnnotator_ImplBase {
	protected String TOKENMATCH = "CD|FW|JJ.*|NN.*|RB.*|UH.*";
	protected String CONSTITMATCH = "AD.*|NP|PP|QP";

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* Walk the constituent tree and add all content-bearing
		 * constituents as keyphrases and content-bearing tokens
		 * as keywords. The constituents are often nested, so
		 * we can add all of "the Nobel Prize for Physiology and Medicine",
		 * "the Nobel Prize", "for Physiology and Medicine",
		 * "Physiology and Medicine", etc.  (And then the individual
		 * tokens too.) */

		/* This is a DFS over the Constituent tree. */
		LinkedList<Constituent> lifo = new LinkedList<Constituent>();
		for (ROOT sentence : JCasUtil.select(jcas, ROOT.class))
			lifo.add(sentence);
		while (!lifo.isEmpty()) {
			Constituent c = lifo.poll();
			if (c.getConstituentType().matches(CONSTITMATCH))
				addClue(jcas, c.getBegin(), c.getEnd(), c);

			for (FeatureStructure child : c.getChildren().toArray()) {
				if (!(child instanceof Constituent)) {
					Token t = (Token) child;
					if (t.getPos().getPosValue().matches(TOKENMATCH))
						addClue(jcas, t.getBegin(), t.getEnd(), t);
					continue;
				}
				lifo.add((Constituent) child);
			}
		}

		/* Oh, and all SVs are also Clues; they wouldn't be included
		 * in the constituents harvested above. */

		for (SV sv : JCasUtil.select(jcas, SV.class))
			addClue(jcas, sv.getBegin(), sv.getEnd(), sv);
	}

	protected void addClue(JCas jcas, int begin, int end, Annotation base) {
		Clue clue = new Clue(jcas);
		clue.setBegin(begin);
		clue.setEnd(end);
		clue.setBase(base);
		clue.addToIndexes();
	}
}
