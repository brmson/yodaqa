package cz.brmlab.yodaqa.analysis.result;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;
import cz.brmlab.yodaqa.model.SearchResult.Passage;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.NP;

/**
 * Create CandidateAnswers for all NP constituents (noun phrases) that do not
 * contain supplied clues.
 *
 * This is pretty naive but should generate some useful answers. */

@SofaCapability(
	inputSofas = { "Question", "PickedPassages" },
	outputSofas = { "Result" }
)

public class CanByNPSurprise extends JCasAnnotator_ImplBase {
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, passagesView, resultView;
		try {
			questionView = jcas.getView("Question");
			passagesView = jcas.getView("PickedPassages");
			resultView = jcas.getView("Result");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}
		for (NP np : JCasUtil.select(passagesView, NP.class)) {
			String text = np.getCoveredText();

			/* TODO: This can be optimized a lot. */
			boolean matches = false;
			for (Clue clue : JCasUtil.select(questionView, Clue.class)) {
				if (text.contains(clue.getCoveredText())) {
					matches = true;
					break;
				}
			}
			if (matches)
				continue;

			/* Surprise! */

			System.err.println("ca " + np.getCoveredText());
			CandidateAnswer ca = new CandidateAnswer(resultView);
			ca.setBegin(np.getBegin());
			ca.setEnd(np.getEnd());
			ca.setPassage(JCasUtil.selectCovering(Passage.class, np).get(0));
			ca.setBase(np);
			ca.setConfidence(1.0);
			ca.addToIndexes();
		}
	}
}
