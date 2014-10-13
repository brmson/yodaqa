package cz.brmlab.yodaqa.analysis.question;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ADVMOD;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DEP;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DET;
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

		/* Skip question word focuses (e.g. "Who"). */
		if (stok.getPos().getPosValue().matches("^W.*"))
			return;

		/* Prefer a covering Named Entity: */
		for (NamedEntity ne : JCasUtil.selectCovering(NamedEntity.class, stok)) {
			addSubject(jcas, ne);
			return;
		}
		/* But when there's none, just add the token. */
		addSubject(jcas, stok);
		/* The covering base noun phrase often doesn't work so well:
		 *
		 * What is the state song of Kansas? -> "the state song" (too short)
		 * What year was the first Macy's Thanksgiving Day Parade held? -> "the first Macy's Thanksgiving Day Parade" (too long)
		 *
		 * but when NamedEntity detection fails (e.g. "How high is
		 * Pikes peak?"), it's indispensible.  Let's create a Subject
		 * annotation for that too, and let ClueBySubject sort it
		 * out based on whether it has a token or constituent. */
		addSubject(jcas, stok.getParent());
	}

	protected void addSubject(JCas jcas, Annotation subj) throws AnalysisEngineProcessException {
		Subject s = new Subject(jcas);
		s.setBegin(subj.getBegin());
		s.setEnd(subj.getEnd());
		s.setBase(subj);
		s.addToIndexes();
	}
}
