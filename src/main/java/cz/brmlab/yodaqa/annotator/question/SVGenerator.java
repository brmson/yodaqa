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
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.model.Question.SV;

/**
 * Selective Verb annotations in a QuestionCAS. These represent the
 * coordinating verb of the question that "selects" the answer with regard to
 * other clues. E.g.  "Who has received the Nobel Prize for Physiology and
 * Medicine?" will have "received" as SV; "When were they born?" will have
 * "born"; "How many colors do you need to color a planar graph?" will have
 * "need".  SV is one of the primary clues but is found in a special way and
 * might (or might not) be used specially in answer selection.
 *
 * Prospectively, we will want to add multiple diverse SV annotators,
 * especially for dealing with corner cases. This one is based on Constituent
 * annotations. */

public class SVGenerator extends JCasAnnotator_ImplBase {
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		for (ROOT sentence : JCasUtil.select(jcas, ROOT.class)) {
			processSentence(jcas, sentence);
		}
	}

	public void processSentence(JCas jcas, Constituent sentence) throws AnalysisEngineProcessException {
		/* Find the shallowest WH* clause. */
		Constituent WHc = shallowestByType(sentence, "WH.*");
		if (WHc == null) return;

		/* Find a coordinating sentence constituent (S, SINV, SQ, ...). */
		Constituent Sc = siblingNext(WHc);
		if (Sc == null) return;

		/* An immediate child should be a VP; in case of nested VPs,
		 * find the deepest VP. */
		Constituent VPc = straightDeepestByType(Sc, "VP");
		if (VPc == null) return;

		/* Now, take the actual verb word. */
		Token VBt = childTokByType(VPc, "VB.*");
		if (VBt == null) return;

		/* Ok, we believe this verb is the Selecting Verb. */
		SV sv = new SV(jcas);
		sv.setBegin(VBt.getBegin());
		sv.setEnd(VBt.getEnd());
		sv.setToken(VBt);
		sv.addToIndexes();
	}


	/* TODO: Move these to some generic Constituent tree walking class. */

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

	/** The next sibling (on the same level) after a given node. */
	public Constituent siblingNext(Constituent origin) {
		int end = origin.getEnd();
		Constituent parent = (Constituent) origin.getParent();

		for (FeatureStructure child : parent.getChildren().toArray()) {
			if (!(child instanceof Constituent))
				continue;
			Constituent c = (Constituent) child;
			if (c.getBegin() >= end)
				return c;
		}

		return null;
	}

	/** The (first) child constituent in a given node of a given type. */
	public Constituent childByType(Constituent parent, String typematch) {
		for (FeatureStructure child : parent.getChildren().toArray()) {
			if (!(child instanceof Constituent))
				continue;
			Constituent c = (Constituent) child;
			if (c.getConstituentType().matches(typematch))
				return c;
		}

		return null;
	}

	/** The sibling constituent in a given tree of a given type. */
	public Constituent siblingByType(Constituent origin, String typematch) {
		Constituent parent = (Constituent) origin.getParent();
		return childByType(parent, typematch);
	}

	/** The deepest constituent in a given tree of a given type in
	 * an uninterrupted line.  This is to unearth the deepest of nested
	 * same-type constituents, typically VPs for "has been indirected"
	 * to get to "indirected". */
	public Constituent straightDeepestByType(Constituent parent, String typematch) {
		Constituent child = null;
		Constituent nextchild = childByType(parent, typematch);
		while (nextchild != null) {
			child = nextchild;
			nextchild = childByType(child, typematch);
		}
		return child;
	}

	/** The (first) child Token in a given node of a given type. */
	public Token childTokByType(Constituent parent, String typematch) {
		for (FeatureStructure child : parent.getChildren().toArray()) {
			if (!(child instanceof Token))
				continue;
			Token t = (Token) child;
			if (t.getPos().getPosValue().matches(typematch))
				return t;
		}

		return null;
	}
}
