package cz.brmlab.yodaqa.analysis.passage;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
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
import cz.brmlab.yodaqa.model.SearchResult.QuestionLATMatch;

/**
 * Abstract base class for generators of CandidateAnswer from PickedPassage.
 * They might use different criteria, but re-use common mechanics regarding
 * appropriate feature generation and such. */

public abstract class CandidateGenerator extends JCasAnnotator_ImplBase {
	protected Logger logger;

	protected void addCandidateAnswer(JCas passagesView, Passage p, Annotation np, AnswerFV fv)
			throws AnalysisEngineProcessException {

		if (logger == null)
			System.err.println("logger is null");
		logger.info("np {}", np);
		logger.info("can {}", np.getCoveredText());

		fv.setFeature(AF_Occurences.class, 1.0);
		fv.setFeature(AF_PassageLogScore.class, Math.log(1 + p.getScore()));

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

		CandidateAnswer ca = new CandidateAnswer(passagesView);
		ca.setBegin(np.getBegin());
		ca.setEnd(np.getEnd());
		ca.setPassage(p);
		ca.setBase(np);
		ca.setFeatures(fv.toFSArray(passagesView));
		ca.addToIndexes();
	}
}
