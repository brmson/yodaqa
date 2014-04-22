package cz.brmlab.yodaqa.pipeline;

import java.util.Iterator;

import edu.cmu.lti.oaqa.core.provider.solr.SolrWrapper;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.component.JCasMultiplier_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;

import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;

/**
 * Take a question CAS and search for keywords, yielding a search result
 * CAS instance.
 *
 * We just feed all the clues to a Solr search. */

public class PrimarySearch extends JCasMultiplier_ImplBase {
	/** Number of results to grab and analyze. */
	public static final String PARAM_HITLIST_SIZE = "hitlist-size";
	@ConfigurationParameter(name = PARAM_HITLIST_SIZE, mandatory = false, defaultValue = "6")
	private int hitListSize;

	/** Whether embedded (internal) or standalone (external) Solr
	 * instance is to be used. */
	public static final String PARAM_EMBEDDED = "embedded";
	@ConfigurationParameter(name = PARAM_EMBEDDED, mandatory = false, defaultValue = "true")
	protected boolean embedded;

	/** "Core" is the name of Solr database. In case of embedded,
	 * the pathname to one. */
	public static final String PARAM_CORE = "core";
	@ConfigurationParameter(name = PARAM_CORE, mandatory = false, defaultValue = "data/guten")
	protected String core;

	/** URL to a Solr server if !embedded. */
	public static final String PARAM_SERVER_URL = "server-url";
	@ConfigurationParameter(name = PARAM_SERVER_URL, mandatory = false, defaultValue = "http://localhost:8983/solr/")
	protected String serverUrl;

	protected SolrWrapper Solr;

	JCas src_jcas;
	protected Iterator<SolrDocument> documenti;
	int i;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		try {
			this.Solr = new SolrWrapper(serverUrl, null, embedded, core);
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		src_jcas = jcas;
		i = 0;

		String query = formulateQuery(jcas);
		try {
			SolrDocumentList documents = Solr.runQuery(query, hitListSize);
			documenti = documents.iterator();
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

	@Override
	public boolean hasNext() throws AnalysisEngineProcessException {
		return documenti.hasNext();
	}

	@Override
	public AbstractCas next() throws AnalysisEngineProcessException {
		JCas jcas = getEmptyJCas();
		try {
			jcas.createView("Question");
			CasCopier copier = new CasCopier(src_jcas.getCas(), jcas.getCas());
			copyQuestion(copier, src_jcas, jcas.getView("Question"));

			jcas.createView("Result");
			generateResult(documenti.next(), jcas.getView("Result"), !documenti.hasNext());
		} catch (Exception e) {
			jcas.release();
			throw new AnalysisEngineProcessException(e);
		}
		i++;
		return jcas;
	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		Solr.close();
	}


	protected String formulateQuery(JCas jcas) {
		StringBuffer result = new StringBuffer();
		for (Clue clue : JCasUtil.select(jcas, Clue.class)) {
			String keyterm = clue.getCoveredText();
			result.append("\"" + keyterm + "\" ");
			result.append("titleText:\"" + keyterm + "\" ");
		}
		String query = result.toString();
		System.err.println(" QUERY: " + query);
		return query;
	}

	protected void copyQuestion(CasCopier copier, JCas src, JCas jcas) throws Exception {
		copier.copyCasView(src.getCas(), jcas.getCas(), true);
	}

	protected void generateResult(SolrDocument document, JCas jcas,
			boolean isLast) throws Exception {

		Integer id = (Integer) document.getFieldValue("id");
		String title = (String) document.getFieldValue("titleText");
		System.err.println(" FOUND: " + id + " " + (title != null ? title : ""));
		String text;
		try {
			text = Solr.getDocText(id.toString());
		} catch (SolrServerException e) {
			e.printStackTrace();
			return;
		}
		// System.err.println("--8<-- " + text + " --8<--");
		jcas.setDocumentText(text);
		jcas.setDocumentLanguage("en"); // XXX

		ResultInfo ri = new ResultInfo(jcas);
		ri.setDocumentId(id.toString());
		ri.setDocumentTitle(title);
		ri.setRelevance(((Float) document.getFieldValue("score")).floatValue());
		ri.setIsLast(isLast);
		ri.addToIndexes();
	}
}
