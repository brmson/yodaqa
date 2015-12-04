package cz.brmlab.yodaqa.pipeline.structured;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import cz.brmlab.yodaqa.analysis.rdf.FBPathGloVeScoring;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.rdf.FBPathGloVeScoring;
import cz.brmlab.yodaqa.analysis.rdf.FBPathLogistic;
import cz.brmlab.yodaqa.analysis.rdf.FBPathLogistic.PathScore;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceStructured;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.Question.ClueNE;
import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.model.TyCor.FBOntologyLAT;
import cz.brmlab.yodaqa.provider.rdf.FreebaseOntology;
import cz.brmlab.yodaqa.provider.rdf.PropertyValue;

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
		super("Freebase", AF.OriginFBO_ClueType, AF.OriginFBONoClue);
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

	/* Fetch concept properties from the Freebase ontology dataset,
	 * looking for Freebase topics specifically linked to the enwiki
	 * articles we have linked to the question. */
	protected synchronized List<PropertyValue> getConceptProperties(JCas questionView, Concept concept) {
		List<PropertyValue> properties = new ArrayList<>();
		/* --- Uncomment the next line to disable Freebase lookups. --- */
		// if (true) return properties;

		/* First, get the set of topics covering this concept. */
		/* (This will be just a single-member set aside of a few
		 * exceptional cases.) */
		Set<FreebaseOntology.TitledMid> topics = fbo.queryTopicByPageID(concept.getPageID(), logger);

		/* Get a list of witnesses (besides concepts), i.e. clues of
		 * question that might select the relevant property path by
		 * co-occurence in a composite node. */
		List<String> witnessLabels = new ArrayList<>();
		for (Clue cl : JCasUtil.select(questionView, ClueNE.class)) {
			witnessLabels.add(cl.getLabel());
		}
		List<Concept> concepts = new ArrayList<>(JCasUtil.select(questionView, Concept.class));

		/* Method #1 (Explorative): Get a list of promising-looking property paths
		 * (based on looking at their labels). */
		/* FIXME: getPaths() should be called only once per question! */
		/* FIXME: getPaths() actually already fetches all the
		 * PropertyValues, but we throw them away... */
		FBPathGloVeScoring fbglove = new FBPathGloVeScoring();
		List<PathScore> fbgPaths = fbglove.getPaths(questionView, N_TOP_PATHS);
		if (!fbgPaths.isEmpty())
			for (FreebaseOntology.TitledMid topic : topics)
				properties.addAll(fbo.queryTopicSpecific(topic.title, topic.mid, fbgPaths, concepts, witnessLabels, logger));

		/* Method #2 (A Priori): Get a question-based list of specific properties
		 * to query (predicting them based on the paths we've seen
		 * for similar questions). */
		List<PathScore> lPaths = fbpathLogistic.getPaths(fbpathLogistic.questionFeatures(questionView)).subList(0, N_TOP_PATHS);
		if (!lPaths.isEmpty())
			for (FreebaseOntology.TitledMid topic : topics)
				properties.addAll(fbo.queryTopicSpecific(topic.title, topic.mid, lPaths, concepts, witnessLabels, logger));

		return properties;
	}

	protected AnswerSourceStructured makeAnswerSource(PropertyValue property) {
		return new AnswerSourceStructured(AnswerSourceStructured.TYPE_FREEBASE,
				property.getOrigin(), property.getObjRes(), property.getObject());
	}

	protected void addTypeLAT(JCas jcas, AnswerFV fv, String type) throws AnalysisEngineProcessException {
		fv.setFeature(AF.LATFBOntology, 1.0);
		addTypeLAT(jcas, fv, type, new FBOntologyLAT(jcas));
	}
}
