package cz.brmlab.yodaqa.analysis.passage;

import java.util.LinkedList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.answer.AnswerFV;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_Occurences;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginNP;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_PassageLogScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_ResultLogScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;
import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.NP;

/**
 * Create CandidateAnswers for all NP constituents (noun phrases) that do not
 * contain supplied clues.
 *
 * This is pretty naive but should generate some useful answers. */

@SofaCapability(
	inputSofas = { "Question", "Result", "PickedPassages" },
	outputSofas = { "PickedPassages" }
)

public class CanByNPSurprise extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(CanByNPSurprise.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, resultView, passagesView;
		try {
			questionView = jcas.getView("Question");
			resultView = jcas.getView("Result");
			passagesView = jcas.getView("PickedPassages");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}
		for (NP np : JCasUtil.select(passagesView, NP.class)) {
			String text = np.getCoveredText();

			/* TODO: This can be optimized a lot. */
			boolean matches = false;
			for (Clue clue : JCasUtil.select(questionView, Clue.class)) {
				if (text.endsWith(clue.getLabel())) {
					matches = true;
					break;
				}
			}
			if (matches)
				continue;

			/* Surprise! */

			logger.info("caNP {}", np.getCoveredText());

			Passage p = JCasUtil.selectCovering(Passage.class, np).get(0);
			AnswerFV fv = new AnswerFV();
			fv.setFeature(AF_Occurences.class, 1.0);
			fv.setFeature(AF_PassageLogScore.class, Math.log(1 + p.getScore()));
			fv.setFeature(AF_ResultLogScore.class, Math.log(1 + JCasUtil.selectSingle(resultView, ResultInfo.class).getRelevance()));
			fv.setFeature(AF_OriginNP.class, 1.0);

			CandidateAnswer ca = new CandidateAnswer(passagesView);
			ca.setBegin(np.getBegin());
			ca.setEnd(np.getEnd());
			ca.setPassage(p);
			ca.setBase(np);
			ca.setFeatures(fv.toFSArray(passagesView));
			ca.addToIndexes();
		}
	}
}
