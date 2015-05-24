package cz.brmlab.yodaqa.pipeline.solrfull;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasMultiplier_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceEnwiki;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
import cz.brmlab.yodaqa.provider.solr.SolrNamedSource;

/**
 * Take a question CAS and multiply it to a CAS instance for each SolrResult
 * featureset of the Search view. */

public class ResultGenerator extends JCasMultiplier_ImplBase {
	final Logger logger = LoggerFactory.getLogger(ResultGenerator.class);

	/** Origin field of ResultInfo. If set, ResultInfo annotations
	 * with different origin are ignored. */
	public static final String PARAM_RESULT_INFO_ORIGIN = "result-info-origin";
	@ConfigurationParameter(name = PARAM_RESULT_INFO_ORIGIN, mandatory = false)
	protected String resultInfoOrigin;

	JCas questionView, searchView;

	/* Prepared list of results to return. */
	FSIterator results;
	ResultInfo nextResult;
	int i;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		try {
			questionView = jcas.getView(CAS.NAME_DEFAULT_SOFA);
			searchView = jcas.getView("Search");
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		results = searchView.getJFSIndexRepository().getAllIndexedFS(ResultInfo.type);
		getNextResult();
		i = 0;
	}

	@Override
	public boolean hasNext() throws AnalysisEngineProcessException {
		return nextResult != null || i == 0;
	}

	@Override
	public AbstractCas next() throws AnalysisEngineProcessException {
		ResultInfo ri = nextResult;
		getNextResult();

		JCas jcas = getEmptyJCas();
		try {
			jcas.createView("Question");
			CasCopier qcopier = new CasCopier(questionView.getCas(), jcas.getView("Question").getCas());
			copyQuestion(qcopier, questionView, jcas.getView("Question"));

			jcas.createView("Result");
			JCas resultView = jcas.getView("Result");
			CasCopier rcopier = new CasCopier(searchView.getCas(), resultView.getCas());
			if (ri != null) {
				fillResult(rcopier, ri, resultView, (nextResult == null));
				/* XXX: Ugh. We clearly need global result ids. */
				QuestionDashboard.getInstance().get(questionView).setSourceState(
						ri.getOrigin() == "cz.brmlab.yodaqa.pipeline.solrfull.fulltext"
							? AnswerSourceEnwiki.ORIGIN_FULL
							: AnswerSourceEnwiki.ORIGIN_TITLE,
						Integer.parseInt(ri.getDocumentId()),
						1);
			} else {
				/* We will just generate a single dummy CAS
				 * to avoid flow breakage. */
				resultView.setDocumentText("");
				resultView.setDocumentLanguage(questionView.getDocumentLanguage());
				ri = new ResultInfo(resultView);
				ri.setDocumentTitle("");
				ri.setOrigin("cz.brmlab.yodaqa.pipeline.ResultGenerator");
				ri.setIsLast(true);
				ri.addToIndexes();
			}
		} catch (Exception e) {
			jcas.release();
			throw new AnalysisEngineProcessException(e);
		}
		i++;
		return jcas;
	}

	protected void getNextResult() {
		nextResult = null;
		while (results.hasNext()) {
			ResultInfo ri = (ResultInfo) results.next();
			if (resultInfoOrigin != null && !ri.getOrigin().equals(resultInfoOrigin))
				continue;
			nextResult = ri;
			return;
		}
	}


	protected void copyQuestion(CasCopier copier, JCas src, JCas jcas) throws Exception {
		copier.copyCasView(src.getCas(), jcas.getCas(), true);
	}

	protected void fillResult(CasCopier copier, ResultInfo ri, JCas jcas, boolean isLast) throws Exception {
		String title = ri.getDocumentTitle();
		logger.info(" ** SearchResultCAS: " + ri.getDocumentId() + " " + (title != null ? title : ""));

		String text;
		try {
			text = SolrNamedSource.get(ri.getSource()).getDocText(ri.getDocumentId());
		} catch (SolrServerException e) {
			e.printStackTrace();
			return;
		}
		// System.err.println("--8<-- " + text + " --8<--");
		jcas.setDocumentText(text);
		jcas.setDocumentLanguage("en"); // XXX

		ResultInfo ri2 = (ResultInfo) copier.copyFs(ri);
		ri2.setIsLast(isLast);
		ri2.addToIndexes();
	}

	@Override
	public int getCasInstancesRequired() {
		// Do not hang on ParallelStep barrier; see MultiThreadASB for explanation
		return 32;
	}
}
