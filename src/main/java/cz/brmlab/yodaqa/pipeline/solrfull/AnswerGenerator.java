package cz.brmlab.yodaqa.pipeline.solrfull;

import cz.brmlab.yodaqa.flow.dashboard.AnswerIDGenerator;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasMultiplier_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.flow.asb.MultiThreadASB;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceEnwiki;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
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

		qi = JCasUtil.selectSingle(questionView, QuestionInfo.class);
		ri = JCasUtil.selectSingle(resultView, ResultInfo.class);

		answers = pickedPassagesView.getJFSIndexRepository().getAllIndexedFS(CandidateAnswer.type);
		i = 0;
	}

	public boolean hasNext() throws AnalysisEngineProcessException {
		return answers.hasNext() || i == 0;
	}

	public AbstractCas next() throws AnalysisEngineProcessException {
		CandidateAnswer answer = null;
		if (answers.hasNext())
			answer = (CandidateAnswer) answers.next();
		i++;

		JCas jcas = getEmptyJCas();
		try {
			jcas.createView("Question");
			JCas canQuestionView = jcas.getView("Question");
			copyQuestion(questionView, canQuestionView);

			jcas.createView("Answer");
			JCas canAnswerView = jcas.getView("Answer");
			if (answer != null) {
				boolean isLast = !answers.hasNext();
				generateAnswer(answer, canAnswerView, isLast ? i : 0);
				if (isLast) {
 					QuestionDashboard.getInstance().get(questionView).setSourceState(ri.getSourceID(), 2);
				}
			} else {
				/* We will just generate a single dummy CAS
				 * to avoid flow breakage. */
				canAnswerView.setDocumentText("");
				canAnswerView.setDocumentLanguage(resultView.getDocumentLanguage());
				AnswerInfo ai = new AnswerInfo(canAnswerView);
				ai.setIsLast(i);
				ai.addToIndexes();
			}
			copyResultInfo(resultView, canAnswerView);

		} catch (Exception e) {
			jcas.release();
			throw new AnalysisEngineProcessException(e);
		}
		return jcas;
	}

	protected void copyQuestion(JCas src, JCas dest) throws Exception {
		CasCopier copier = new CasCopier(src.getCas(), dest.getCas());
		copier.copyCasView(src.getCas(), dest.getCas(), true);
	}

	protected void copyResultInfo(JCas src, JCas dest) throws Exception {
		CasCopier copier = new CasCopier(src.getCas(), dest.getCas());

		ResultInfo ri2 = (ResultInfo) copier.copyFs(ri);
		ri2.addToIndexes();
	}

	protected void generateAnswer(CandidateAnswer answer, JCas jcas,
			int isLast) throws Exception {
		jcas.setDocumentText(answer.getCoveredText());
		jcas.setDocumentLanguage(answer.getCAS().getDocumentLanguage());
		/* Grab answer features */
		AnswerFV srcFV = new AnswerFV(answer);

		/* Generate the AnswerInfo singleton */
		AnswerInfo ai = new AnswerInfo(jcas);
		ai.setFeatures(srcFV.toFSArray(jcas));
		ai.setIsLast(isLast);
		ai.setAnswerID(AnswerIDGenerator.getInstance().generateID());
		ai.setSnippetIDs(new IntegerArray(jcas, answer.getSnippetIDs().size()));
		ai.getSnippetIDs().copyFromArray(answer.getSnippetIDs().toArray(), 0, 0, answer.getSnippetIDs().size());
		ai.addToIndexes();
		CasCopier copier = new CasCopier(answer.getCAS(), jcas.getCas());

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
			Annotation a2 = (Annotation) copier.copyFs(a);
			a2.setBegin(a2.getBegin() - ofs);
			a2.setEnd(a2.getEnd() - ofs);
			a2.addToIndexes();
		}

	}

	@Override
	public int getCasInstancesRequired() {
		return MultiThreadASB.maxJobs * 2;
	}
}
