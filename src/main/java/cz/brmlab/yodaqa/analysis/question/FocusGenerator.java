package cz.brmlab.yodaqa.analysis.question;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.ADV;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ADVMOD;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DEP;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DET;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DOBJ;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NSUBJ;

import java.util.Iterator;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.Focus;

/**
 * Focus annotations in a QuestionCAS. This is the focus point of the sentence
 * where you should be able to place the answer.  In "What was the first book
 * written by Terry Pratchett?", "book" is the focus.  In "The actor starring
 * in Moon?", "the actor" is the focus (though that doesn't work terribly
 * well).  Typically, focus would be used by aligning algorithms and as a LAT.
 *
 * When was the U.S. capitol built?			date (time)
 * How did Virginia Woolf die?				-- (SV:die)
 * How big is Mars?					big
 * What is the play "West Side Story" based on?		base (???)
 * What color is the top stripe on the U.S. flag?	color
 * What is the name of Ling Ling's mate?		name, mate (!)
 * What did George Washington call his house?		-- (SV:call)
 * Who created the literary character Phineas Fogg?	person (SV:create)
 * In which city would you find the Louvre?		city
 * How many electoral votes does Tennessee have?	many
 * What dissolves gold?					-- (SV:dissolve-nt)
 * Where is Mount Olympus?				place
 * The sun is mostly made up of what two gasses?	gas
 *
 * The above makes it clear that this is not too easy.  So far, we hardcode
 * a simple heuristic of selection based on dependencies. */

public class FocusGenerator extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(FocusGenerator.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		for (ROOT sentence : JCasUtil.select(jcas, ROOT.class)) {
			processSentence(jcas, sentence);
		}
	}

	protected Token firstK1Token(JCas jcas) {
		for (Token t : JCasUtil.select(jcas, Token.class)) {
			if (t.getPos().getPosValue().matches("k1.*"))
				return t;
		}
		return null;
	}

	protected Token firstK1Token(JCas jcas, Token since) {
		for (Token t : JCasUtil.select(jcas, Token.class)) {
			if (t == since)
				since = null;
			else if (since != null)
				continue;
			if (t.getPos().getPosValue().matches("k1.*"))
				return t;
		}
		return null;
	}

	public void processSentence(JCas jcas, Constituent sentence) throws AnalysisEngineProcessException {
		Token focus = null;

		Iterator<Token> tokens = JCasUtil.select(jcas, Token.class).iterator();
		Token first = tokens.next();
		Token second = tokens.hasNext() ? tokens.next() : null;
		logger.debug("first {}/{}, second {}/{}",
			first.getLemma().getValue(), first.getPos().getPosValue(),
			second == null ? null : second.getLemma().getValue(),
			second == null ? null : second.getPos().getPosValue());

		// k6yQ + k3c4 -> next k1 (“jak se”)
		if (focus == null && second != null
		    && (first.getPos().getPosValue().matches("k6yQ")
		        && second.getPos().getPosValue().matches("k3c4.*"))) {
			focus = firstK1Token(jcas);
			if (focus != null)
				logger.debug("jakse, firstK1: {}", focus.getCoveredText());
		}

		// kdo/co/jak <být> k1 -> that k1
		if (focus == null
		    && ((first.getLemma().getValue().equals("kdo") || first.getLemma().getValue().equals("co") || first.getLemma().getValue().startsWith("jak"))
			    && second != null
			    && (second.getLemma().getValue().equals("být") || second.getLemma().getValue().equals("je") /* XXX */))) {
			focus = firstK1Token(jcas, second);
			if (focus != null)
				logger.debug("kdobýt, firstK1: {}", focus.getCoveredText());
		}

		// kdo, kdy, kde... -> ID
		if (focus == null
		    && (first.getPos().getPosValue().matches("k[3694].*yQ"))) {
			focus = first;
			logger.debug("first: {}", focus.getCoveredText());
		}

		// ELSE -> next k1
		if (focus == null) {
			focus = firstK1Token(jcas);
			if (focus != null)
				logger.debug("firstK1: {}", focus.getCoveredText());
		}

		if (focus == null) {
			logger.info("?! No focus in: {}", sentence.getCoveredText());
			return;
		}

		Focus f = new Focus(jcas);
		f.setBegin(focus.getBegin());
		f.setEnd(focus.getEnd());
		f.setBase(focus);
		f.setToken(focus);
		f.addToIndexes();
	}
}
