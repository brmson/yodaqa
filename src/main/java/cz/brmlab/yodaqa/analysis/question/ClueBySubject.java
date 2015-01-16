package cz.brmlab.yodaqa.analysis.question;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.Question.ClueSubjectNE;
import cz.brmlab.yodaqa.model.Question.ClueSubjectToken;
import cz.brmlab.yodaqa.model.Question.ClueSubjectPhrase;
import cz.brmlab.yodaqa.model.Question.Subject;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.NP;

/**
 * Generate Clue annotations in a QuestionCAS. These represent key information
 * stored in the question that is then used in primary search.
 *
 * This generates clues from the question subject, i.e. NSUBJ annotation.
 * E.g. in "When did Einstein die?", subject is "Einstein" and will have such
 * a clue generated. */

public class ClueBySubject extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(ClueBySubject.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		for (Subject s : JCasUtil.select(jcas, Subject.class)) {
			/* Single-token subjects are treated as reliable
			 * indicators.  Phrasal subjects aren't, as they
			 * may have overly specific phrasing.  NamedEntity
			 * subjects are treated as reliable indicators but
			 * have lower height. */
			if (s.getBase() instanceof NamedEntity)
				addClue(new ClueSubjectNE(jcas), s.getBegin(), s.getEnd(), s, true, 2.2);
			else if (s.getBase() instanceof Token)
				addClue(new ClueSubjectToken(jcas), s.getBegin(), s.getEnd(), s, true, 2.5);
			else if (s.getBase() instanceof NP)
				addClue(new ClueSubjectPhrase(jcas), s.getBegin(), s.getEnd(), s, false, 2.7);
			else assert(false);
		}
	}

	protected void addClue(Clue clue, int begin, int end, Subject base, boolean isReliable, double weight) {
		clue.setBegin(begin);
		clue.setEnd(end);
		clue.setBase(base);
		clue.setWeight(weight);
		clue.setLabel(clue.getCoveredText());
		clue.setIsReliable(isReliable);
		clue.addToIndexes();
		logger.debug("new by {} {}: {}", base.getType().getShortName(), base.getBase().getType().getShortName(), clue.getLabel());
	}
}
