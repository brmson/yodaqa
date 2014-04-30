package cz.brmlab.yodaqa.analysis.question;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ADVMOD;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DET;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NSUBJ;

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
 * What is the name of Ling Ling's mate?		name (?)
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

	public void processSentence(JCas jcas, Constituent sentence) throws AnalysisEngineProcessException {
		Annotation focus = null;

		/* What -- and Which -- are DET dependencies; the governor
		 * may be either a noun or a verb. */
		if (focus == null) {
			for (DET det : JCasUtil.selectCovered(DET.class, sentence)) {
				if (det.getDependent().getPos().getPosValue().matches("^W.*")) {
					focus = det.getGovernor();
					break;
				}
			}
		}

		/* When, where is ADVMOD; take the covered text as focus.
		 * However, "how" is also ADVMOD; we need to take the
		 * governing token then (if it's an actual adverb). */
		if (focus == null) {
			for (ADVMOD advmod : JCasUtil.selectCovered(ADVMOD.class, sentence)) {
				if (advmod.getDependent().getLemma().getValue().equals("how")
				    && advmod.getGovernor().getPos().getPosValue().matches("^J.*|R.*")) {
					focus = advmod.getGovernor();
					break;
				} else if (advmod.getDependent().getPos().getPosValue().matches("^W.*")) {
					focus = advmod.getDependent();
					break;
				}
			}
		}

		/* Fall back on the NSUBJ.
		 * "Who is the most famous Italian painter?" -> NSUBJ:painter
		 * "Who invented the first transistor?" -> NSUBJ:who
		 */
		if (focus == null) {
			for (NSUBJ nsubj : JCasUtil.selectCovered(NSUBJ.class, sentence)) {
				focus = nsubj;
				break;
			}
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
			logger.info("?! No focus in: {}", sentence.getCoveredText());
			return;
		}

		Focus f = new Focus(jcas);
		f.setBegin(focus.getBegin());
		f.setEnd(focus.getEnd());
		f.setBase(focus);
		f.addToIndexes();
	}
}
