package cz.brmlab.yodaqa.analysis.question;

import java.util.LinkedList;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;

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

import cz.brmlab.yodaqa.analysis.answer.SyntaxCanonization;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.Question.CluePhrase;
import cz.brmlab.yodaqa.model.Question.ClueToken;

/**
 * Generate Clue annotations in a QuestionCAS. These represent key information
 * stored in the question that is then used in primary search.  E.g. "What was
 * the first book written by Terry Pratchett?" should generate clues "first",
 * "book", "first book", "write" and "Terry Pratchett".
 *
 * This one creates clues from a bunch of POS-interesting tokens (like nouns,
 * numbers, etc.) and also interesting constituents like noun phrases. */

public class ClueByTokenConstituent extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(ClueByTokenConstituent.class);

	public static String TOKENMATCH = "k1.*|UNKNOWN|k2.*|k4.*|k6.*";

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* XXX we don't do constituents in Czech */

		for (Token t : JCasUtil.select(jcas, Token.class)) {
			if (t.getPos().getPosValue().matches(TOKENMATCH)) {
				if (t.getEnd() - t.getBegin() > 1) // skip one-letter things like interpunction
					addClue(new ClueToken(jcas), t.getBegin(), t.getEnd(), t, true, 1.0);
			}
		}
	}

	protected void addClue(Clue clue, int begin, int end, Annotation base, boolean isReliable, double weight) {
		clue.setBegin(begin);
		clue.setEnd(end);
		clue.setBase(base);
		clue.setWeight(weight);
		clue.setLabel(SyntaxCanonization.getCanonText(clue.getCoveredText()));
		clue.setIsReliable(isReliable);
		clue.addToIndexes();
		logger.debug("new by {}: {}", base.getType().getShortName(), clue.getLabel());
	}
}
