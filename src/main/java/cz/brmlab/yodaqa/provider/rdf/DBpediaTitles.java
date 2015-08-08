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
	public class Article {
		protected int pageID;
		protected String label;

		public Article(int pageID_, String label_) {
			pageID = pageID_;
			label = label_;
		}

		/** @return the pageID */
		public int getPageID() {
			return pageID;
		}

		/** @return the label */
		public String getLabel() {
			return label;
		}
	}

	public class CustomArticle extends Article {
		protected String name;
		protected int distance;
		public CustomArticle(int distance, String label, String name, int pageID) {
			super(pageID, label);
			this.name = name;
			this.distance = distance;
		}

		/** @return the pageID */
		public int getDistance() {
			return distance;
		}

		/** @return the label */
		public String getName() {
			return name;
		}
	}
	/** Query for a given title, returning a set of articles. */
	public List<Article> query(String title, Logger logger) {
		for (String titleForm : cookedTitles(title)) {
			List<Article> results = queryTitleForm(titleForm, logger);
			if (!results.isEmpty())
				return results;
		}
		return new ArrayList<Article>();
	}

	/** Query for a given specific title form, returning a set
	 * of articles. */
	public List<Article> queryTitleForm(String title, Logger logger) {
		/* XXX: Case-insensitive search via SPARQL turns out
		 * to be surprisingly tricky.  Cover 91% of all cases
		 * by capitalizing words that are not stopwords  */
		boolean wasCapitalized = Character.toUpperCase(title.charAt(0)) == title.charAt(0);
		title = super.capitalizeTitle(title);

		title = title.replaceAll("\"", "").replaceAll("\\\\", "").replaceAll("\n", " ");

		String rawQueryStr =
			"{\n" +
			   // (A) fetch resources with @title label
			"  ?res rdfs:label \"" + title + "\"@en.\n" +
			"} UNION {\n" +
			   // (B) fetch also resources targetted by @title redirect
			"  ?redir dbo:wikiPageRedirects ?res .\n" +
			"  ?redir rdfs:label \"" + title + "\"@en .\n" +
			"} UNION {\n" +
			   // (C) fetch also resources targetted by @title disambiguation
			"  ?disamb dbo:wikiPageDisambiguates ?res .\n" +
			"  ?disamb rdfs:label \"" + title + "\"@en .\n" +
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
			 // weed out categories and other in-namespace junk
			 // FIXME: this also covers X-Men:.*, 2001:.*, ... movie titles
			"FILTER ( !regex(str(?res), '^http://dbpedia.org/resource/[^_]*:', 'i') )\n" +
			 // output only english labels, thankyouverymuch
			"FILTER ( LANG(?label) = 'en' )\n" +
			"";
		//logger.debug("executing sparql query: {}", rawQueryStr);
		List<Literal[]> rawResults = rawQuery(rawQueryStr,
			new String[] { "pageID", "label" }, 0);

		List<Article> results = new ArrayList<Article>(rawResults.size());
		for (Literal[] rawResult : rawResults) {
			String label = rawResult[1].getString();
			/* Undo capitalization if the label isn't all-caps
			 * and the original title wasn't capitalized either.
			 * Otherwise, all our terms will end up all-caps,
			 * a silly thing. */
			if (!wasCapitalized && (label.length() > 1 && Character.toUpperCase(label.charAt(1)) != label.charAt(1)))
				label = Character.toLowerCase(label.charAt(0)) + label.substring(1);
			logger.debug("DBpedia {}: [[{}]]", title, label);
	//		results.add(new Article(rawResult[0].getInt(), label));
		}
		for (Article a: getLabelsFromFlask(title)) {
			System.out.println(a.getLabel() + " " + a.getPageID());
			results.add(a);
		}

		return results;

		/*
		return rawQuery("?res rdfs:label \"" + title + "\"@en.\n" +
				"?res dbo:wikiPageID ?pageid\n" +
				// weed out categories and other in-namespace junk
				"FILTER ( !regex(str(?res), '^http://dbpedia.org/resource/[^_]*:', 'i') )\n",
				"pageid");
				*/

	}
	public synchronized List<Article> getLabelsFromFlask(String name) {
		List<Article> results = new LinkedList<>();
		try {
			String encodedName = URLEncoder.encode(name, "UTF-8").replace("+", "%20");
			String requestURL = "http://dbp-labels.ailao.eu:5000/search/" + encodedName;
			URL request = new URL(requestURL);
			URLConnection connection = request.openConnection();
			Gson gson = new Gson();
			JsonReader jr = new JsonReader(new InputStreamReader(connection.getInputStream()));

			jr.beginObject();
				jr.nextName(); //results :
				jr.beginArray();
				while (jr.hasNext()) {
					CustomArticle o = gson.fromJson(jr, CustomArticle.class);
					if (results.isEmpty()) // XXX: Only pick the single nearest concept
						results.add(o);
						/*XXX this is a quick fix
						since CustomArticle extends Article, nothing noteworthy should happen in the code using the same getters
						to actually extract the new information, change the return value from List<Article> to List<CustomArticle>, cast it,
						or just refactor CustomArticle to Article*/
					logger.debug("Server returned: {} {} {} {}", o.getDistance(), o.getLabel(), o.getName(), o.getPageID());
				}
				jr.endArray();
			jr.endObject();

		} catch (IOException e) {
			e.printStackTrace();
			return results;
		}
		return results;
	}

}
