package cz.brmlab.yodaqa.io.web;

import cz.brmlab.yodaqa.model.Question.Concept;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.flow.dashboard.Question;
import cz.brmlab.yodaqa.flow.dashboard.QuestionConcept;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;


/**
 * A collection that reads the question from WebInterface. */

public class WebQuestionReader extends CasCollectionReader_ImplBase {
	final Logger logger = LoggerFactory.getLogger(WebQuestionReader.class);

	/**
	 * Name of optional configuration parameter that contains the language
	 * of questions. This is mandatory as x-unspecified will break e.g. OpenNLP.
	 */
	public static final String PARAM_LANGUAGE = ComponentParameters.PARAM_LANGUAGE;
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	protected String language;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	@Override
	public boolean hasNext() throws CollectionException {
		return true;
	}

	protected void initCas(JCas jcas, Question q) {
		jcas.setDocumentLanguage(language);

		QuestionInfo qInfo = new QuestionInfo(jcas);
		qInfo.setSource("web");
		qInfo.setQuestionId(q.getId());
		qInfo.addToIndexes(jcas);
		qInfo.setOnlyArtificialConcepts(q.getHasOnlyArtificialConcept());
		logger.debug("INPUT [{}]: {}", q.getId(), q.getText());

		if (q.getArtificialConcepts() != null) {
			for (QuestionConcept c : q.getArtificialConcepts()) {
				Concept aConcept = new Concept(jcas);
				aConcept.setPageID(c.getPageId());
				aConcept.setFullLabel(c.getTitle());
				aConcept.setCookedLabel(aConcept.getFullLabel());
				aConcept.addToIndexes();
				logger.debug("CONCEPT: {}/{}", c.getPageId(), c.getTitle());
			}
		}
	}

	@Override
	public void getNext(CAS aCAS) throws CollectionException {
		Question q = QuestionDashboard.getInstance().getQuestionToAnswer();
		try {
			JCas jcas = aCAS.getJCas();
			initCas(jcas, q);
			jcas.setDocumentText(q.getText());
		} catch (CASException e) {
			throw new CollectionException(e);
		}
	}

	@Override
	public Progress[] getProgress() {
		return new Progress[]{new ProgressImpl(0, -1, Progress.ENTITIES)};
	}

	@Override
	public void close() {
	}
}
