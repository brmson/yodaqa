package cz.brmlab.yodaqa.provider.rdf;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import com.hp.hpl.jena.query.*;
import cz.brmlab.yodaqa.analysis.StopWordFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;

import cz.brmlab.yodaqa.analysis.answer.SyntaxCanonization;

/** This is an abstract base class for accessing various RDF resources,
 * typically DBpedia.  We leverage Apache Jena for the backend, plus
 * employ a cache store of already obtained query responses, since by
 * nature we will have a massive repeat rate of our queries.  Our
 * interface is optimized for direct triplet resolution rather than
 * complex queries.
 *
 * TODO: Actually do cache the response. :-) */

public abstract class CachedJenaLookup {
	final Logger logger = LoggerFactory.getLogger(CachedJenaLookup.class);

	protected String service;
	protected String prefixes;
	private static RDFCache cache = new RDFCache();
	private StopWordFilter filter = new StopWordFilter();

	/** Initialize a CachedJenaLookup object. */
	public CachedJenaLookup(String service_, String prefixes_) {
		service = service_;
		prefixes =
			"PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
			"PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
			"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
			prefixes_;
	}

	/** Issue a select statement, returning list of resource
	 * values.  limit is the limit on number of results; pass 0
	 * to specify no limit.
	 *
	 * The resources[] array lists list of variables to fetch.
	 * The variable names can be prefixed by '/' to indicate
	 * that a resource identifier, instead of literal, is to
	 * be fetched (or null, failing that).
	 *
	 * Example: rawQuery("?lab rdfs:label \"Achilles\"@en", "lab", 0); */
	public List<Literal[]> rawQuery(String distinct, String selectWhere, String resources[], int limit) {
		ArrayList<String> varNames = new ArrayList<>();
		for (String r : resources)
			varNames.add(r.replace("/", ""));

		String queryExpr = prefixes + "SELECT " + distinct + "?"
			+ StringUtils.join(varNames, " ?")
			+ " WHERE { " + selectWhere + " }"
			+ (limit > 0 ? "LIMIT " + Integer.toString(limit) : "");
		if (cache.contains(queryExpr)) {
			return cache.retrieve(queryExpr);
		}
		QueryExecution qe = QueryExecutionFactory.sparqlService(service, queryExpr);
//		logger.debug(queryExpr);

		ResultSet rs;
//		RedisCache redisCache = RedisCache.getInstance();
//		logger.debug("Before load" + Thread.currentThread().getName());
//		ResultSet rs = redisCache.loadResultSet(queryExpr);
//		logger.debug("After load");
//		logger.debug("Resultset " + rs);
//		if (rs == null) {
//			logger.debug("Not loading from cache");
			while (true) {
				try {
					rs = qe.execSelect();
					break; // Success!
				} catch (QueryExceptionHTTP e) {
					e.printStackTrace();
					System.err.println("*** " + service + " SPARQL Query (temporarily?) failed, retrying in a moment...");
					System.err.println("Please refer to the README.md for tips on disabling this lookup.");
					try {
						TimeUnit.SECONDS.sleep(10);
					} catch (InterruptedException e2) { // oof...
						e2.printStackTrace();
					}
				}
			}
//			logger.debug("Before store"+ Thread.currentThread().getName());
//			redisCache.storeResultSet(queryExpr, rs);
//			logger.debug("After store");
//		}

		List<Literal[]> results = new LinkedList<Literal[]>();
		while (rs.hasNext()) {
			QuerySolution s = rs.nextSolution();
			Literal[] result = new Literal[resources.length];
			for (int i = 0; i < resources.length; i++) {
				RDFNode node = s.get("?" + varNames.get(i));
				if (resources[i].startsWith("/")) {
					// resource-only mode
					if (node == null)
						result[i] = null;
					else if (node.isLiteral())
						result[i] = null;
					else if (node.isResource())
						result[i] = ResourceFactory.createPlainLiteral(node.asResource().getURI());
					else assert(false);
				} else {
					if (node == null)
						result[i] = null;
					else if (node.isLiteral())
						result[i] = node.asLiteral();
					else if (node.isResource())
						result[i] = ResourceFactory.createPlainLiteral(node.asResource().getLocalName());
					else assert(false);
				}
			}
			results.add(result);
		}
		qe.close();
		cache.add(queryExpr,results);
		return results;
	}

	public List<Literal[]> rawQuery(String selectWhere, String resources[], int limit) {
		return rawQuery("", selectWhere, resources, limit);
	}

	public List<Literal[]> rawQueryDisctinct(String selectWhere, String resources[], int limit) {
		return rawQuery("DISTINCT ", selectWhere, resources, limit);
	}

	/** Generate a list of various cooked forms of the title for
	 * fallback.  Cooked means without the leading "the", etc. */
	public static List<String> cookedTitles(String title) {
		List<String> titles = new LinkedList<String>();
		titles.add(title); // by default, try non-cooked

		/* Syntax cooked will drop leading the-, a-, etc. */
		String canonTitle = SyntaxCanonization.getCanonText(title);
		if (!canonTitle.equals(title) && !canonTitle.matches("^\\s*$"))
			titles.add(canonTitle);

		return titles;
	}

	/**
	 * Converts title to upper case with the exception of stopwords (the, and, or.. etc.)
	 * The first letter is always transformed
	 */
	protected String capitalizeTitle(String title) {
		StringTokenizer str = new StringTokenizer(title);
		String resultTitle = "";
		while (str.hasMoreTokens()) {
			String token = str.nextToken();
			if (filter.contains(token)) {
				resultTitle += token + " ";
				continue;
			}
			resultTitle += WordUtils.capitalize(token)+" ";
		}
		return (Character.toUpperCase(resultTitle.charAt(0)) + resultTitle.substring(1)).trim();
	}
}
