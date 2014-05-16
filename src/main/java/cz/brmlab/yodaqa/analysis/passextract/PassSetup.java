package cz.brmlab.yodaqa.analysis.passextract;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Setup a Passages view. */

@SofaCapability(
	inputSofas = { "Question", "Result" },
	outputSofas = { "Passages" }
)


public class PassSetup extends JCasAnnotator_ImplBase {
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas resultView, passagesView;
		try {
			resultView = jcas.getView("Result");
			jcas.createView("Passages");
			passagesView = jcas.getView("Passages");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}
		passagesView.setDocumentText(resultView.getDocumentText());
		passagesView.setDocumentLanguage(resultView.getDocumentLanguage());
	}
}
