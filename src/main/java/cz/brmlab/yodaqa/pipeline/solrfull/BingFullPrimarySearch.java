package cz.brmlab.yodaqa.pipeline.solrfull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;

import com.google.gson.GsonBuilder;

import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceBingSnippet;
import cz.brmlab.yodaqa.provider.sqlite.BingResultsCache;

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

import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.flow.asb.MultiThreadASB;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;

import org.apache.commons.codec.binary.Base64;


/**
 * Take a question CAS and search for keywords in the Bing web search.
 * Each search results gets a new CAS.
 *
 * Details about the Bing search, an API key you need to get in order
 * for this to get used, etc., are in data/bing/README.md.
 *
 * Bing search is disabled by default. You need to set system property
 * cz.brmlab.yodaqa.use_bing=yes to enabled it.
 *
 * XXX: The containing package shouldn't be called "solrfull" as this
 * search has nothing to do with Solr. */

public class BingFullPrimarySearch extends JCasMultiplier_ImplBase {
	final Logger logger = LoggerFactory.getLogger(BingFullPrimarySearch.class);

	/** Number of results to grab and analyze. */
	public static final String PARAM_HITLIST_SIZE = "hitlist-size";
	@ConfigurationParameter(name = PARAM_HITLIST_SIZE, mandatory = false, defaultValue = "6")
	protected int hitListSize;


	/** Origin field of ResultInfo. This can be used to fetch different
	 * ResultInfos in different CAS flow branches. */
	public static final String PARAM_RESULT_INFO_ORIGIN = "result-info-origin";
	@ConfigurationParameter(name = PARAM_RESULT_INFO_ORIGIN, mandatory = false, defaultValue = "cz.brmlab.yodaqa.pipeline.solrfull.BingFullPrimarySearch")
	protected String resultInfoOrigin;

	protected JCas questionView;


	protected List<BingResult> results;
	protected int i;
	private String apikey;

	private boolean skip;
	private BingResultsCache cache;

	public static class BingResult {
		public String title;
		public String description;
		public String url;
		public int rank;
		public int sourceID;

		public BingResult(String title, String description, String url, int rank) {
			this.title = title;
			this.description = description;
			this.url = url;
			this.rank = rank;


		}
	}

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		skip = false;
		String useBing = System.getProperty("cz.brmlab.yodaqa.use_bing");
		if (useBing != null && useBing.equals("yes")) {
			cache = new BingResultsCache();
			Properties prop = new Properties();
			try {
				prop.load(new FileInputStream("conf/bingapi.properties"));
				apikey = (String) prop.get("apikey");
				if (apikey == null) throw new NullPointerException("Api key is null");
			} catch (IOException | NullPointerException e) {
				logger.info("No api key for bing api! " + e.getMessage());
				skip = true;
			}
		} else {
			logger.info("Bing search is disabled!");
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

		results = new ArrayList<>();
		i = 0;
		/* Run a search for text clues. */

		String useBing = System.getProperty("cz.brmlab.yodaqa.use_bing");
		if (useBing != null && useBing.equals("yes")) {
			try {
				Collection<Clue> clues = JCasUtil.select(questionView, Clue.class);
				results = bingSearch(clues, hitListSize);
			} catch (Exception e) {
				throw new AnalysisEngineProcessException(e);
			}
		}

	}

	private List<BingResult> bingSearch(Collection<Clue> clues, int hitListSize) {
		ArrayList<BingResult> res;
		StringBuilder sb = new StringBuilder();
		int numOfResults = 30;  /* XXX: Irrespective of hitListSize, for the benefit of the cache */
		for (Clue c: clues) {
			sb.append(c.getLabel()).append(" ");
		}
		if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
		logger.info("QUERY: " + sb.toString());

		res = cache.load(sb.toString(), questionView, hitListSize);
		if ((res != null && res.size() > 0) || skip)
			return res;

		String query;
		String bingUrl = "";
		try {
			query = URLEncoder.encode(sb.toString(), Charset.defaultCharset().name());
			bingUrl = "https://api.datamarket.azure.com/Bing/Search/Web?"
					+ "Query=%27" + query + "%27&$top=" + numOfResults + "&$format=JSON&Market=%27en-US%27";

			final String accountKeyEnc = Base64.encodeBase64String((apikey + ":" + apikey).getBytes());

			final URL url = new URL(bingUrl);
			final URLConnection connection = url.openConnection();
			connection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);

			GsonBuilder builder = new GsonBuilder();
			Map<String, Map> json = builder.create()
					.fromJson(new InputStreamReader(connection.getInputStream()), Map.class);
			Map<String, ArrayList> d = json.get("d");
			ArrayList<Map> results = d.get("results");
			int rank = 1;
			for (Map<String, String> m : results) {
				BingResult br = new BingResult(m.get("Title"), m.get("Description"), m.get("Url"), rank);
				AnswerSourceBingSnippet as = new AnswerSourceBingSnippet(br.title, br.url);
				br.sourceID = QuestionDashboard.getInstance().get(questionView).storeAnswerSource(as);
				res.add(br);
				rank++;
			}
			cache.save(sb.toString(), res);
		} catch (IOException e) {
			logger.error("Unable to obtain bing results: " + e.getMessage());
			logger.info("BINGURL " + bingUrl);
			return res;
		}
		if (res.size() == 0)
			logger.info("No bing results.");
		else if (res.size() > hitListSize)
			res.subList(hitListSize, res.size()).clear();
		return res;
	}

	@Override
	public boolean hasNext() throws AnalysisEngineProcessException {
		return i < results.size() || i == 0;
	}

	@Override
	public AbstractCas next() throws AnalysisEngineProcessException {
		BingResult result = i < results.size() ? results.get(i) : null;
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
				ResultInfo ri = generateBingResult(questionView, resultView, result, isLast ? i : 0);
				String title = ri.getDocumentTitle();
				logger.info(" ** SearchResultCAS: " + ri.getDocumentId() + " " + (title != null ? title : ""));
				/* XXX: Ugh. We clearly need global result ids. */
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

	protected ResultInfo generateBingResult(JCas questionView, JCas resultView,
											BingResult result,
											int isLast)
			throws AnalysisEngineProcessException {

		// System.err.println("--8<-- " + text + " --8<--");
		resultView.setDocumentText(result.description);
		resultView.setDocumentLanguage("en"); // XXX

		AnswerFV afv = new AnswerFV();
		afv.setFeature(AF.ResultRR, 1 / ((float) result.rank));
		afv.setFeature(AF.OriginBingSnippet, 1.0);

		ResultInfo ri = new ResultInfo(resultView);
		ri.setDocumentId("1"); //FIXME dummy id
		ri.setDocumentTitle(result.title);
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
