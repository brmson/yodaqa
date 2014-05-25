package cz.brmlab.yodaqa.analysis.question;

import java.util.List;

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
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.Question.ClueSubject;
import cz.brmlab.yodaqa.model.Question.ClueSubjectAux;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NSUBJ;

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
		for (ROOT sentence : JCasUtil.select(jcas, ROOT.class)) {
			processSentence(jcas, sentence);
		}
	}

	public void processSentence(JCas jcas, Constituent sentence) throws AnalysisEngineProcessException {
		for (NSUBJ subj : JCasUtil.selectCovered(NSUBJ.class, sentence)) {
			Token stok = subj.getDependent();

			/* Skip question word focuses (e.g. "Who"). */
			if (stok.getPos().getPosValue().matches("^W.*"))
				continue;

			/* If the subject governs an object, e.g.
			 * "Holmes' name" or "capital of Argentina",
			 * switch the subject to "Holmes" and "Argentina"
			 * and keep the original only with lower priority. */
			List<Token> objDeps = TreeUtil.getAllGoverned(jcas, sentence, stok, "pobj|poss");
			if (!objDeps.isEmpty()) {
				for (Token objDep : objDeps)
					addClue(new ClueSubject(jcas), objDep.getBegin(), objDep.getEnd(), objDep, 2.0);

				addClue(new ClueSubject(jcas), subj.getBegin(), subj.getEnd(), subj, 1.0);
			} else {
				addClue(new ClueSubject(jcas), subj.getBegin(), subj.getEnd(), subj, 2.5);
			}
		}
	}

	protected void addClue(Clue clue, int begin, int end, Annotation base, double weight) {
		clue.setBegin(begin);
		clue.setEnd(end);
		clue.setBase(base);
		clue.setWeight(weight);
		clue.addToIndexes();
		logger.debug("new by {}: {}", base.getType().getShortName(), clue.getCoveredText());
	}
}
