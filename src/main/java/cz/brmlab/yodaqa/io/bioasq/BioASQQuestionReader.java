package cz.brmlab.yodaqa.io.bioasq;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import cz.brmlab.yodaqa.flow.dashboard.Question;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.model.Question.GSAnswer;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.Question.Snippet;


/**
 * A reader that loads a colleciton of questions, extra gathered metadata,
 * and possibly answers, from BioASQ-provided JSON file in their format. */

public class BioASQQuestionReader extends CasCollectionReader_ImplBase {
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
	private String JSONFile;

	LinkedList<JSONObject> questions = new LinkedList<>();
	private int index;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		index = 0;

		JSONParser parser = new JSONParser();
		JSONObject data;
		try {
			data = (JSONObject) parser.parse(new FileReader(JSONFile));
		} catch (Exception io) {
			throw new ResourceInitializationException(io);
		}
		for (Object o : (JSONArray) data.get("questions")) {
			questions.add((JSONObject) o);
		}
	}

	@Override
	public boolean hasNext() throws CollectionException {
		return !questions.isEmpty();
	}

	protected GSAnswer addGSAnswer(JCas jcas, List<String> answers) {
		GSAnswer gsa = new GSAnswer(jcas);

		// XXX: are we in a stone age here?!?
		int i = 0;
		StringArray sa = new StringArray(jcas, answers.size());
		for (String a : answers) {
			sa.set(i, a);
			i++;
		}
		gsa.setTexts(sa);

		gsa.addToIndexes(jcas);
		return gsa;
	}

	protected GSAnswer addGSAnswerFromJSON(JCas jcas, JSONArray arr) {
		List<String> answers = new ArrayList<>();
		for (Object a : arr) {
			answers.add((String) a);
		}
		return addGSAnswer(jcas, answers);
	}

	protected void initCas(JCas jcas, JSONObject question) {
		jcas.setDocumentLanguage(language);

		QuestionInfo qInfo = new QuestionInfo(jcas);
		qInfo.setSource("bioasq");
		qInfo.setQuestionId((String) question.get("id"));
		qInfo.setQuestionType((String) question.get("type"));
		qInfo.setQuestionText((String) question.get("body"));

		qInfo.setProcBeginTime(System.currentTimeMillis());
		qInfo.addToIndexes(jcas);

		/* Store snippets. */
		if (question.containsKey("snippets")) {
			for (Object s : (JSONArray) question.get("snippets")) {
				JSONObject sData = (JSONObject) s;
				Snippet snippet = new Snippet(jcas);
				snippet.setText((String) sData.get("text"));
				snippet.setDocument((String) sData.get("document"));
				snippet.addToIndexes();
			}
		} else {
			// we require them for now
			assert(false);
		}
		// TODO: Also store and use concepts, documents, triples.

		/* Load exact_answer gold standard. */
		if (question.containsKey("exact_answer")) {
			if (qInfo.getQuestionType().equals("list")) {
				for (Object a : (JSONArray) question.get("exact_answer")) {
					addGSAnswerFromJSON(jcas, (JSONArray) a);
				}
			} else if (qInfo.getQuestionType().equals("factoid")) {
				addGSAnswerFromJSON(jcas, (JSONArray) question.get("exact_answer"));
			} else if (qInfo.getQuestionType().equals("yesno")) {
				List<String> answers = new ArrayList<>();
				answers.add((String) question.get("exact_answer"));
				addGSAnswer(jcas, answers);
			} else {
				assert(false);
			}
		}
	}

	@Override
	public void getNext(CAS aCAS) throws CollectionException {
		JSONObject question = questions.remove();
		/* always: body, type, id
		 * alwaysB: documents, triples, concepts, snippets
		 * optional: ideal_answer, exact_answer */

		JCas jcas;
		try {
			jcas = aCAS.getJCas();
			initCas(jcas, question);
			jcas.setDocumentText((String) question.get("body"));
		} catch (CASException e) {
			throw new CollectionException(e);
		}

		Question q = new Question(Integer.toString(index), jcas.getDocumentText());
		QuestionDashboard.getInstance().askQuestion(q);
		QuestionDashboard.getInstance().getQuestionToAnswer();

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
