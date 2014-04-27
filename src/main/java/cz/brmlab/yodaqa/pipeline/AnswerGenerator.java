package cz.brmlab.yodaqa.pipeline;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasMultiplier_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;

import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;

/**
 * Take an input ResultCAS and generate per-answer CandidateAnswerCAS
 * instances.
 *
 * We are a simple CAS multiplier that creates a dedicated CAS for
 * each to-be-analyzed candidate answer. */

public class AnswerGenerator extends JCasMultiplier_ImplBase {
	JCas questionView, resultView, pickedPassagesView;
	QuestionInfo qi;
	ResultInfo ri;

	/* Prepared list of answers to return. */
	FSIterator answers;
	int i;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		try {
			questionView = jcas.getView("Question");
			resultView = jcas.getView("Result");
			pickedPassagesView = jcas.getView("PickedPassages");
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		qi = (QuestionInfo) questionView.getJFSIndexRepository().getAllIndexedFS(QuestionInfo.type).next();
		ri = (ResultInfo) resultView.getJFSIndexRepository().getAllIndexedFS(ResultInfo.type).next();

		answers = pickedPassagesView.getJFSIndexRepository().getAllIndexedFS(CandidateAnswer.type);
		i = 0;
	}

	public boolean hasNext() throws AnalysisEngineProcessException {
		return answers.hasNext();
	}

	public AbstractCas next() throws AnalysisEngineProcessException {
		JCas jcas = getEmptyJCas();
		CasCopier qCopier = new CasCopier(questionView.getCas(), jcas.getCas());
		CasCopier rCopier = new CasCopier(resultView.getCas(), jcas.getCas());
		CasCopier pCopier = new CasCopier(pickedPassagesView.getCas(), jcas.getCas());
		try {
			CandidateAnswer answer = (CandidateAnswer) answers.next();
			jcas.setDocumentText(answer.getCoveredText());
			jcas.setDocumentLanguage(resultView.getDocumentLanguage());

			AnswerInfo ai = new AnswerInfo(jcas);
			ai.setConfidence(answer.getConfidence() * answer.getPassage().getScore());
			ai.setIsLast(!answers.hasNext());
			ai.addToIndexes();

			/* Copy in-answer annotations */
			int ofs = answer.getBegin();
			for (Annotation a : JCasUtil.selectCovered(Annotation.class, answer)) {
				if (a instanceof CandidateAnswer)
					continue;
				/* If we already deep-copied the annotation,
				 * here we get its reference to fix things up.
				 * XXX: We don't get a chance to remove e.g.
				 * outside-reaching dependency annotations;
				 * casCopier is a kind of silly interface. */
				Annotation a2 = (Annotation) pCopier.copyFs(a);
				a2.setBegin(a2.getBegin() - ofs);
				a2.setEnd(a2.getEnd() - ofs);
				a2.addToIndexes();
			}

			QuestionInfo qi2 = (QuestionInfo) qCopier.copyFs(qi);
			qi2.addToIndexes();
			ResultInfo ri2 = (ResultInfo) rCopier.copyFs(ri);
			ri2.addToIndexes();

		} catch (Exception e) {
			jcas.release();
			throw new AnalysisEngineProcessException(e);
		}
		i++;
		return jcas;
	}
}
