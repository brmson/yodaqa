package cz.brmlab.yodaqa.pipeline.structured;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.rdf.FBPathLogistic;
import cz.brmlab.yodaqa.analysis.rdf.FBPathLogistic.PathScore;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceStructured;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATFBOntology;
import cz.brmlab.yodaqa.model.Question.ClueConcept;
import cz.brmlab.yodaqa.model.TyCor.FBOntologyLAT;
import cz.brmlab.yodaqa.provider.rdf.FreebaseOntology;
import cz.brmlab.yodaqa.provider.rdf.PropertyPath;
import cz.brmlab.yodaqa.provider.rdf.PropertyValue;

/* XXX: The clue-specific features, ugh. */
import cz.brmlab.yodaqa.model.Question.*;
import cz.brmlab.yodaqa.model.CandidateAnswer.*;

/**
 * From the QuestionCAS, generate a bunch of CandidateAnswerCAS
 * instances.  In this case, we generate answers from Freebase
 * ontology relations. */

public class FreebaseOntologyPrimarySearch extends StructuredPrimarySearch {
	/* Number of top non-direct property paths to query.
	 * It's ok to be liberal since most will likely be
	 * non-matching. */
	protected static final int N_TOP_PATHS = 15;

	protected static FBPathLogistic fbpathLogistic = null;

	final FreebaseOntology fbo = new FreebaseOntology();

	public FreebaseOntologyPrimarySearch() {
		super("Freebase", AF_OriginFBONoClue.class);
		logger = LoggerFactory.getLogger(FreebaseOntologyPrimarySearch.class);
	}

	@Override
	public synchronized void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		if (fbpathLogistic == null) {
			fbpathLogistic = new FBPathLogistic();
			fbpathLogistic.initialize();
		}
	}

	protected synchronized List<PropertyValue> getConceptProperties(JCas questionView, ClueConcept concept) {
		List<PropertyValue> properties = new ArrayList<>();
		/* --- Uncomment the next line to disable Freebase lookups. --- */
		// return properties;

		/* Get a list of specific properties to query. */
		List<PathScore> pathScs = fbpathLogistic.getPaths(fbpathLogistic.questionFeatures(questionView)).subList(0, N_TOP_PATHS);
		/* Fetch concept properties from the Freebase ontology dataset,
		 * looking for Freebase topics specifically linked to the enwiki
		 * articles we have found. */
		properties.addAll(fbo.queryPageID(concept.getPageID(), pathScs, logger));

		return properties;
	}

	protected AnswerSourceStructured makeAnswerSource(PropertyValue property) {
		return new AnswerSourceStructured(AnswerSourceStructured.TYPE_FREEBASE,
				property.getOrigin(), property.getObjRes(), property.getObject());
	}

	protected void addTypeLAT(JCas jcas, AnswerFV fv, String type) throws AnalysisEngineProcessException {
		fv.setFeature(AF_LATFBOntology.class, 1.0);
		addTypeLAT(jcas, fv, type, new FBOntologyLAT(jcas));
	}

	protected void clueAnswerFeatures(AnswerFV afv, Clue clue) {
		     if (clue instanceof ClueToken     ) afv.setFeature(AF_OriginFBOClueToken.class, 1.0);
		else if (clue instanceof CluePhrase    ) afv.setFeature(AF_OriginFBOCluePhrase.class, 1.0);
		else if (clue instanceof ClueSV        ) afv.setFeature(AF_OriginFBOClueSV.class, 1.0);
		else if (clue instanceof ClueNE        ) afv.setFeature(AF_OriginFBOClueNE.class, 1.0);
		else if (clue instanceof ClueLAT       ) afv.setFeature(AF_OriginFBOClueLAT.class, 1.0);
		else if (clue instanceof ClueSubject   ) {
			afv.setFeature(AF_OriginFBOClueSubject.class, 1.0);
			     if (clue instanceof ClueSubjectNE) afv.setFeature(AF_OriginFBOClueSubjectNE.class, 1.0);
			else if (clue instanceof ClueSubjectToken) afv.setFeature(AF_OriginFBOClueSubjectToken.class, 1.0);
			else if (clue instanceof ClueSubjectPhrase) afv.setFeature(AF_OriginFBOClueSubjectPhrase.class, 1.0);
			else assert(false);
		} else if (clue instanceof ClueConcept ) {
			afv.setFeature(AF_OriginFBOClueConcept.class, 1.0);
			ClueConcept concept = (ClueConcept) clue;
			if (concept.getBySubject())
				afv.setFeature(AF_OriginFBOClueSubject.class, 1.0);
			if (concept.getByLAT())
				afv.setFeature(AF_OriginFBOClueLAT.class, 1.0);
			if (concept.getByNE())
				afv.setFeature(AF_OriginFBOClueNE.class, 1.0);
		}
		else assert(false);
	}
}
