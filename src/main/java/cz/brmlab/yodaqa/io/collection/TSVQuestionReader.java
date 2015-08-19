package cz.brmlab.yodaqa.io.collection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

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

import cz.brmlab.yodaqa.flow.dashboard.Question;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;


/**
 * A collection that loads a colleciton of question and answer patterns
 * from TSV file.
 *
 * The TSV file has one question per line, with tab-separated columns:
 * ID TYPE QUESTION ANSWERPCRE */

public class TSVQuestionReader extends CasCollectionReader_ImplBase {
	/**
	 * Name of optional configuration parameter that contains the language
	 * of questions. This is mandatory as x-unspecified will break e.g. OpenNLP.
	 */
	public static final String PARAM_LANGUAGE = ComponentParameters.PARAM_LANGUAGE;
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;

	/**
	 * Name of the TSV file.
	 */
	public static final String PARAM_TSVFILE = "TSVFILE";
	@ConfigurationParameter(name = PARAM_TSVFILE, mandatory = true)
	private String TSVFile;

	BufferedReader br;

	private int index;
	private String input;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		index = -1;
		try {
			br = new BufferedReader(new FileReader(TSVFile));;
		} catch (IOException io) {
			throw new ResourceInitializationException(io);
		}
	}

	protected void acquireInput() {
		index++;
		try {
			input = br.readLine();
		} catch (IOException io) {
			io.printStackTrace();
			input = null;
		}
	}

	@Override
	public boolean hasNext() throws CollectionException {
		if (input == null)
			acquireInput();
		return input != null;
	}

	protected void initCas(JCas jcas, String id, String type, String text, String answer) {
		jcas.setDocumentLanguage(language);

		QuestionInfo qInfo = new QuestionInfo(jcas);
		qInfo.setSource("interactive");
		qInfo.setQuestionId(id);
		qInfo.setQuestionType(type);
		qInfo.setQuestionText(text);
		qInfo.setAnswerPattern(answer);
		qInfo.setProcBeginTime(System.currentTimeMillis());
		qInfo.addToIndexes(jcas);
	}

	@Override
	public void getNext(CAS aCAS) throws CollectionException {
		if (input == null)
			acquireInput();
		String[] fields = input.split("\t");

		Question q = new Question(fields[0] /* id */, fields[2] /* text */);
		QuestionDashboard.getInstance().askQuestion(q);
		QuestionDashboard.getInstance().getQuestionToAnswer();

		try {
			JCas jcas = aCAS.getJCas();
			initCas(jcas, /* id */ fields[0],
				/* type */ fields[1],
				/* text */ fields[2],
				/* answerpcre */ fields[3]);
			jcas.setDocumentText(fields[2]);
		} catch (CASException e) {
			throw new CollectionException(e);
		}
		input = null;
	}

	@Override
	public Progress[] getProgress() {
		return new Progress[]{new ProgressImpl(index, -1, Progress.ENTITIES)};
	}

	@Override
	public void close() {
	}
}
