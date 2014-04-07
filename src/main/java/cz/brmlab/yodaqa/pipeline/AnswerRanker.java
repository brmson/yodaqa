package cz.brmlab.yodaqa.pipeline;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.FinalAnswer.Answer;

/**
 * Take a set of per-answer CAS and merge them to a final result CAS.
 *
 * So far, this answer ranker is super-primitive, just to showcase
 * a merging CAS multiplier behavior. But it actually does the job. */

public class AnswerRanker extends JCasMultiplier_ImplBase {
	JCas finalCas;
	boolean isLast;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		isLast = false;
	}

	public void process(JCas candCas) throws AnalysisEngineProcessException {
		if (finalCas == null)
			finalCas = getEmptyJCas();

		Answer answer = new Answer(finalCas);
		answer.setText(candCas.getDocumentText());

		FSIterator infos = candCas.getJFSIndexRepository().getAllIndexedFS(AnswerInfo.type);
		if (infos.hasNext()) {
			AnswerInfo info = (AnswerInfo) infos.next();
			answer.setConfidence(info.getConfidence());
			isLast = info.getIsLast();
		}
		// System.err.println("AR process: " + answer.getText() + " c " + answer.getConfidence());

		answer.addToIndexes();
	}

	public boolean hasNext() throws AnalysisEngineProcessException {
		return isLast;
	}

	public AbstractCas next() throws AnalysisEngineProcessException {
		if (!isLast)
			throw new AnalysisEngineProcessException();

		JCas outputCas = finalCas;
		finalCas = null;
		isLast = false;
		return outputCas;
	}
}
