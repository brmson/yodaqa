package cz.brmlab.yodaqa.provider.solr;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * SolrNamedSource manages named Solr instances.  This allows the
 * main pipeline just set up a bunch of sources at the beginning,
 * then refer to them by name in PrimarySearch and allow these
 * references to be resolved in the ResultGenerator. */

public class SolrNamedSource {
	protected static Map<String, Solr> solrNameMap = new HashMap<String, Solr>();

	public static void register(String name, String core, String serverUrl) throws Exception {
		boolean embedded = serverUrl == null;
		Solr solr = new Solr(serverUrl, null, embedded, core);
		register(name, solr);
	}

	public static void register(String name, Solr solr) {
		solrNameMap.put(name, solr);
	}

	public static Solr get(String name) {
		return solrNameMap.get(name);
	}

	/** nameSet() returns set of all registered sources, allowing
	 * iteration over all of them. */
	public static Set<String> nameSet() {
		return solrNameMap.keySet();
	}
}
