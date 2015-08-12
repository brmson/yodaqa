package cz.brmlab.yodaqa.analysis.passage;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_Occurences;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_PassageLogScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorPassageDist;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorPassageInside;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorPassageSp;
import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;
import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.model.SearchResult.QuestionClueMatch;
import cz.brmlab.yodaqa.model.SearchResult.QuestionLATMatch;

/* XXX: The clue-specific features, ugh. */
import cz.brmlab.yodaqa.model.Question.*;
import cz.brmlab.yodaqa.model.CandidateAnswer.*;

/**
 * Abstract base class for generators of CandidateAnswer from PickedPassage.
 * They might use different criteria, but re-use common mechanics regarding
 * appropriate feature generation and such. */

public abstract class CandidateGenerator extends JCasAnnotator_ImplBase {
	protected Logger logger;

	protected void addCandidateAnswer(JCas passagesView, Passage p, Annotation np, AnswerFV fv)
			throws AnalysisEngineProcessException {

		logger.info("can {}", np.getCoveredText());

		fv.setFeature(AF_Occurences.class, 1.0);
		fv.setFeature(AF_PassageLogScore.class, p.getScore());

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
		     if (clue instanceof ClueToken     ) afv.setFeature(AF_PsgDistClueToken.class, value);
		else if (clue instanceof CluePhrase    ) afv.setFeature(AF_PsgDistCluePhrase.class, value);
		else if (clue instanceof ClueSV        ) afv.setFeature(AF_PsgDistClueSV.class, value);
		else if (clue instanceof ClueNE        ) afv.setFeature(AF_PsgDistClueNE.class, value);
		else if (clue instanceof ClueLAT       ) afv.setFeature(AF_PsgDistClueLAT.class, value);
		else if (clue instanceof ClueSubject   ) {
			afv.setFeature(AF_PsgDistClueSubject.class, value);
			     if (clue instanceof ClueSubjectNE) afv.setFeature(AF_PsgDistClueSubjectNE.class, value);
			else if (clue instanceof ClueSubjectToken) afv.setFeature(AF_PsgDistClueSubjectToken.class, value);
			else if (clue instanceof ClueSubjectPhrase) afv.setFeature(AF_PsgDistClueSubjectPhrase.class, value);
			else assert(false);
		} else if (clue instanceof ClueConcept ) {
			afv.setFeature(AF_PsgDistClueConcept.class, value);
			ClueConcept concept = (ClueConcept) clue;
			if (concept.getBySubject())
				afv.setFeature(AF_PsgDistClueSubject.class, value);
			if (concept.getByLAT())
				afv.setFeature(AF_PsgDistClueLAT.class, value);
			if (concept.getByNE())
				afv.setFeature(AF_PsgDistClueNE.class, value);
		}
		else assert(false);
	}
}
