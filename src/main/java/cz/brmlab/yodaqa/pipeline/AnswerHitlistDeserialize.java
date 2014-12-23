package cz.brmlab.yodaqa.pipeline;

import java.io.FileInputStream;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.QuestionInfo;

/**
 * Serialize the AnswerHitlistCAS to a file. */


public class AnswerHitlistDeserialize extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(AnswerHitlistDeserialize.class);

	/** Directory in which to store for serialized answer data. */
	public static final String PARAM_LOAD_DIR = "load-dir";
	@ConfigurationParameter(name = PARAM_LOAD_DIR, mandatory = true)
	protected String loadDir;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		QuestionInfo qi = JCasUtil.selectSingle(jcas, QuestionInfo.class);

		FileInputStream in = null;

		try {
			// load XMI
			String fileName = loadDir + "/" + qi.getQuestionId() + ".xmi";
			in = new FileInputStream(fileName);
			logger.debug("deserializing from {}", fileName);
			XmiCasDeserializer.deserialize(in, jcas.getCas());
			in.close();
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
	}
}
