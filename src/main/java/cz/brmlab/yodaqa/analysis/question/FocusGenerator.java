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

	public void processSentence(JCas jcas, Constituent sentence) throws AnalysisEngineProcessException {
		Token focusTok = null;
		Annotation focus = null;

		/* What -- and Which -- are DET dependencies; the governor
		 * may be either a noun or a verb, accept only a noun.
		 * ("Which is the genetic defect causing Neurofibromatosis type 1?"
		 * has "is" as a governor). */
		if (focus == null) {
			for (DET det : JCasUtil.selectCovered(DET.class, sentence)) {
				if (det.getDependent().getPos().getPosValue().matches("^W.*")
				    && !det.getGovernor().getPos().getPosValue().matches("^V.*")) {
					focusTok = det.getGovernor();
					focus = focusTok;
					logger.debug("DET+W {}", focus.getCoveredText());
					break;
				}
			}
		}

		/* When, where is ADVMOD; take the covered text as focus.
		 * However, "how" is also ADVMOD; we need to take the
		 * governing token then (adverb or verb). */
		if (focus == null) {
			for (ADVMOD advmod : JCasUtil.selectCovered(ADVMOD.class, sentence)) {
				if (advmod.getDependent().getLemma().getValue().toLowerCase().equals("how")) {
					focusTok = advmod.getGovernor();
					focus = focusTok;
					logger.debug("ADVMOD+how {}", focus.getCoveredText());
					break;
				} else if (advmod.getDependent().getPos().getPosValue().matches("^W.*")) {
					focusTok = advmod.getDependent();
					focus = focusTok;
					logger.debug("ADVMOD+W {}", focus.getCoveredText());
					break;
				}
			}
		}

		/* DEP dependencies are also sometimes generated, e.g.
		 * "When was the battle of Aigospotamoi?" (When / was)
		 * "What lays blue eggs?" (What / lays) */
		if (focus == null) {
			for (DEP dep : JCasUtil.selectCovered(DEP.class, sentence)) {
				if (dep.getDependent().getPos().getPosValue().matches("^W.*")) {
					if (dep.getDependent().getPos() instanceof ADV) {
						/* Not 'what' but adverbish like 'when'. */
						focusTok = dep.getDependent();
					} else {
						/* A verb like 'lays'. */
						focusTok = dep.getGovernor();
					}
					focus = focusTok;
					logger.debug("DEP+W {}", focus.getCoveredText());
					break;
				}
			}
		}

		/* Wh- DOBJ is preferrable to NSUBJ, if available and not be-bound.
		 * "Who did X Y play in Z?" -> DOBJ:who (and NSUBJ:X Y) */
		if (focus == null) {
			for (DOBJ dobj : JCasUtil.selectCovered(DOBJ.class, sentence)) {
				if (dobj.getDependent().getPos().getPosValue().matches("^W.*")
				    && !LATByFocus.isAmbiguousQLemma(dobj.getDependent().getLemma().getValue().toLowerCase())) {
					focusTok = dobj.getDependent();
					focus = focusTok;
					logger.debug("DOBJ+W {}", focus.getCoveredText());
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
				focusTok = nsubj.getDependent();
				focus = nsubj;
				logger.debug("NSUBJ {}", focus.getCoveredText());
				break;
			}
		}

		/* If the question is actually an imperative sentence,
		 * take DOBJ:
		 * List all games by GMT. -> DOBJ:games
		 */
		if (focus == null) {
			for (DOBJ dobj : JCasUtil.selectCovered(DOBJ.class, sentence)) {
				focusTok = dobj.getDependent();
				focus = dobj;
				logger.debug("DOBJ {}", focus.getCoveredText());
				break;
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
		f.setToken(focusTok);
		f.addToIndexes();
	}
}
