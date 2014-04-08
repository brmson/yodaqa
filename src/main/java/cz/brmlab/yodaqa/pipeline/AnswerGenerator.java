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
import org.apache.uima.util.CasCopier;

import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;

/**
 * Take an input CAS and generate per-answer CAS instances.
 *
 * So far, this answer generator is super-primitive, just to showcase
 * a CAS multiplier behavior. Just splits the input CAS sofa
 * (the question) by space to individual "answers". */

public class AnswerGenerator extends JCasMultiplier_ImplBase {
	JCas src_jcas;
	ResultInfo ri;

	/* Prepared list of answers to return. */
	String[] answers;
	int i;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		answers = jcas.getDocumentText().split(" ");
		i = 0;

		src_jcas = jcas;
		ri = (ResultInfo) jcas.getJFSIndexRepository().getAllIndexedFS(ResultInfo.type).next();
	}

	public boolean hasNext() throws AnalysisEngineProcessException {
		return i < answers.length;
	}

	public AbstractCas next() throws AnalysisEngineProcessException {
		JCas jcas = getEmptyJCas();
		CasCopier copier = new CasCopier(src_jcas.getCas(), jcas.getCas());
		try {
			jcas.setDocumentText(answers[i]);

			AnswerInfo ai = new AnswerInfo(jcas);
			ai.setConfidence(1.0 / (i + 1.0));
			ai.setIsLast(i == answers.length - 1);
			ai.addToIndexes();

			ResultInfo ri2 = (ResultInfo) copier.copyFs(ri);
			ri2.addToIndexes();
		} catch (Exception e) {
			jcas.release();
			throw new AnalysisEngineProcessException(e);
		}
		i++;
		return jcas;
	}
}
