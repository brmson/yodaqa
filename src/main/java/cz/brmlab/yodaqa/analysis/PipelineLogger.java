package cz.brmlab.yodaqa.analysis;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A trivial class for writing debug prints during pipeline run.
 * Useful for simple timing of pipeline phases. */

public class PipelineLogger extends JCasAnnotator_ImplBase {
	final static Logger logger = LoggerFactory.getLogger(PipelineLogger.class);

	public static final String PARAM_LOG_MESSAGE = "log-message";
	@ConfigurationParameter(name = PARAM_LOG_MESSAGE, mandatory = true)
	protected String logMessage;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		logger.debug(logMessage);
	}
}
