package cz.brmlab.yodaqa.analysis.question;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.NP;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.WHNP;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NSUBJ;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NSUBJPASS;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.TreeUtil;
import cz.brmlab.yodaqa.analysis.answer.SyntaxCanonization;
import cz.brmlab.yodaqa.model.Question.Subject;

/**
 * Subject annotations in a QuestionCAS. These represent key information
 * stored in the question that is then used in primary search.
 *
 * This generates clues from the question subject, i.e. NSUBJ annotation.
 * E.g. in "When did Einstein die?", subject is "Einstein" and will have such
 * a clue generated. */

public class SubjectGenerator extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(SubjectGenerator.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		for (ROOT sentence : JCasUtil.select(jcas, ROOT.class)) {
			processSentence(jcas, sentence);
		}
	}

	protected void processSentence(JCas jcas, Constituent sentence) throws AnalysisEngineProcessException {
		for (NSUBJ subj : JCasUtil.selectCovered(NSUBJ.class, sentence))
			processSubj(jcas, subj);
		for (NSUBJPASS subj : JCasUtil.selectCovered(NSUBJPASS.class, sentence))
			processSubj(jcas, subj);
	}

	protected void processSubj(JCas jcas, Dependency subj) throws AnalysisEngineProcessException {
		Token stok = subj.getDependent();
		Annotation parent = stok.getParent();
		Constituent cparent = null;
		if (parent != null && parent instanceof Constituent)
			cparent = (Constituent) parent;

		/* Skip question word focuses (e.g. "Who"). */
		if (stok.getPos().getPosValue().matches("^W.*"))
			return;
		/* In "What country is Berlin in?", "country" (with
		 * parent "What country" WHNP) is *also* a NSUBJ
		 * - skip that one. */
		if (cparent instanceof WHNP)
			return;

		String genSubject = null;

		/* Prefer a covering Named Entity: */
		boolean genSubjectNE = false;
		for (NamedEntity ne : JCasUtil.selectCovering(NamedEntity.class, stok)) {
			addSubject(jcas, ne);
			genSubject = ne.getCoveredText();
			genSubjectNE = true;
		}

		/* N.B. Sometimes NamedEntity detection fails (e.g. "How high
		 * is Pikes peak?"). So when there's none, just add the token
		 * as the subject. */
		/* But do not add subjects like "it". */
		if (genSubject == null
		    && stok.getPos().getPosValue().matches(ClueByTokenConstituent.TOKENMATCH)) {
			addSubject(jcas, stok);
			genSubject = stok.getCoveredText();
		}

		/* However, just the token is often pretty useless, yielding
		 * e.g.
		 * - How hot does it get in Death Valley? (it)
		 * - What is the southwestern-most tip of England? (tip)
		 * - What is the capital of Laos? (capital)
		 * - What is the motto for California? (motto)
		 * - What is the name of the second Beatles album? (name)
		 * so we rather add the widest covering NP (e.g.
		 * "capital of Laos"). */
		/* (Adding just the token, e.g. "capital", above too also makes
		 * sense as it can be treated as reliable compared to the full
		 * phrase which may not be in the text word-by-word.) */
		NP np = TreeUtil.widestCoveringNP(stok);
		if (np == null) {
			// <<How long before bankruptcy is removed from a credit report?>>
			return;
		} else if (np.getCoveredText().equals(genSubject)) {
			// <<it>> is often a NP too, or other short tokens
			return;
		}
		addSubject(jcas, np);

		/* However, if there *is* a NamedEntity in the covering NP,
		 * add it as a subject too - NamedEntity subject clues can
		 * be treated as reliable. */
		if (!genSubjectNE) {
			for (NamedEntity ne : JCasUtil.selectCovered(NamedEntity.class, np)) {
				addSubject(jcas, ne);
			}
		}

		/* Also generate subject for the shortest covering NP, which is
		 * often just a very specific phrase like 'main character' or
		 * 'middle name', useful as e.g. a property selector. */
		NP npShort = TreeUtil.shortestCoveringNP(stok);
		if (npShort != np && !npShort.getCoveredText().equals(genSubject)) {
			/* XXX: Blacklisting "name" in "the name of XYZ".
			 * We probably don't need a sophisticated name
			 * proxy like for LATs. */
			if (!SyntaxCanonization.getCanonText(npShort.getCoveredText().toLowerCase()).equals("name"))
				addSubject(jcas, npShort);
		}
	}

	protected void addSubject(JCas jcas, Annotation subj) throws AnalysisEngineProcessException {
		Subject s = new Subject(jcas);
		s.setBegin(subj.getBegin());
		s.setEnd(subj.getEnd());
		s.setBase(subj);
		s.addToIndexes();
	}
}
