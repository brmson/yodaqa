package cz.brmlab.yodaqa.analysis.question;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NSUBJ;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.model.Question.Focus;

/**
 * Focus annotations in a QuestionCAS. This is the focus point of the sentence
 * where you should be able to place the answer.  In "What was the first book
 * written by Terry Pratchett?", "what" is the focus.  In "The actor starring
 * in Moon?", "the actor" is the focus (though that doesn't work terribly
 * well).  Typically, focus would be used by aligning algorithms.
 *
 * Prospectively, we will want to add multiple diverse Focus annotators.
 * This one is based on Constituent annotations. */

public class FocusGenerator extends JCasAnnotator_ImplBase {
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		for (ROOT sentence : JCasUtil.select(jcas, ROOT.class)) {
			processSentence(jcas, sentence);
		}
	}

	public void processSentence(JCas jcas, Constituent sentence) throws AnalysisEngineProcessException {
		Annotation focus = null;

		/* It's real easy - the NSUBJ is the focus!
		 * "Who is the most famous Italian painter?" -> NSUBJ:painter
		 * "Who invented the first transistor?" -> NSUBJ:who
		 */
		for (NSUBJ nsubj : JCasUtil.selectCovered(NSUBJ.class, sentence)) {
			focus = nsubj;
			break;
		}

		/* Ok, no NSUBJ. Just pick the first noun then, e.g.
		 * "The inventor of transistor" or
		 * "Name the inventor of transistor."
		 */
		if (focus == null) {
			for (Token t : JCasUtil.selectCovered(Token.class, sentence)) {
				if (t.getPos().getPosValue().matches("^NN.*")) {
					focus = t;
					break;
				}
			}
		}

		if (focus == null) {
			System.err.println("?! No focus in: " + sentence.getCoveredText());
			return;
		}

		Focus f = new Focus(jcas);
		f.setBegin(focus.getBegin());
		f.setEnd(focus.getEnd());
		f.setBase(focus);
		f.addToIndexes();
	}
}
