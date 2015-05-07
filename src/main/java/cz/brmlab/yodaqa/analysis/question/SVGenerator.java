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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.Focus;
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
	final Logger logger = LoggerFactory.getLogger(SVGenerator.class);

	// Unfortunately, it seems our lemmatizer doesn't handle contractions
	// and verb forms? XXX: We should just roll our own lemmatizer
	protected String SVBLACKLIST = "be|have|do|'s|'re|'d|'ve|doe|has|get|give|list";

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
		for (Focus f : JCasUtil.selectCovered(Focus.class, sentence)) {
			focus = f.getBase();
			break;
		}

		Token v = null;

		if (focus != null) {
			if (v == null && focus.getTypeIndexID() == NSUBJ.type) {
				/* Make the subject's controlling verb an SV. */
				v = ((NSUBJ) focus).getGovernor();

				/* In "What is the X that Y?", "What" can be
				 * the governor.  That won't do. */
				if (!v.getPos().getPosValue().matches("^V.*")) {
					logger.debug("Ignoring SV proposal: {}", v.getCoveredText());
					v = null;
				}
			}

			if (v == null && focus.getTypeIndexID() == Token.type
			    && ((Token) focus).getPos().getPosValue().matches("^V.*")
			    && !isAux((Token) focus)) {
				/* The focus is a verb itself! Make it an SV too. */
				v = (Token) focus;
			}
		}

		if (v == null) {
			/* Take the first non-blacklisted verb. */
			v = getFirstVerb(sentence);
		}

		if (v == null) {
			logger.debug("No suitable SV proposed");
			return;
		}

		/* Ok, we believe this verb is the Selecting Verb. */
		SV sv = new SV(jcas);
		sv.setBegin(v.getBegin());
		sv.setEnd(v.getEnd());
		sv.setBase(v);
		sv.addToIndexes();
		logger.debug("SV: {}", sv.getCoveredText());
	}

	protected Token getFirstVerb(Constituent sentence) {
		for (Token v : JCasUtil.selectCovered(Token.class, sentence)) {
			if (!v.getPos().getPosValue().matches("^V.*"))
				continue;
			if (isAux(v))
				continue;
			return v;
		}
		return null;
	}

	protected boolean isAux(Token v) {
		/* What was the name... -> "was" is useless for us.
		 * Ignore over-generic verbs. */
		return v.getLemma().getValue().toLowerCase().matches(SVBLACKLIST);
	}
}
