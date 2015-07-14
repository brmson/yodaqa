package cz.brmlab.yodaqa.io.sqlite;

import cz.brmlab.yodaqa.pipeline.solrfull.BingFullPrimarySearch;
import cz.brmlab.yodaqa.pipeline.solrfull.BingFullPrimarySearch.BingResult;
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


	public ArrayList<BingResult> load(String query) {
		ArrayList<BingResult> res = new ArrayList<>();
		ResultSet set;
		try {
			set = connector.select("answer", "QUERY = '" + query + "'");
			if (set.isClosed()) {
				logger.info("No entry for query: " + query + " in cache.");
				return res;
			}
			while (set.next()) {
				res.add(new BingResult(set.getString("title"),
									   set.getString("description"),
						  			   set.getInt("rank")));
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
		String[] cols = {"query" , "title", "description", "rank"};
		for (BingResult bres: results) {
			Object[] vals = {"'" + query + "'", "'" + bres.title + "'", "'" + bres.description + "'", bres.rank};
			connector.insert("answer", cols, vals);
		}
	}
}
