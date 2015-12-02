package cz.brmlab.yodaqa.analysis.answer;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
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

	protected class FocusPair {
		public Token focusTok;
		public Annotation focus;

		public FocusPair(Token focusTok_, Annotation focus_) {
			focusTok = focusTok_;
			focus = focus_;
		}

		public Token getFocusTok() { return focusTok; }
		public Annotation getFocus() { return focus; }
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		FocusPair fp = null;

		/* First, do not generate a focus in a number, particularly
		 * one that might be segmented at a decimal dot.  Therefore,
		 * further annotators will always be forced to consider the
		 * answer only as a whole. */
		if (LATByQuantity.answerIsNumber(jcas.getDocumentText())) {
			logger.debug(".. No focus generated in all-number answer: " + jcas.getDocumentText());
			return;
		}

		if (fp == null)
			fp = fpDepRoot(jcas);
		if (fp == null)
			fp = fpByPos(jcas);

		if (fp == null) {
			addFocusFeature(jcas, AF.AnswerFocusNone);
			logger.info("?. No focus in: " + jcas.getDocumentText());
			return;
		} else {
			if (fp.getFocus().getCoveredText().equals(jcas.getDocumentText()))
				addFocusFeature(jcas, AF.AnswerFocusWhole);
			logger.debug(".. Focus '{}' in: {}", fp.getFocus().getCoveredText(), jcas.getDocumentText());
		}

		Focus f = new Focus(jcas);
		f.setBegin(fp.getFocus().getBegin());
		f.setEnd(fp.getFocus().getEnd());
		f.setBase(fp.getFocus());
		f.setToken(fp.getFocusTok());
		f.addToIndexes();
	}

	/* Check all dependencies, picking the "pinnacles", i.e.
	 * tokens that govern some dependencies but aren't governed
	 * themselves, these might be local roots.  In case of multiple
	 * pinnacles, the first one is chosen. */
	protected FocusPair fpDepRoot(JCas jcas) {
		FocusPair fp = null;

		/* Sometimes, we will get dependencies that reach out of
		 * the current CandidateAnswerCAS. Therefore, consider
		 * only governors that have corresponding tokens in our CAS. */
		Set<Token> tokens = new HashSet<Token>();
		for (Token t : JCasUtil.select(jcas, Token.class))
			tokens.add(t);

		SortedSet<Token> governors = new TreeSet<Token>(
			new Comparator<Token>(){ @Override
				public int compare(Token t1, Token t2){
					return t1.getBegin() - t2.getBegin();
				}
			});
		for (Dependency d : JCasUtil.select(jcas, Dependency.class)) {
			if (tokens.contains(d.getGovernor())) {
				governors.add(d.getGovernor());
				// logger.debug("+ governor {}", d.getGovernor());
			}
		}
		for (Dependency d : JCasUtil.select(jcas, Dependency.class)) {
			if (tokens.contains(d.getGovernor())) {
				governors.remove(d.getDependent());
				// logger.debug("- dependent {}", d.getDependent());
			}
		}

		for (Token t : governors) {
			if (fp != null) {
				logger.debug("?. Ignoring secondary potential focus '{}' in: {}",
					t.getCoveredText(), jcas.getDocumentText());
				continue;
			}
			fp = new FocusPair(t, t);
		}
		return fp;
	}

	/* We do a pretty naive thing - selecting the first noun or
	 * the last adverb / adjective / number. */
	protected FocusPair fpByPos(JCas jcas) {
		Token focusTok = null;
		Annotation focus = null;

		for (Token t : JCasUtil.select(jcas, Token.class)) {
			if (t.getPos() == null) {
				/* This in theory should never happen.
				 * However, in unsanitized DBpedia inputs,
				 * evil characters (like |) that mess up
				 * postagging might happen, so this dirty
				 * hack guards against that. */
				continue;
			}
			if (t.getPos().getPosValue().matches("^NN.*")) {
				focusTok = t;
				focus = focusTok;
				break;
			} else if (t.getPos().getPosValue().matches("^RB.*")) {
				focusTok = t;
			} else if (t.getPos().getPosValue().matches("^JJ.*")) {
				focusTok = t;
			} else if (t.getPos().getPosValue().matches("^CD.*")) {
				focusTok = t;
			}
			focus = focusTok;
		}

		if (focusTok != null)
			return new FocusPair(focusTok, focus);
		else
			return null;
	}

	protected void addFocusFeature(JCas jcas, String f) throws AnalysisEngineProcessException {
		AnswerInfo ai = JCasUtil.selectSingle(jcas, AnswerInfo.class);
		AnswerFV fv = new AnswerFV(ai);
		fv.setFeature(f, 1.0);

		if (ai.getFeatures() != null)
			for (FeatureStructure af : ai.getFeatures().toArray())
				((AnswerFeature) af).removeFromIndexes();
		ai.removeFromIndexes();

		ai.setFeatures(fv.toFSArray(jcas));
		ai.addToIndexes();
	}
}
