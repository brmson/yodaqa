package cz.brmlab.yodaqa.provider.rdf;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Literal;

import cz.brmlab.yodaqa.provider.Wordnet;

import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.dictionary.Dictionary;
import net.sf.extjwnl.data.Synset;

import org.apache.commons.lang.WordUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;

/** A wrapper around DBpedia dataset that maps concepts to
 * dbpedia2:wordnet_type ontology pieces. This gives some evidence
 * of the type of the concept that we directly match with other
 * WordNet data.
 *
 * XXX: The WordNet types here also disambiguate word senses.
 * We drop this information for now, instead of converting this
 * to synset numbers. */

public class DBpediaWNTypes extends DBpediaLookup {
	private static final Log logger = LogFactory.getLog(DBpediaWNTypes.class);
	
	Dictionary dictionary = null;
	
	public void initialize() throws ResourceInitializationException
	{
		dictionary = Wordnet.getDictionary();
	}
	
	/** Query for a given title, returning a set of types. The type is in
	 * the form of string/synset. */
	public List<String> query(String title, Logger logger) {
		for (String titleForm : cookedTitles(title)) {
			List<String> results = queryTitleForm(titleForm, logger);
			if (!results.isEmpty())
				return results;
		}
		return new ArrayList<String>();
	}

	/** Query for a given specific title form, returning a set
	 * of types. The type is in the form of string/synset. */
	public List<String> queryTitleForm(String title, Logger logger) {
		/* XXX: Case-insensitive search via SPARQL turns out
		 * to be surprisingly tricky.  Cover 91% of all cases
		 * by capitalizing words that are not stopwords  */
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
			"}\n" +
			 // set the output variable
			"?res dbpedia2:wordnet_type ?type .\n" +

			 // weed out resources that are categories and other in-namespace junk
			"FILTER ( !regex(str(?res), '^http://dbpedia.org/resource/[^_]*:', 'i') )\n" +
			"";
		//logger.debug("executing sparql query: {}", rawQueryStr);
		List<Literal[]> rawResults = rawQuery(rawQueryStr,
			new String[] { "type" }, 0);

		List<String> results = new LinkedList<String>();
		for (Literal[] rawResult : rawResults) {
			// XXX: we assume the type is always noun
			int senseIdx = Integer.parseInt(rawResult[0].getString().replaceAll("^.*-([^-]*)$", "$1"));
			String typeLabel = rawResult[0].getString().replaceAll("^synset-([^-]*)-.*$", "$1").replaceAll("_", " ");

			try {
				IndexWord w = dictionary.getIndexWord(net.sf.extjwnl.data.POS.NOUN, typeLabel);
				Synset s = w.getSenses().get(senseIdx - 1);
				long synset = s.getOffset();

				// logger.debug("DBpedia {} wntype: [[{}]]", title, typeLabel);
				results.add(typeLabel + "/" + Long.toString(synset));
			} catch (Exception e) {
				logger.debug("DBpedia {} wntype [[{}]] exception: {}", title, typeLabel, e);
			}
		}

		return results;
	}
}
