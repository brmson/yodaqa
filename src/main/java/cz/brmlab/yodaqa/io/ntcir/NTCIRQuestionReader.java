package cz.brmlab.yodaqa.io.ntcir;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import cz.brmlab.yodaqa.flow.dashboard.Question;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * A collection that loads a collection of question and answer patterns
 * from JSON file.
 *
 * The JSON file is an array consisting of json objects containing
 * {qId, qText, answers, author}
 *
 * */
public class NTCIRQuestionReader extends CasCollectionReader_ImplBase {

	class NTCIRQuestion {
		String qId;
		String qText;
		List<String> answers;
		String author;

		public String getqId() {
			return qId;
		}
		public String getqText() {
			return qText;
		}
		public List<String> getAnswers() {
			return answers;
		}
		public String getAuthor() {
			return author;
		}

		public NTCIRQuestion(String qId, String qText, List<String> answers, String author) {
			this.qId = qId;
			this.qText = qText;
			this.answers = answers;
			this.author = author;
		}
	}
	/**
	 * Name of optional configuration parameter that contains the language
	 * of questions. This is mandatory as x-unspecified will break e.g. OpenNLP.
	 */
	public static final String PARAM_LANGUAGE = ComponentParameters.PARAM_LANGUAGE;
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;

	/**
	 * Name of the JSON file.
	 */
	public static final String PARAM_JSONFILE = "JSONFILE";
	@ConfigurationParameter(name = PARAM_JSONFILE, mandatory = true)
	private String xmlFile;

	xmlReader xmlreader;
	private int index;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		index = -1;
		xmlreader = new xmlReader();
		xmlreader.initialize();
	}

	@Override
	public void getNext(CAS aCAS) throws IOException, CollectionException {
		index++;
		NTCIRQuestion j = xmlreader.readNext(this);
		Question q = new Question(j.getqId(), j.getqText());

		QuestionDashboard.getInstance().askQuestion(q);
		QuestionDashboard.getInstance().getQuestionToAnswer();

		try {
			JCas jcas = aCAS.getJCas();
			initCas(jcas, /* id */ j.getqId(),
				/* type */ "factoid",
				/* text */ j.getqText(),
				/* answerpcre */ j.getAnswers());
			jcas.setDocumentText(j.getqText());
		} catch (CASException e) {
			throw new CollectionException(e);
		}
	}

	protected void initCas(JCas jcas, String id, String type, String text, List<String> answers) {
		jcas.setDocumentLanguage(language);
		Iterator<String> answerIterator = answers.iterator();
		String answerPattern = "";
		while(answerIterator.hasNext()) {
			answerPattern += answerIterator.next();
			if (answerIterator.hasNext()) {
				answerPattern += "|";
			}
		}
		QuestionInfo qInfo = new QuestionInfo(jcas);
		qInfo.setSource("interactive");
		qInfo.setQuestionId(id);
		qInfo.setQuestionType(type);
		qInfo.setQuestionText(text);
		qInfo.setAnswerPattern(answerPattern);
		qInfo.setProcBeginTime(System.currentTimeMillis());
		qInfo.addToIndexes(jcas);
	}

	@Override
	public boolean hasNext() throws IOException, CollectionException {
		boolean next = xmlreader.hasNext();
//		if (next == false) {
//			xmlreader.endArray();
//		}
		return next;
	}

	@Override
	public Progress[] getProgress() {
		return new Progress[]{new ProgressImpl(index, -1, Progress.ENTITIES)};
	}

}
