package cz.brmlab.yodaqa.analysis.passage;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;

/**
 * Create CandidateAnswers for all NEs (named entities) that do not
 * contain supplied clues.
 *
 * This is pretty naive but should generate some useful answers. */

@SofaCapability(
	inputSofas = { "Question", "Result", "PickedPassages" },
	outputSofas = { "PickedPassages" }
)

public class CanByNESurprise extends CandidateGenerator {
	public CanByNESurprise() {
		logger = LoggerFactory.getLogger(CanByNESurprise.class);
	}

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

		ResultInfo ri = JCasUtil.selectSingle(resultView, ResultInfo.class);

		for (Passage p: JCasUtil.select(passagesView, Passage.class)) {
			for (NamedEntity ne : JCasUtil.selectCovered(NamedEntity.class, p)) {
				String text = ne.getCoveredText();

				/* TODO: This can be optimized a lot. */
				boolean matches = false;
				for (Clue clue : JCasUtil.select(questionView, Clue.class)) {
					if (text.endsWith(clue.getLabel())) {
						matches = true;
						break;
					}
				}

				AnswerFV fv = new AnswerFV(ri.getAnsfeatures());
				fv.merge(new AnswerFV(p.getAnsfeatures()));
				fv.setFeature(AF.OriginPsgNE, 1.0);
				if (!matches) {
					/* Surprise! */
					fv.setFeature(AF.OriginPsgSurprise, 1.0);
				}

				addCandidateAnswer(passagesView, p, ne, fv);
			}
		}
	}
}
