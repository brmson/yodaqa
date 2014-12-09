package cz.brmlab.yodaqa.pipeline;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.XMLSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.QuestionInfo;

/**
 * Serialize the AnswerHitlistCAS to a file. */


public class AnswerHitlistSerialize extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(AnswerHitlistSerialize.class);

	/** Directory in which to store for serialized answer data. */
	public static final String PARAM_SAVE_DIR = "save-dir";
	@ConfigurationParameter(name = PARAM_SAVE_DIR, mandatory = true)
	protected String saveDir;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		QuestionInfo qi;
		try {
			qi = JCasUtil.selectSingle(jcas.getView("Question"), QuestionInfo.class);
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		FileOutputStream out = null;

		try {
			(new File(saveDir)).mkdir();
			// write XMI
			String fileName = saveDir + "/" + qi.getQuestionId() + ".xmi";
			out = new FileOutputStream(fileName);
			logger.debug("serializing to {}", fileName);
			XmiCasSerializer ser = new XmiCasSerializer(jcas.getTypeSystem());
			XMLSerializer xmlSer = new XMLSerializer(out, false);
			ser.serialize(jcas.getCas(), xmlSer.getContentHandler());
			out.close();
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
	}
}
