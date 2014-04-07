package cz.brmlab.yodaqa.pipeline;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;

/**
 * Take an input CAS and generate per-answer CAS instances.
 *
 * So far, this answer generator is super-primitive, just to showcase
 * a CAS multiplier behavior. Just splits the input CAS sofa
 * (the question) by space to individual "answers". */

public class AnswerGenerator extends JCasMultiplier_ImplBase {
	/* Prepared list of answers to return. */
	String[] answers;
	int i;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		answers = jcas.getDocumentText().split(" ");
		i = 0;
	}

	public boolean hasNext() throws AnalysisEngineProcessException {
		return i < answers.length;
	}

	public AbstractCas next() throws AnalysisEngineProcessException {
		JCas jcas = getEmptyJCas();
		try {
			jcas.setDocumentText(answers[i++]);

			AnswerInfo ai = new AnswerInfo(jcas);
			ai.setConfidence(1.0 / i);
			ai.addToIndexes();
		} catch (Exception e) {
			jcas.release();
			throw new AnalysisEngineProcessException(e);
		}
		return jcas;
	}
}
