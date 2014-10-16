package cz.brmlab.yodaqa.analysis.passage;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.answer.AnswerFV;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_Occurences;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginPsgNP;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_PassageLogScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorPassageDist;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorPassageInside;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorPassageSp;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;
import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.model.SearchResult.QuestionLATMatch;
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

		ResultInfo ri = JCasUtil.selectSingle(resultView, ResultInfo.class);

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
			AnswerFV fv = new AnswerFV(ri.getAnsfeatures());
			fv.merge(new AnswerFV(p.getAnsfeatures()));
			fv.setFeature(AF_Occurences.class, 1.0);
			fv.setFeature(AF_PassageLogScore.class, Math.log(1 + p.getScore()));
			fv.setFeature(AF_OriginPsgNP.class, 1.0);
			for (QuestionLATMatch qlm : JCasUtil.selectCovered(QuestionLATMatch.class, p)) {
				double distance = 1000;
				if (qlm.getBegin() >= np.getBegin() && qlm.getEnd() <= np.getEnd()) {
					distance = 0; // contained inside!
					fv.setFeature(AF_TyCorPassageInside.class, 1.0);
				} else if (qlm.getEnd() <= np.getBegin()) {
					distance = np.getBegin() - qlm.getEnd() - 1;
				} else if (qlm.getBegin() >= np.getEnd()) {
					distance = qlm.getBegin() - np.getEnd() - 1;
				}
				fv.setFeature(AF_TyCorPassageDist.class, Math.exp(-distance));
				fv.setFeature(AF_TyCorPassageSp.class, Math.exp(qlm.getBaseLAT().getSpecificity()) * Math.exp(-distance));
				// this should be a singleton
				break;
			}

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
