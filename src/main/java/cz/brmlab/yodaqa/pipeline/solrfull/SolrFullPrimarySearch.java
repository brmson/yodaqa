package cz.brmlab.yodaqa.pipeline.solrfull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.answer.AnswerFV;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginConcept;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginPsg;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginPsgFirst;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_ResultLogScore;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.Question.ClueConcept;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
import cz.brmlab.yodaqa.provider.solr.Solr;
import cz.brmlab.yodaqa.provider.solr.SolrNamedSource;
import cz.brmlab.yodaqa.provider.solr.SolrQuerySettings;
import cz.brmlab.yodaqa.provider.solr.SolrTerm;

/**
 * Take a question CAS and search for keywords (or already resolved pageIDs)
 * in the Solr data source.
 *
 * We just feed most of the clues to a Solr search. */

@SofaCapability(
	inputSofas = { "_InitialView" },
	outputSofas = { "Search" }
)

public class SolrFullPrimarySearch extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(SolrFullPrimarySearch.class);

	/** Number of results to grab and analyze. */
	public static final String PARAM_HITLIST_SIZE = "hitlist-size";
	@ConfigurationParameter(name = PARAM_HITLIST_SIZE, mandatory = false, defaultValue = "6")
	protected int hitListSize;

	/** Number and baseline distance of gradually desensitivized
	 * proximity searches. Total of proximity-num optional search
	 * terms are included, covering proximity-base-dist * #of terms
	 * neighborhood. For each proximity term, the coverage is
	 * successively multiplied by proximity-base-factor; initial weight
	 * is sum of individual weights and is successively halved. */
	public static final String PARAM_PROXIMITY_NUM = "proximity-num";
	@ConfigurationParameter(name = PARAM_PROXIMITY_NUM, mandatory = false, defaultValue = "2")
	protected int proximityNum;
	public static final String PARAM_PROXIMITY_BASE_DIST = "proximity-base-dist";
	@ConfigurationParameter(name = PARAM_PROXIMITY_BASE_DIST, mandatory = false, defaultValue = "2")
	protected int proximityBaseDist;
	public static final String PARAM_PROXIMITY_BASE_FACTOR = "proximity-base-factor";
	@ConfigurationParameter(name = PARAM_PROXIMITY_BASE_FACTOR, mandatory = false, defaultValue = "3")
	protected int proximityBaseFactor;

	/** Search full text of articles in addition to their titles. */
	public static final String PARAM_SEARCH_FULL_TEXT = "search-full-text";
	@ConfigurationParameter(name = PARAM_SEARCH_FULL_TEXT, mandatory = false, defaultValue = "true")
	protected boolean searchFullText;

	/** Make all clues required to be present. */
	public static final String PARAM_CLUES_ALL_REQUIRED = "clues-all-required";
	@ConfigurationParameter(name = PARAM_CLUES_ALL_REQUIRED, mandatory = false, defaultValue = "true")
	protected boolean cluesAllRequired;

	/** Origin field of ResultInfo. This can be used to fetch different
	 * ResultInfos in different CAS flow branches. */
	public static final String PARAM_RESULT_INFO_ORIGIN = "result-info-origin";
	@ConfigurationParameter(name = PARAM_RESULT_INFO_ORIGIN, mandatory = false, defaultValue = "cz.brmlab.yodaqa.pipeline.solrfull.SolrFullPrimarySearch")
	protected String resultInfoOrigin;

	protected SolrQuerySettings settings = null;
	protected String srcName;
	protected Solr solr;

	/* This is just information for generating AnswerFeatures. */
	protected boolean onlyFirstPassage;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		/* Eew... well, for now, we just expect that only a single
		 * Solr source has been registered and grab that one,
		 * whatever its name (allows easy enwiki/guten switching). */
		this.srcName = (String) SolrNamedSource.nameSet().toArray()[0];
		this.solr = SolrNamedSource.get(srcName);

		/* XXX: When generating features, we assume that
		 * PARAM_SEARCH_FULL_TEXT always corresponds to
		 * passage PARAM_PASS_SEL_FIRST and title-in-clue
		 * search.  The proper solution if this ever ceases
		 * to be true will be to introduce Passage ansfeatures
		 * and determine AF_OriginFirstPsg in passextract. */
		onlyFirstPassage = ! searchFullText;

		if (searchFullText) {
			this.settings = new SolrQuerySettings(proximityNum, proximityBaseDist, proximityBaseFactor,
					new String[]{"", "titleText"}, cluesAllRequired);
		} else {
			this.settings = new SolrQuerySettings(proximityNum, proximityBaseDist, proximityBaseFactor,
					new String[]{"titleText"}, cluesAllRequired);
		}
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* First, set up the views. */
		try {
			jcas.createView("Search");
		} catch (Exception e) {
			/* That's ok, the Search view might have been
			 * already created by a different PrimarySearch. */
		}
		JCas questionView, searchView;
		try {
			questionView = jcas.getView(CAS.NAME_DEFAULT_SOFA);
			searchView = jcas.getView("Search");
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		/* Make sure we aren't processing any document twice in our
		 * sequence of searches below. */
		Collection<Integer> visitedIDs = new TreeSet<Integer>();

		/* Run a search for concept clues (pageID)
		 * if they weren't included above. */

		SolrDocumentList documents;
		try {
			Collection<ClueConcept> concepts = JCasUtil.select(questionView, ClueConcept.class);
			Collection<Integer> IDs = conceptsToIDs(concepts);
			documents = solr.runIDQuery(IDs, hitListSize /* XXX: should we even limit this? */, logger);
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		for (SolrDocument doc : documents) {
			visitedIDs.add((Integer) doc.getFieldValue("id"));
			generateSolrResult(searchView, doc, true);
		}

		/* Run a search for text clues. */

		try {
			Collection<Clue> clues = JCasUtil.select(questionView, Clue.class);
			Collection<SolrTerm> terms = SolrTerm.cluesToTerms(clues);
			documents = solr.runQuery(terms, hitListSize, settings, logger);
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		for (SolrDocument doc : documents) {
			Integer docID = (Integer) doc.getFieldValue("id");
			if (visitedIDs.contains(docID)) {
				logger.info(" REDUNDANT: " + docID);
				continue;
			}
			visitedIDs.add(docID);
			generateSolrResult(searchView, doc, false);
		}
	}

	protected List<Integer> conceptsToIDs(Collection<ClueConcept> concepts) {
		List<Integer> terms = new ArrayList<Integer>(concepts.size());
		for (ClueConcept concept : concepts)
			terms.add(concept.getPageID());
		return terms;
	}

	protected void generateSolrResult(JCas jcas, SolrDocument document, boolean isConcept)
			throws AnalysisEngineProcessException {
		Integer id = (Integer) document.getFieldValue("id");
		String title = (String) document.getFieldValue("titleText");
		double score = ((Float) document.getFieldValue("score")).floatValue();
		logger.info(" FOUND: " + id + " " + (title != null ? title : "") + " (" + score + ")");

		AnswerFV afv = new AnswerFV();
		afv.setFeature(AF_ResultLogScore.class, Math.log(1 + score));
		afv.setFeature(AF_OriginPsg.class, 1.0);
		if (isConcept)
			afv.setFeature(AF_OriginConcept.class, 1.0);
		if (onlyFirstPassage)
			afv.setFeature(AF_OriginPsgFirst.class, 1.0);

		ResultInfo ri = new ResultInfo(jcas);
		ri.setDocumentId(id.toString());
		ri.setDocumentTitle(title);
		ri.setSource(srcName);
		ri.setRelevance(score);
		ri.setOrigin(resultInfoOrigin);
		ri.setAnsfeatures(afv.toFSArray(jcas));
		ri.addToIndexes();
	}
}
