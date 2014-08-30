package cz.brmlab.yodaqa.provider.rdf;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Literal;

import org.slf4j.Logger;

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
	public List<Article> query(String title, Logger logger) {
		/* XXX: Case-insensitive search via SPARQL turns out
		 * to be surprisingly tricky.  Cover 90% of all cases
		 * by force-capitalizing the first letter in the sought
		 * after title. */
		boolean wasCapitalized = Character.toUpperCase(title.charAt(0)) == title.charAt(0);
		title = Character.toUpperCase(title.charAt(0)) + title.substring(1);

		title = title.replaceAll("\"", "");
		String rawQueryStr =
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
			"";
		//logger.debug("executing sparql query: {}", rawQueryStr);
		List<Literal[]> rawResults = rawQuery(rawQueryStr,
			new String[] { "pageID", "label" });

		List<Article> results = new ArrayList<Article>(rawResults.size());
		for (Literal[] rawResult : rawResults) {
			String label = rawResult[1].getString();
			/* Undo capitalization if the label isn't all-caps
			 * and the original title wasn't capitalized either.
			 * Otherwise, all our terms will end up all-caps,
			 * a silly thing. */
			if (!wasCapitalized && (Character.toUpperCase(label.charAt(1)) != label.charAt(1)))
				label = Character.toLowerCase(label.charAt(0)) + label.substring(1);
			logger.debug("DBpedia {}: [[{}]]", title, label);
			results.add(new Article(rawResult[0].getInt(), label));
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
