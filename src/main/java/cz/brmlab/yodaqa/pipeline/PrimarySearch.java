package cz.brmlab.yodaqa.pipeline;

import java.util.Iterator;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
import cz.brmlab.yodaqa.provider.Solr;
import cz.brmlab.yodaqa.provider.SolrNamedSource;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;

/**
 * Take a question CAS and search for keywords, yielding a search result
 * CAS instance.
 *
 * We just feed all the clues to a Solr search. */

public class PrimarySearch extends JCasMultiplier_ImplBase {
	final Logger logger = LoggerFactory.getLogger(PrimarySearch.class);

	/** Number of results to grab and analyze. */
	public static final String PARAM_HITLIST_SIZE = "hitlist-size";
	@ConfigurationParameter(name = PARAM_HITLIST_SIZE, mandatory = false, defaultValue = "6")
	private int hitListSize;

	protected Solr solr;

	JCas src_jcas;
	protected Iterator<SolrDocument> documenti;
	int i;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		/* Eew... well, for now, we just expect that only a single
		 * Solr source has been registered and grab that one,
		 * whatever its name (allows easy enwiki/guten switching). */
		this.solr = SolrNamedSource.get((String) SolrNamedSource.nameSet().toArray()[0]);
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		src_jcas = jcas;
		i = 0;

		String query = formulateQuery(jcas);
		try {
			SolrDocumentList documents = solr.runQuery(query, hitListSize);
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

	protected void copyQuestion(CasCopier copier, JCas src, JCas jcas) throws Exception {
		copier.copyCasView(src.getCas(), jcas.getCas(), true);
	}

	protected void generateResult(SolrDocument document, JCas jcas,
			boolean isLast) throws Exception {

		Integer id = (Integer) document.getFieldValue("id");
		String title = (String) document.getFieldValue("titleText");
		logger.info(" FOUND: " + id + " " + (title != null ? title : ""));
		String text;
		try {
			text = solr.getDocText(id.toString());
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
