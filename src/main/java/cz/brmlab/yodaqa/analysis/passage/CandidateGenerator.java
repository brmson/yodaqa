package cz.brmlab.yodaqa.analysis.passage;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.Question.ClueConcept;
import cz.brmlab.yodaqa.model.Question.ClueSubject;
import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;
import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.model.SearchResult.QuestionClueMatch;
import cz.brmlab.yodaqa.model.SearchResult.QuestionLATMatch;

/**
 * Abstract base class for generators of CandidateAnswer from PickedPassage.
 * They might use different criteria, but re-use common mechanics regarding
 * appropriate feature generation and such. */

public abstract class CandidateGenerator extends JCasAnnotator_ImplBase {
	protected Logger logger;

	protected void addCandidateAnswer(JCas passagesView, Passage p, Annotation np, AnswerFV fv)
			throws AnalysisEngineProcessException {

		logger.info("can {}", np.getCoveredText());

		fv.setFeature(AF.Occurences, 1.0);
		fv.setFeature(AF.PassageLogScore, Math.log(1 + p.getScore()));

		for (QuestionLATMatch qlm : JCasUtil.selectCovered(QuestionLATMatch.class, p)) {
			double distance = 1000;
			if (qlm.getBegin() >= np.getBegin() && qlm.getEnd() <= np.getEnd()) {
				distance = 0; // contained inside!
				fv.setFeature(AF.TyCorPassageInside, 1.0);
			} else if (qlm.getEnd() <= np.getBegin()) {
				distance = np.getBegin() - qlm.getEnd() - 1;
			} else if (qlm.getBegin() >= np.getEnd()) {
				distance = qlm.getBegin() - np.getEnd() - 1;
			}
			fv.setFeature(AF.TyCorPassageDist, Math.exp(-distance));
			fv.setFeature(AF.TyCorPassageSp, Math.exp(qlm.getBaseLAT().getSpecificity()) * Math.exp(-distance));
			logger.debug("Passage TyCor (d {}, contains {})", distance, qlm.getBaseLAT().getText());
			// this should be a singleton
			break;
		}

		for (QuestionClueMatch qlc : JCasUtil.selectCovered(QuestionClueMatch.class, p)) {
			double distLeft = np.getBegin() - qlc.getEnd();
			double distRight = qlc.getBegin() - np.getEnd();
			double distance;
			if (distLeft >= 0 && distRight >= 0) {
				// contained inside!
				distance = -1;
			} else if (distLeft >= 0) {
				distance = distLeft;
			} else if (distRight >= 0) {
				distance = distRight;
			} else {
				// some strange overlap
				distance = -1;
			}
			double value = Math.exp(-distance);
			clueDistAnswerFeature(fv, qlc.getBaseClue(), value);
			logger.debug("ClueMatch {}/<{}> d {}",
				qlc.getBaseClue().getClass().getSimpleName(),
				qlc.getBaseClue().getLabel(), distance);
			// this should be a singleton
			break;
		}

		CandidateAnswer ca = new CandidateAnswer(passagesView);
		ca.setBegin(np.getBegin());
		ca.setEnd(np.getEnd());
		ca.setPassage(p);
		ca.setBase(np);
		ca.setFeatures(fv.toFSArray(passagesView));
		//Snippet added this way should already have the sourceID saved
		ca.setSnippetIDs(new IntegerArray(passagesView, 1));
		ca.setSnippetIDs(0, p.getSnippetID());
		ca.addToIndexes();
	}

	protected void clueDistAnswerFeature(AnswerFV afv, Clue clue, double value) {
		afv.setFeature(AF.PsgDist_ClueType + clue.getType().getShortName(), value);
		if (clue instanceof ClueSubject) {
			afv.setFeature(AF.PsgDist_ClueType + "ClueSubject", value);
		} else if (clue instanceof ClueConcept) {
			double bestSourceRr = 0, bestLabelRr = 0, bestScore = 0;
			for (Concept concept : FSCollectionFactory.create(((ClueConcept) clue).getConcepts(), Concept.class)) {
				if (concept.getBySubject())
					afv.setFeature(AF.PsgDist_ClueType + "ClueSubject", value);
				if (concept.getByLAT())
					afv.setFeature(AF.PsgDist_ClueType + "ClueLAT", value);
				if (concept.getByNE())
					afv.setFeature(AF.PsgDist_ClueType + "ClueNE", value);
				if (concept.getByNgram())
					afv.setFeature(AF.PsgDist_ClueType + "ClueNgram", value);
				if (concept.getSourceRr() > bestSourceRr)
					bestSourceRr = concept.getSourceRr();
				if (concept.getLabelRr() > bestLabelRr)
					bestLabelRr = concept.getLabelRr();
				if (concept.getScore() > bestScore)
					bestScore = concept.getScore();
			}
			afv.setFeature(AF.PsgDist_ClueType + AF._clueType_ConceptSourceRR, bestSourceRr);
			afv.setFeature(AF.PsgDist_ClueType + AF._clueType_ConceptLabelRR, bestLabelRr);
			afv.setFeature(AF.PsgDist_ClueType + AF._clueType_ConceptScore, bestScore);
		}
	}
}
