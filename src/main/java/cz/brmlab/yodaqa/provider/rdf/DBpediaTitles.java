package cz.brmlab.yodaqa.provider.rdf;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Literal;

/** A wrapper around DBpedia "Titles" dataset that maps titles to
 * Wikipedia articles. The main point of using this is to find out
 * whether an article with the given title (termed also "label")
 * exists.
 *
 * Articles are represented as (label, pageId), where label is the
 * article title. The label is included as we pass through redirects. */

public class DBpediaTitles extends CachedJenaLookup {
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

	/** Query for a given title, returning a set of articles. */
	public List<Article> query(String title) {
		title = title.replaceAll("\"", "\\\"");
		List<Literal[]> rawResults = rawQuery(
			"{\n" +
			   // (A) fetch resources with @title label
			"  ?res rdfs:label \"" + title + "\"@en.\n" +
			"} UNION {\n" +
			   // (B) fetch also resources targetted by @title redirect
			"  ?redir dbo:wikiPageRedirects ?res .\n" +
			"  ?redir rdfs:label \"" + title + "\"@en .\n" +
			"}\n" +
			 // for (B), we are also getting a redundant (A) entry;
			 // identify the redundant (A) entry by filling
			 // ?redirTarget in that case
			"OPTIONAL { ?res dbo:wikiPageRedirects ?redirTarget . }\n" +
			 // set the output variables
			"?res dbo:wikiPageID ?pageID .\n" +
			"?res rdfs:label ?label .\n" +

			 // ignore the redundant (A) entries (that are redirects)
			"FILTER ( !BOUND(?redirTarget) )\n" +
			 // weed out categories and other in-namespace junk
			"FILTER ( !regex(str(?y), '^http://dbpedia.org/resource/[^_]*:', 'i') )\n" +
			 // output only english labels, thankyouverymuch
			"FILTER ( LANG(?label) = 'en' )\n" +
			"",
			new String[] { "pageID", "label" });

		List<Article> results = new ArrayList<Article>(rawResults.size());
		for (Literal[] rawResult : rawResults) {
			results.add(new Article(rawResult[0].getInt(), rawResult[1].getString()));
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
}
