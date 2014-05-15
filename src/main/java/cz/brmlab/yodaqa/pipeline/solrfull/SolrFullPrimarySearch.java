package cz.brmlab.yodaqa.pipeline.solrfull;

import java.util.Collection;

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

import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
import cz.brmlab.yodaqa.provider.solr.Solr;
import cz.brmlab.yodaqa.provider.solr.SolrNamedSource;
import cz.brmlab.yodaqa.provider.solr.SolrQuerySettings;
import cz.brmlab.yodaqa.provider.solr.SolrTerm;

/**
 * Take a question CAS and search for keywords in the Solr data source.
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

	protected SolrQuerySettings settings = null;
	protected String srcName;
	protected Solr solr;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		/* Eew... well, for now, we just expect that only a single
		 * Solr source has been registered and grab that one,
		 * whatever its name (allows easy enwiki/guten switching). */
		this.srcName = (String) SolrNamedSource.nameSet().toArray()[0];
		this.solr = SolrNamedSource.get(srcName);

		this.settings = new SolrQuerySettings(proximityNum, proximityBaseDist, proximityBaseFactor,
				new String[]{"", "titleText"});
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

		SolrDocumentList documents;
		try {
			Collection<Clue> clues = JCasUtil.select(questionView, Clue.class);
			Collection<SolrTerm> terms = SolrTerm.cluesToTerms(clues);
			documents = solr.runQuery(terms, hitListSize, settings, logger);
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		for (SolrDocument doc : documents)
			generateSolrResult(searchView, doc);
	}

	protected void generateSolrResult(JCas jcas, SolrDocument document) {
		Integer id = (Integer) document.getFieldValue("id");
		String title = (String) document.getFieldValue("titleText");
		logger.info(" FOUND: " + id + " " + (title != null ? title : ""));

		ResultInfo ri = new ResultInfo(jcas);
		ri.setDocumentId(id.toString());
		ri.setDocumentTitle(title);
		ri.setSource(srcName);
		ri.setRelevance(((Float) document.getFieldValue("score")).floatValue());
		ri.addToIndexes();
	}
}
