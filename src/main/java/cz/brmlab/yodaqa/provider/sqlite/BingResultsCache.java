package cz.brmlab.yodaqa.provider.sqlite;

import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceBingSnippet;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.flow.dashboard.SourceIDGenerator;
import cz.brmlab.yodaqa.pipeline.solrfull.BingFullPrimarySearch.BingResult;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honza on 14.7.15.
 */
public class BingResultsCache {
	private final Logger logger = LoggerFactory.getLogger(BingResultsCache.class);
	private SqliteConnector connector;

	public BingResultsCache() {
		connector = new SqliteConnector();
		connector.connect();
	}


	public ArrayList<BingResult> load(String query, JCas questionView, int hitListSize) {
		ArrayList<BingResult> res = new ArrayList<>();
		ResultSet set;
		try {
			set = connector.select("answer", "QUERY = '" + StringEscapeUtils.escapeSql(query) + "' LIMIT " + hitListSize);
			if (!set.isBeforeFirst()) {
				logger.info("No entry for query: " + query + " in cache.");
				return res;
			}
			while (set.next()) {
				BingResult br = new BingResult(set.getString("title"),
						set.getString("description"),
						set.getString("url"),
						set.getInt("rank"));
				AnswerSourceBingSnippet as = new AnswerSourceBingSnippet(br.title, br.url);
				br.sourceID = QuestionDashboard.getInstance().get(questionView).storeAnswerSource(as);
				res.add(br);
			}
			logger.info("Bing results loaded from cache.");
			set.close();
		} catch (SQLException e) {
			logger.error("Could not load answers from cache: " + e.getMessage());
			return res;
		}
		return res;
	}

	public void save(String query, List<BingResult> results) {
		String[] cols = {"query" , "title", "description", "url", "rank"};
		for (BingResult bres: results) {
			Object[] vals = {"'" + StringEscapeUtils.escapeSql(query) + "'",
							 "'" + StringEscapeUtils.escapeSql(bres.title) + "'",
					         "'" + StringEscapeUtils.escapeSql(bres.description) + "'",
					         "'" + StringEscapeUtils.escapeSql(bres.url) + "'", bres.rank};
			connector.insert("answer", cols, vals);
		}
	}
}
