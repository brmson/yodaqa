package cz.brmlab.yodaqa.annotator.question;

import java.util.LinkedList;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.model.Question.SV;
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
		Annotation focus;

		/* Find the first "what"-ish token. */
		focus = firstTokenByType(sentence, "W[PR].*");

		/* Fall back on the first NP constituent. */
		if (focus == null)
			focus = shallowestByType(sentence, "NP");

		if (focus == null)
			return;

		Focus f = new Focus(jcas);
		f.setBegin(focus.getBegin());
		f.setEnd(focus.getEnd());
		f.addToIndexes();
	}


	/* TODO: Move these to some generic Constituent tree walking class. */

	/** The first token in a given tree of a given type.
	 * Performs a simple DFS. */
	public Token firstTokenByType(Constituent root, String typematch) {
		LinkedList<Constituent> lifo = new LinkedList<Constituent>();
		lifo.add(root);

		while (!lifo.isEmpty()) {
			Constituent c = lifo.poll();

			for (FeatureStructure child : c.getChildren().toArray()) {
				if (!(child instanceof Constituent)) {
					Token t = (Token) child;
					if (t.getPos().getPosValue().matches(typematch))
						return t;
					continue;
				}
				lifo.add((Constituent) child);
			}
		}

		return null;
	}

	/** The shallowest constituent in a given tree of a given type.
	 * Performs a simple BFS. */
	public Constituent shallowestByType(Constituent root, String typematch) {
		LinkedList<Constituent> fifo = new LinkedList<Constituent>();
		fifo.add(root);

		while (!fifo.isEmpty()) {
			Constituent c = fifo.pop();

			if (c.getConstituentType().matches(typematch))
				return c;

			for (FeatureStructure child : c.getChildren().toArray()) {
				if (!(child instanceof Constituent))
					continue;
				fifo.add((Constituent) child);
			}
		}

		return null;
	}
}
