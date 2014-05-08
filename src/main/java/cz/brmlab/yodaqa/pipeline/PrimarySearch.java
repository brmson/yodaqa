package cz.brmlab.yodaqa.pipeline;

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
import cz.brmlab.yodaqa.provider.Solr;
import cz.brmlab.yodaqa.provider.SolrNamedSource;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;

/**
 * Take a question CAS and search for keywords in the Solr data source.
 *
 * We just feed most of the clues to a Solr search. */

@SofaCapability(
	inputSofas = { "_InitialView" },
	outputSofas = { "Search" }
)

public class PrimarySearch extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(PrimarySearch.class);

	/** Number of results to grab and analyze. */
	public static final String PARAM_HITLIST_SIZE = "hitlist-size";
	@ConfigurationParameter(name = PARAM_HITLIST_SIZE, mandatory = false, defaultValue = "6")
	private int hitListSize;

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

		String query = formulateQuery(questionView);

		SolrDocumentList documents;
		try {
			documents = solr.runQuery(query, hitListSize);
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		for (SolrDocument doc : documents)
			generateSolrResult(searchView, doc);
	}


	protected String formulateQuery(JCas jcas) {
		StringBuffer result = new StringBuffer();
		for (Clue clue : JCasUtil.select(jcas, Clue.class)) {
			// constituent clues are too phrasal for use as search keywords
			if (clue.getBase() instanceof Constituent)
				continue;

			String keyterm = clue.getCoveredText();
			Double weight = clue.getWeight();

			if (result.length() > 0)
				result.append("AND ");
			result.append("(\"" + keyterm + "\" OR titleText:\"" + keyterm + "\")^" + weight + " ");
		}
		String query = result.toString();
		logger.info(" QUERY: " + query);
		return query;
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
