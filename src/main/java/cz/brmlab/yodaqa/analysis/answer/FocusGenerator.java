package cz.brmlab.yodaqa.analysis.answer;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

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
 * Focus annotations in a CandidateAnswerCAS. This is the focus point of the
 * clause, i.e. its "root":
 *
 *   mathematics -> mathematics
 *   several complex variables -> variables
 *   very slow -> slow
 *   many different and incompatible systems of notation for them -> systems
 *
 * It serves similar purpose as question focus, i.e. defining the answer type
 * or possibly a support for alignment. */

public class FocusGenerator extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(FocusGenerator.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		Annotation focus = null;

		/* We do a pretty naive thing - selecting the first noun
		 * or the last adverb / adjective / number.  No focus
		 * if there is neither. */

		if (focus == null) {
			for (Token t : JCasUtil.select(jcas, Token.class)) {
				if (t.getPos().getPosValue().matches("^NN.*")) {
					focus = t;
					break;
				} else if (t.getPos().getPosValue().matches("^RB.*")) {
					focus = t;
				} else if (t.getPos().getPosValue().matches("^JJ.*")) {
					focus = t;
				} else if (t.getPos().getPosValue().matches("^CD.*")) {
					focus = t;
				}
			}
		}

		if (focus == null) {
			logger.info("?. No focus in: " + jcas.getDocumentText());
			return;
		}

		Focus f = new Focus(jcas);
		f.setBegin(focus.getBegin());
		f.setEnd(focus.getEnd());
		f.setBase(focus);
		f.addToIndexes();
	}
}
