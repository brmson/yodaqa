package cz.brmlab.yodaqa.provider.rdf;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.stream.JsonReader;
import com.google.gson.Gson;
import com.hp.hpl.jena.rdf.model.Literal;

import org.slf4j.Logger;

/** A wrapper around DBpedia "Titles" dataset that maps titles to
 * Wikipedia articles. The main point of using this is to find out
 * whether an article with the given title (termed also "label")
 * exists.
 *
 * Articles are represented as (label, pageId), where label is the
 * article title. The label is included as we pass through redirects
 * and disambiguation pages. */

public class DBpediaTitles extends DBpediaLookup {
	protected static final String fuzzyLookupUrl = "http://dbp-labels.ailao.eu:5000";
	protected static final String crossWikiLookupUrl = "http://dbp-labels.ailao.eu:5001";

	/** A container of enwiki article metadata.
	 * This must 1:1 map to label-lookup API. */
	public class Article {
		protected String name;
		protected int pageID;
		protected String matchedLabel;
		protected String canonLabel;
		protected double dist; // edit dist.
		protected double pop; // relevance/prominence of the concept (universally or wrt. the question)
		protected double prob;
		protected String description; // long-ish text
		protected boolean getByFuzzyLookup = false;
		protected boolean getByCWLookup = false;

		public Article(String label, int pageID) {
			this.matchedLabel = label;
			this.canonLabel = label;
			this.pageID = pageID;
		}

		public Article(String label, int pageID, String name, double dist) {
			this(label, pageID);
			this.name = name;
			this.dist = dist;
		}

		public Article(String label, int pageID, String name, double dist, double prob) {
			this(label, pageID);
			this.name = name;
			this.dist = dist;
			this.prob = prob;
		}

		public Article(Article baseA, String label, int pageID, String name, double pop, double prob, String descr) {
			this.name = name;
			this.pageID = pageID;
			this.matchedLabel = baseA.matchedLabel;
			this.canonLabel = label;
			this.dist = baseA.dist;
			this.pop = pop;
			this.prob = prob;
			this.description = descr;
			this.getByCWLookup = baseA.getByCWLookup;
			this.getByFuzzyLookup = baseA.getByFuzzyLookup;
		}

		public String getName() { return name; }
		public int getPageID() { return pageID; }
		public String getMatchedLabel() { return matchedLabel; }
		public String getCanonLabel() { return canonLabel; }
		public double getDist() { return dist; }
		public double getPop() { return pop; }
		public double getProb() { return prob; }
		public String getDescription() { return description; }
		public boolean isByFuzzyLookup() { return getByFuzzyLookup; }
		public boolean isByCWLookup() { return getByCWLookup; }
	}

	/** Query for a given title, returning a set of articles. */
	public List<Article> query(String title, Logger logger) {
		for (String titleForm : cookedTitles(title)) {
			List<Article> fuzzyLookupEntities;
			List<Article> crossWikiEntities;
			while (true) {
				try {
					fuzzyLookupEntities = queryFuzzyLookup(titleForm, logger);
					crossWikiEntities = queryCrossWikiLookup(titleForm, logger);
					break; // Success!
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("*** " + fuzzyLookupUrl + " or " + crossWikiLookupUrl + " label lookup query (temporarily?) failed, retrying in a moment...");
					try {
						TimeUnit.SECONDS.sleep(10);
					} catch (InterruptedException e2) { // oof...
						e2.printStackTrace();
					}
				}
			}
			List<Article> entities = mergeResults(fuzzyLookupEntities, crossWikiEntities, logger);
			List<Article> results = new ArrayList<>();
			for (Article a : entities) {
				results.addAll(queryArticle(a, logger));
			}
			results = deduplicateResults(results);
			if (!results.isEmpty())
				return results;
		}
		return new ArrayList<Article>();
	}

	/** Query for a given Article in full DBpedia, returning a set of
	 * articles (transversing redirects and disambiguations). */
	public List<Article> queryArticle(Article baseA, Logger logger) {
		String name = baseA.getName();
		if (name.contains("\"") || name.startsWith("-")) // generates syntax errors
			return new ArrayList<Article>();
		// This escapes unicode characters properly
		String resURI;
		try {
			resURI = new URI("http://dbpedia.org/resource/" + name).toASCIIString();
		} catch (URISyntaxException e) {
			System.err.println("Bad name: " + name);
			e.printStackTrace();
			return new ArrayList<Article>();
		}

		double prob = baseA.getProb();
		String rawQueryStr =
			"{\n" +
			   // (A) fetch resources with a given name
			"  BIND(<" + resURI + "> AS ?res)\n" +
			"} UNION {\n" +
			   // (B) fetch also resources targetted by redirect
			"  BIND(<" + resURI + "> AS ?redir)\n" +
			"  ?redir dbo:wikiPageRedirects ?res .\n" +
			"} UNION {\n" +
			   // (C) fetch also resources targetted by disambiguation
			"  BIND(<" + resURI + "> AS ?disamb)\n" +
			"  ?disamb dbo:wikiPageDisambiguates ?res .\n" +
			"} UNION {\n" +
			   // (B) + (C) (e.g. Aladdin_(film) -> Aladdin_(disambiguation)
			"  BIND(<" + resURI + "> AS ?redir)\n" +
			"  ?redir dbo:wikiPageRedirects ?disamb .\n" +
			"  ?disamb dbo:wikiPageDisambiguates ?res .\n" +
			"}\n" +
			 // for (B) and (C), we are also getting a redundant (A) entry;
			 // identify the redundant (A) entry by filling
			 // ?redirTarget in that case
			"OPTIONAL { ?res dbo:wikiPageRedirects ?redirTarget . }\n" +
			"OPTIONAL { ?res dbo:wikiPageDisambiguates ?disambTarget . }\n" +
			 // set the output variables
			"?res dbo:wikiPageID ?pageID .\n" +
			"?res rdfs:label ?label .\n" +
			"OPTIONAL { ?res rdfs:comment ?description . FILTER ( LANG(?description) = 'en' ) }\n" +

			 // ignore the redundant (A) entries (redirects, disambs)
			"FILTER ( !BOUND(?redirTarget) )\n" +
			"FILTER ( !BOUND(?disambTarget) )\n" +
			 // output only english labels, thankyouverymuch
			"FILTER ( LANG(?label) = 'en' )\n" +
			"";
		//logger.debug("executing sparql query: {}", rawQueryStr);
		List<Literal[]> rawResults = rawQuery(rawQueryStr,
			new String[] { "pageID", "label", "/res", "description" }, 0);

		List<Article> results = new ArrayList<Article>(rawResults.size());
		for (Literal[] rawResult : rawResults) {
			int pageID = rawResult[0].getInt();
			String label = rawResult[1].getString();
			String tgRes = rawResult[2].getString();
			String descr = rawResult[3] != null ? rawResult[3].getString() : null;

			/* http://dbpedia.org/resource/-al is valid IRI, but
			 * Jena bastardizes it automatically to :-al which is
			 * not valid and must be written as :\-al.
			 * XXX: Unfortunately, Jena also eats the backslashes
			 * it sees during that process, so we just give up and
			 * throw away these rare cases. */
			if (tgRes.contains("/-")) {
				logger.warn("Giving up on DBpedia {}", tgRes);
				continue;
			}

			String tgName = tgRes.substring("http://dbpedia.org/resource/".length());

			/* We approximate the concept popularity simply by how
			 * many relations it partakes in.  We take a log
			 * though to keep it at least roughly normalized. */
			double pop = Math.log(queryCount(tgRes));

			String descrText = "(null)";
			if (descr != null) {
				try {
					descrText = "..." + descr.substring(10, 30) + "...";
				} catch (StringIndexOutOfBoundsException e) {
					descrText = descr;
				}
			}
			logger.debug("DBpedia {}: [[{}]] (id={}; pop={}; descr=<<{}>>)",
					name, label, pageID, pop, descrText);
			results.add(new Article(baseA, label, pageID, tgName, pop, prob, descr));
		}

		return results;
	}

	/** Counts the number of matches in rdf triplets (outward and inward). */
	public int queryCount(String name) {
		String queryString = "SELECT (COUNT(*) AS ?c) \n" +
				"{" +
					"{" +
				    "?a ?b <"+ name +">"+
				    "} UNION {" +
				    "<"+name+"> ?a ?b" +
				    "}" +
				"}";
		List<Literal[]> rawResults = rawQuery(queryString,
				new String[] { "c" }, 0);
		return rawResults.get(0)[0].getInt();
	}

	/**
	 * Query the fuzzy search label-lookup service for a concept label.
	 * We use https://github.com/brmson/label-lookup/ (main label-lookup
	 * script) as a fuzzy search that's tolerant to wrong capitalization,
	 * omitted interpunction and typos.
	 *
	 * XXX: This method should probably be in a different
	 * provider subpackage altogether... */
	public List<Article> queryFuzzyLookup(String label, Logger logger) throws IOException {
		List<Article> results = new LinkedList<>();
		String capitalisedLabel = super.capitalizeTitle(label);
		String encodedName = URLEncoder.encode(capitalisedLabel, "UTF-8").replace("+", "%20");
		String requestURL = fuzzyLookupUrl + "/search/" + encodedName + "?ver=1";

		URL request = new URL(requestURL);
		URLConnection connection = request.openConnection();
		Gson gson = new Gson();

		JsonReader jr = new JsonReader(new InputStreamReader(connection.getInputStream()));
		jr.beginObject();
			jr.nextName(); //results :
			jr.beginArray();
			while (jr.hasNext()) {
				Article o = gson.fromJson(jr, Article.class);
				// Record all exact-matching entities,
				// or the single nearest fuzzy-matched
				// one.
				o.getByFuzzyLookup = true;
				if (o.getDist() == 0) {
					// Sometimes, we get duplicates
					// like "U.S. Navy" and "U.S. navy".
					if (results.isEmpty() || !results.get(results.size() - 1).getName().equals(o.getName()))
						results.add(o);
				} else if (results.isEmpty()) {
					results.add(o);
				}
				logger.debug("fuzzy-lookup({}) returned: d{} ~{} [{}] {} {}", label, o.getDist(), o.getMatchedLabel(), o.getCanonLabel(), o.getName(), o.getPageID());
			}
			jr.endArray();
		jr.endObject();

		return results;
	}

	/**
	 * Query the CrossWiki label-lookup service for a concept label.
	 * We use https://github.com/brmson/label-lookup/ (-sqlite script)
	 * which looks up the label in a large database of labels including
	 * common mispellings etc.  P(entity | label) is also returned.
	 *
	 * Take only the first result with the highest probability;
	 *
	 * XXX: same as above, this method should be moved somewhere else */
	public List<Article> queryCrossWikiLookup(String label, Logger logger) throws IOException {
		List<Article> results = new LinkedList<>();
		String capitalisedLabel = super.capitalizeTitle(label);
		String encodedName = URLEncoder.encode(capitalisedLabel, "UTF-8").replace("+", "%20");
		String requestURL = crossWikiLookupUrl + "/search/" + encodedName + "?ver=1";

		URL request = new URL(requestURL);
		URLConnection connection = request.openConnection();
		Gson gson = new Gson();

		JsonReader jr = new JsonReader(new InputStreamReader(connection.getInputStream()));
		jr.beginObject();
		jr.nextName(); //results :
		jr.beginArray();
		while (jr.hasNext()) {
			Article o = gson.fromJson(jr, Article.class);
			o.getByCWLookup = true;
			results.add(o);
			logger.debug("sqlite-lookup({}) returned: p{} ~{} [{}] {} {}", label, o.getProb(), o.getMatchedLabel(), o.getCanonLabel(), o.getName(), o.getPageID());
		}
		jr.endArray();
		jr.endObject();

		return results;
	}

	/**
	 * Merges the result for fuzzy and CrossWiki searches.
	 * Priority is given to the results from label lookup, we add
	 * the probability if the canon label matches.
	 * May modify Article objects from the source lists,
	 * as well as lists themselves!
	 */
	public List<Article> mergeResults(List<Article> fuzzyResult, List<Article> cwResult, Logger logger) {
		if (fuzzyResult.isEmpty())
			return cwResult;
		if (cwResult.isEmpty())
			return fuzzyResult;

		Map<String, Article> cwArticles = new HashMap<>();
		for (Article a : cwResult) {
			cwArticles.put(a.getName(), a);
		}
		for (Article a : fuzzyResult) {
			Article cwA = cwArticles.get(a.getName());
			if (cwA == null) {
				logger.debug("merge: fuzzyResult {}", a.getName());
				continue;
			} else {
				logger.debug("merge: fuzzyResult+cwResult {}", a.getName());
			}
			cwArticles.remove(a.getName());
			a.prob = cwA.getProb();
			a.getByCWLookup = true;
		}
		for (Article a : cwArticles.values()) {
			logger.debug("merge: cwResult {}", a.getName());
			fuzzyResult.add(a);
		}
		return fuzzyResult;
	}

	/** Eliminate duplicate Article objects from results.
	 * This may happen e.g. by fuzzy-lookup linking to [[Top Gun]]
	 * and sqlite-lookup linking to [[Top Gun (film)]], both of which
	 * are the same pageID; so use pageID as the deduplication key.
	 *
	 * This not only creates query overhead, but in a parallel setting
	 * due to slightly non-deterministic merging (XXX figure out why?)
	 * it may include a quite wide variance in benchmark results,
	 * apparently! */
	protected List<Article> deduplicateResults(List<Article> results) {
		List<Article> newResults = new ArrayList<>();
		Map<Integer, Article> byPageIDs = new HashMap<>();
		for (Article r : results) {
			Article r2 = byPageIDs.get(r.getPageID());
			if (r2 != null) {
				// already have it, merge
				if (r.prob > r2.prob)
					r2.prob = r.prob;
				// pop, dist is guaranteed to be the same
				r2.getByCWLookup = r2.getByCWLookup || r.getByCWLookup;
				r2.getByFuzzyLookup = r2.getByFuzzyLookup || r.getByFuzzyLookup;
				continue;
			}
			newResults.add(r);
			byPageIDs.put(r.getPageID(), r);
		}
		return newResults;
	}
}
