package cz.brmlab.yodaqa.io.qald5;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import cz.brmlab.yodaqa.model.Question.GSAnswer;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;


/**
 * A loader that loads a collection of questions and possibly answers from
 * a QALD-5 type XML file. */

public class QALD5QuestionReader extends CasCollectionReader_ImplBase {
	/**
	 * Name of optional configuration parameter that contains the language
	 * of questions. This is mandatory as x-unspecified will break e.g. OpenNLP.
	 * And for QALD-5 it also just matters because it is multi-lingual.
	 */
	public static final String PARAM_LANGUAGE = ComponentParameters.PARAM_LANGUAGE;
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;

	/**
	 * Name of the XML file.
	 */
	public static final String PARAM_XMLFILE = "XMLFILE";
	@ConfigurationParameter(name = PARAM_XMLFILE, mandatory = true)
	private String XMLFile;

	protected NodeList questionNodes;
	protected int index;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		index = 0;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(XMLFile);
			doc.getDocumentElement().normalize();
			questionNodes = doc.getElementsByTagName("question");
		} catch (Exception io) {
			throw new ResourceInitializationException(io);
		}
	}

	@Override
	public boolean hasNext() throws CollectionException {
		return index < questionNodes.getLength();
	}

	protected void initCas(JCas jcas, Element question) {
		String text = null;
		NodeList strings = question.getElementsByTagName("string");
		for (int j = 0; j < strings.getLength(); j++) {
			Element e = (Element) strings.item(j);
			if (!e.getAttribute("lang").equals(language))
				continue;
			text = e.getTextContent().trim();
			break;
		}

		// read answers
		NodeList answers = question.getElementsByTagName("answer");
		for (int j = 0; j < answers.getLength(); j++) {
			Element e = (Element) answers.item(j);
			GSAnswer gs = new GSAnswer(jcas);
			gs.setText(e.getTextContent());
			gs.addToIndexes(jcas);
		}

		jcas.setDocumentLanguage(language);
		jcas.setDocumentText(text);

		QuestionInfo qInfo = new QuestionInfo(jcas);
		qInfo.setSource("interactive");
		qInfo.setQuestionId(question.getAttribute("id"));
		qInfo.setQuestionType(question.getAttribute("answertype"));
		/* Ignored: aggregation, onlydbo, hybrid, keywords,
		 * pseudoquery, query. */
		qInfo.setQuestionText(text);
		qInfo.setProcBeginTime(System.currentTimeMillis());
		qInfo.addToIndexes(jcas);
	}

	@Override
	public void getNext(CAS aCAS) throws CollectionException {
		Element question = (Element) questionNodes.item(index);
		try {
			JCas jcas = aCAS.getJCas();
			initCas(jcas, question);
		} catch (CASException e) {
			throw new CollectionException(e);
		}
		index++;
	}

	@Override
	public Progress[] getProgress() {
		return new Progress[]{new ProgressImpl(index, -1, Progress.ENTITIES)};
	}

	@Override
	public void close() {
	}
}
