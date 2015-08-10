package cz.brmlab.yodaqa.provider.rdf;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
	/** A container of enwiki article metadata.
	 * This must 1:1 map to label-lookup API. */
	public class Article {
		protected String name;
		protected int pageID;
		protected String matchedLabel;
		protected String canonLabel;
		protected int dist; // edit dist.
		protected int count; //number of matched queries

		public Article(String label, int pageID) {
			this.matchedLabel = label;
			this.canonLabel = label;
			this.pageID = pageID;
		}

		public Article(String label, int pageID, String name, int dist) {
			this(label, pageID);
			this.name = name;
			this.dist = dist;
		}

		public Article(Article baseA, String label, int pageID, String name, int count) {
			this.name = name;
			this.pageID = pageID;
			this.matchedLabel = baseA.matchedLabel;
			this.canonLabel = label;
			this.dist = baseA.dist;
			this.count = count;
		}

		public String getName() { return name; }
		public int getPageID() { return pageID; }
		public String getMatchedLabel() { return matchedLabel; }
		public String getCanonLabel() { return canonLabel; }
		public int getDist() { return dist; }
		public int getCount() { return count; }
	}

	/** Query for a given title, returning a set of articles. */
	public List<Article> query(String title, Logger logger) {
		for (String titleForm : cookedTitles(title)) {
			List<Article> results = new ArrayList<>();
			for (Article a : queryLabelLookup(titleForm, logger)) {
				results.addAll(queryArticle(a, logger));
			}
			if (!results.isEmpty())
				return results;
		}
		return new ArrayList<Article>();
	}

	/** Query for a given Article in full DBpedia, returning a set of
	 * articles (transversing redirects and disambiguations). */
	public List<Article> queryArticle(Article baseA, Logger logger) {
		String name = baseA.getName();
		String rawQueryStr =
			"{\n" +
			   // (A) fetch resources with a given name
			"  BIND(<http://dbpedia.org/resource/" + name + "> AS ?res)\n" +
			"} UNION {\n" +
			   // (B) fetch also resources targetted by redirect
			"  BIND(<http://dbpedia.org/resource/" + name + "> AS ?redir)\n" +
			"  ?redir dbo:wikiPageRedirects ?res .\n" +
			"} UNION {\n" +
			   // (C) fetch also resources targetted by disambiguation
			"  BIND(<http://dbpedia.org/resource/" + name + "> AS ?disamb)\n" +
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

			 // ignore the redundant (A) entries (redirects, disambs)
			"FILTER ( !BOUND(?redirTarget) )\n" +
			"FILTER ( !BOUND(?disambTarget) )\n" +
			 // output only english labels, thankyouverymuch
			"FILTER ( LANG(?label) = 'en' )\n" +
			"";
		//logger.debug("executing sparql query: {}", rawQueryStr);
		List<Literal[]> rawResults = rawQuery(rawQueryStr,
			new String[] { "pageID", "label", "/res" }, 0);

		List<Article> results = new ArrayList<Article>(rawResults.size());
		for (Literal[] rawResult : rawResults) {
			int pageID = rawResult[0].getInt();
			String label = rawResult[1].getString();
			String tgName = rawResult[2].getString().substring("http://dbpedia.org/resource/".length());
			logger.debug("DBpedia {}: [[{}]]", name, label);
			int count = queryCount(rawResult[2].getString());
			results.add(new Article(baseA, label, pageID, tgName, count));
		}

		return results;
	}
	/** Counts the number of matches in rdf triplets (outward and inward) */
	public int queryCount(String name) {
		String queryString = "SELECT (count(*) as ?c) \n" +
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
	 * Query a label-lookup service (fuzzy search) for a concept label.
	 * We use https://github.com/brmson/label-lookup/ as a fuzzy search
	 * that's tolerant to wrong capitalization, omitted interpunction
	 * and typos; we get the enwiki article metadata from here.
	 *
	 * XXX: This method should probably be in a different
	 * provider subpackage altogether... */
	public List<Article> queryLabelLookup(String label, Logger logger) {
		List<Article> results = new LinkedList<>();

		try {
			String encodedName = URLEncoder.encode(label, "UTF-8").replace("+", "%20");
			String requestURL = "http://dbp-labels.ailao.eu:5000/search/" + encodedName;
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
					if (o.getDist() == 0) {
						// Sometimes, we get duplicates
						// like "U.S. Navy" and "U.S. navy".
						if (results.isEmpty() || !results.get(results.size() - 1).getName().equals(o.getName()))
							results.add(o);
					} else if (results.isEmpty()) {
						results.add(o);
					}
					logger.debug("label-lookup({}) returned: d{} ~{} [{}] {} {}", label, o.getDist(), o.getMatchedLabel(), o.getCanonLabel(), o.getName(), o.getPageID());
				}
				jr.endArray();
			jr.endObject();

		} catch (IOException e) {
			// FIXME: Retry mechanism.
			e.printStackTrace();
			return results;
		}
		return results;
	}
}
