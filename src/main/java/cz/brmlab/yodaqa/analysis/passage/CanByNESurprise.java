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
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginPsgNE;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginPsgSurprise;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_PassageLogScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorPassageDist;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorPassageInside;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorPassageSp;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;
import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.model.SearchResult.QuestionLATMatch;
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

public class CanByNESurprise extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(CanByNESurprise.class);

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

				logger.info("caNE {}", ne.getCoveredText());

				AnswerFV fv = new AnswerFV(ri.getAnsfeatures());
				fv.merge(new AnswerFV(p.getAnsfeatures()));
				fv.setFeature(AF_Occurences.class, 1.0);
				fv.setFeature(AF_PassageLogScore.class, Math.log(1 + p.getScore()));
				fv.setFeature(AF_OriginPsgNE.class, 1.0);
				if (!matches) {
					/* Surprise! */
					fv.setFeature(AF_OriginPsgSurprise.class, 1.0);
				}
				for (QuestionLATMatch qlm : JCasUtil.selectCovered(QuestionLATMatch.class, p)) {
					double distance = 1000;
					if (qlm.getBegin() >= ne.getBegin() && qlm.getEnd() <= ne.getEnd()) {
						distance = 0; // contained inside!
						fv.setFeature(AF_TyCorPassageInside.class, 1.0);
					} else if (qlm.getEnd() <= ne.getBegin()) {
						distance = ne.getBegin() - qlm.getEnd() - 1;
					} else if (qlm.getBegin() >= ne.getEnd()) {
						distance = qlm.getBegin() - ne.getEnd() - 1;
					}
					fv.setFeature(AF_TyCorPassageDist.class, Math.exp(-distance));
					fv.setFeature(AF_TyCorPassageSp.class, Math.exp(qlm.getBaseLAT().getSpecificity()) * Math.exp(-distance));
					// this should be a singleton
					break;
				}

				CandidateAnswer ca = new CandidateAnswer(passagesView);
				ca.setBegin(ne.getBegin());
				ca.setEnd(ne.getEnd());
				ca.setPassage(p);
				ca.setBase(ne);
				ca.setFeatures(fv.toFSArray(passagesView));
				ca.addToIndexes();
			}
		}
	}
}
