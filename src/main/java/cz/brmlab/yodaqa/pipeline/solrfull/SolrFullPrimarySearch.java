package cz.brmlab.yodaqa.pipeline.solrfull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import cz.brmlab.yodaqa.flow.dashboard.SourceIDGenerator;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.component.JCasMultiplier_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.flow.asb.MultiThreadASB;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceEnwiki;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.Question.ClueConcept;
import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
import cz.brmlab.yodaqa.provider.solr.Solr;
import cz.brmlab.yodaqa.provider.solr.SolrNamedSource;
import cz.brmlab.yodaqa.provider.solr.SolrQuerySettings;
import cz.brmlab.yodaqa.provider.solr.SolrTerm;


/**
 * Take a question CAS and search for keywords (or already resolved pageIDs)
 * in the Solr data source.  Each search results gets a new CAS.
 *
 * We just feed most of the clues to a Solr search. */

public class SolrFullPrimarySearch extends JCasMultiplier_ImplBase {
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
	@ConfigurationParameter(name = PARAM_PROXIMITY_NUM, mandatory = false, defaultValue = "3")
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

	protected JCas questionView;

	protected class SolrResult {
		public SolrDocument doc;
		public Concept concept;
		public int rank;
		public int sourceID;

		public SolrResult(SolrDocument doc, Concept concept, int rank) {
			this.doc = doc;
			this.concept = concept;
			this.rank = rank;

			// XXX: Perhaps this shouldn't be in a constructor

			Integer id = (Integer) doc.getFieldValue("id");
			String title = (String) doc.getFieldValue("titleText");
			double score = ((Float) doc.getFieldValue("score")).floatValue();
			logger.info(" FOUND: " + id + " " + (title != null ? title : "") + " (" + score + ")");
			AnswerSourceEnwiki as = new AnswerSourceEnwiki(
					searchFullText ? AnswerSourceEnwiki.ORIGIN_FULL : AnswerSourceEnwiki.ORIGIN_TITLE,
					title, id);
			sourceID = QuestionDashboard.getInstance().get(questionView).storeAnswerSource(as);
		}
	};

	protected List<SolrResult> results;
	protected int i;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		/* Eew... well, for now, we just expect that only a single
		 * Solr source has been registered and grab that one,
		 * whatever its name (allows easy enwiki/guten switching). */
		this.srcName = (String) SolrNamedSource.nameSet().toArray()[0];
		this.solr = SolrNamedSource.get(srcName);

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
			questionView = jcas.getView(CAS.NAME_DEFAULT_SOFA);
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		/* Make sure we aren't processing any document twice in our
		 * sequence of searches below. */
		Collection<Integer> visitedIDs = new TreeSet<Integer>();

		results = new ArrayList<>();
		i = 0;

		/* Run a search for concepts (pageID)
		 * if they weren't included above. */

		Map<Integer, Concept> concepts = new HashMap<>();
		SolrDocumentList documents;
		int i;
		try {
			for (Concept concept : JCasUtil.select(questionView, Concept.class))
				concepts.put(concept.getPageID(), concept);
			Collection<Integer> IDs = concepts.keySet();
			documents = solr.runIDQuery(IDs, hitListSize /* XXX: should we even limit this? */, logger);
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		i = 0;
		for (SolrDocument doc : documents) {
			Integer id = (Integer) doc.getFieldValue("id");
			visitedIDs.add(id);
			/* Record the result. */
			results.add(new SolrResult(doc, concepts.get(id), 1));
		}

		/* Run a search for text clues. */

		try {
			Collection<Clue> clues = JCasUtil.select(questionView, Clue.class);
			Collection<SolrTerm> terms = SolrTerm.cluesToTerms(clues);
			documents = solr.runQuery(terms, hitListSize, settings, logger);
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		i = 0;
		for (SolrDocument doc : documents) {
			Integer docID = (Integer) doc.getFieldValue("id");
			if (visitedIDs.contains(docID)) {
				logger.info(" REDUNDANT: " + docID);
				continue;
			}
			visitedIDs.add(docID);
			/* Record the result. */
			results.add(new SolrResult(doc, null, i+1));
		}
	}

	@Override
	public boolean hasNext() throws AnalysisEngineProcessException {
		return i < results.size() || i == 0;
	}

	@Override
	public AbstractCas next() throws AnalysisEngineProcessException {
		SolrResult result = i < results.size() ? results.get(i) : null;
		i++;

		JCas jcas = getEmptyJCas();
		try {
			jcas.createView("Question");
			CasCopier qcopier = new CasCopier(questionView.getCas(), jcas.getView("Question").getCas());
			copyQuestion(qcopier, questionView, jcas.getView("Question"));

			jcas.createView("Result");
			JCas resultView = jcas.getView("Result");
			if (result != null) {
				boolean isLast = (i == results.size());
				ResultInfo ri = generateSolrResult(questionView, resultView, result, isLast ? i : 0);
				String title = ri.getDocumentTitle();
				logger.info(" ** SearchResultCAS: " + ri.getDocumentId() + " " + (title != null ? title : ""));
				QuestionDashboard.getInstance().get(questionView).setSourceState(ri.getSourceID(), 1);
			} else {
				/* We will just generate a single dummy CAS
				 * to avoid flow breakage. */
				resultView.setDocumentText("");
				resultView.setDocumentLanguage(questionView.getDocumentLanguage());
				ResultInfo ri = new ResultInfo(resultView);
				ri.setDocumentTitle("");
				ri.setOrigin(resultInfoOrigin);
				ri.setIsLast(i);
				ri.addToIndexes();
			}
		} catch (Exception e) {
			jcas.release();
			throw new AnalysisEngineProcessException(e);
		}
		return jcas;
	}

	protected void copyQuestion(CasCopier copier, JCas src, JCas jcas) throws Exception {
		copier.copyCasView(src.getCas(), jcas.getCas(), true);
	}

	protected ResultInfo generateSolrResult(JCas questionView, JCas resultView,
					  SolrResult result,
					  int isLast)
			throws AnalysisEngineProcessException {
		Integer id = (Integer) result.doc.getFieldValue("id");
		String title = (String) result.doc.getFieldValue("titleText");
		double score = ((Float) result.doc.getFieldValue("score")).floatValue();

		String text;
		try {
			text = SolrNamedSource.get(srcName).getDocText(id.toString());
		} catch (SolrServerException e) {
			throw new AnalysisEngineProcessException(e);
		}
		// System.err.println("--8<-- " + text + " --8<--");
		resultView.setDocumentText(text);
		resultView.setDocumentLanguage("en"); // XXX

		AnswerFV afv = new AnswerFV();
		afv.setFeature(AF.ResultRR, 1 / ((float) result.rank));
		afv.setFeature(AF.ResultLogScore, Math.log(1 + score));
		if (result.concept != null) {
			afv.setFeature(AF.OriginConcept, 1.0);
			if (result.concept.getBySubject())
				afv.setFeature(AF.OriginConceptBySubject, 1.0);
			if (result.concept.getByLAT())
				afv.setFeature(AF.OriginConceptByLAT, 1.0);
			if (result.concept.getByNE())
				afv.setFeature(AF.OriginConceptByNE, 1.0);
			afv.setFeature(AF.OriginConcept_feat + AF._clueType_ConceptSourceRR, result.concept.getSourceRr());
			afv.setFeature(AF.OriginConcept_feat + AF._clueType_ConceptLabelRR, result.concept.getLabelRr());
			afv.setFeature(AF.OriginConcept_feat + AF._clueType_ConceptScore, result.concept.getScore());
		}

		ResultInfo ri = new ResultInfo(resultView);
		ri.setDocumentId(id.toString());
		ri.setDocumentTitle(title);
		ri.setSource(srcName);
		ri.setRelevance(score);
		ri.setOrigin(resultInfoOrigin);
		ri.setAnsfeatures(afv.toFSArray(resultView));
		ri.setIsLast(isLast);
		ri.setSourceID(result.sourceID);
		ri.addToIndexes();

		return ri;
	}

	@Override
	public int getCasInstancesRequired() {
		return MultiThreadASB.maxJobs * 2;
	}
}
