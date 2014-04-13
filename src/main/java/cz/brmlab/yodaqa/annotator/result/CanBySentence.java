package cz.brmlab.yodaqa.annotator.result;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;

/**
 * Create CandidateAnswers simply from whole filtered sentences.
 *
 * This is just a naive generator for debugging. */

@SofaCapability(
	inputSofas = { "Passages" },
	outputSofas = { "Passages" }
)

public class CanBySentence extends JCasAnnotator_ImplBase {
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		try {
			jcas = jcas.getView("Passages");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}
		for (Sentence sentence : JCasUtil.select(jcas, Sentence.class)) {
			System.err.println("ca " + sentence.getCoveredText());
			CandidateAnswer ca = new CandidateAnswer(jcas);
			ca.setBegin(sentence.getBegin());
			ca.setEnd(sentence.getEnd());
			ca.setBase(sentence);
			ca.setConfidence(1.0);
			ca.addToIndexes();
		}
	}
}
