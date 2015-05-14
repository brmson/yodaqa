package cz.brmlab.yodaqa.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/** Query the "geneontology" solr server for stuff, returning type of the stuff.
 * We don't actually use the solr class but just bolt together the correct
 * query as we have sniffed it in Firebug. */

public class GeneOntologyCall {
	// http://geneontology-golr.stanford.edu/solr/select?defType=edismax&qt=standard&indent=on&wt=json&rows=10&start=0&fl=bioentity,bioentity_name,taxon,panther_family,type,source,annotation_class_list,synonym,bioentity_label,taxon_label,panther_family_label,annotation_class_list_label,annotation_class_list_map,score,id&json.nl=arrarr&facet.limit=25&fq=document_category:%22bioentity%22&qf=bioentity%5E2&qf=bioentity_label_searchable%5E2&qf=bioentity_name_searchable%5E1&qf=bioentity_internal_id%5E1&qf=synonym%5E1&qf=isa_partof_closure_label_searchable%5E1&qf=regulates_closure%5E1&qf=regulates_closure_label_searchable%5E1&qf=panther_family_searchable%5E1&qf=panther_family_label_searchable%5E1&qf=taxon_closure_label_searchable%5E1&packet=1&callback_type=search&q="aurora kinase"'

	protected static String server = "http://geneontology-golr.stanford.edu/solr/select";

	protected String fetchQuery(String query) {
		// Build the HTTP Request
		HttpClient client = new HttpClient();
		GetMethod method = new GetMethod(server.toString());
		NameValuePair params[] = {
			new NameValuePair("defType", "edismax"),
			new NameValuePair("qt", "standard"),
			new NameValuePair("indent", "on"),
			new NameValuePair("wt", "json"),
			new NameValuePair("rows", "10"),
			new NameValuePair("start", "0"),
			new NameValuePair("fl", "bioentity,bioentity_name,taxon,panther_family,type,source,annotation_class_list,synonym,bioentity_label,taxon_label,panther_family_label,annotation_class_list_label,annotation_class_list_map,score,id"),
			new NameValuePair("json.nl", "arrarr"),
			new NameValuePair("facet.limit", "25"),
			new NameValuePair("fq", "document_category:\"bioentity\""),
			new NameValuePair("qf", "bioentity^2"),
			new NameValuePair("qf", "bioentity_label_searchable^2"),
			new NameValuePair("qf", "bioentity_name_searchable^1"),
			new NameValuePair("qf", "bioentity_internal_id^1"),
			new NameValuePair("qf", "synonym^1"),
			new NameValuePair("packet", "1"),
			new NameValuePair("callback_type", "search"),
			new NameValuePair("q", "\"" + query.replaceAll("\"", "\\\"") + "\""),
		};
		method.setQueryString(params);

		// Execute and retrieve result
		try {
			client.executeMethod(method);
			String response = method.getResponseBodyAsString();
			// System.out.println(response);
			return response;
		} catch (IOException e) {
			return null;
		}
	}

	protected Collection<String> extractTypes(String query, String response) {
		// Parse result
		JSONObject parsedResult = (JSONObject) JSONValue.parse(response);

		JSONObject res = (JSONObject) parsedResult.get("response");
		JSONArray docs = (JSONArray) res.get("docs");

		List<String> types = new ArrayList<>();
		for (Object dO : docs) {
			JSONObject d = (JSONObject) dO;

			// require exact match with one of the main fields
			boolean matches = false;
			for (String s : new String[]{ "bioentity", "bioentity_label", "bioentity_name", "synonym" }) {
				if (!d.containsKey(s))
					continue;
				List a;
				if (d.get(s) instanceof String) {
					a = Arrays.asList(new String[]{ (String) d.get(s) });
				} else {
					a = (JSONArray) d.get(s);
				}
				for (Object fO : a) {
					String f = (String) fO;
					if (f.toLowerCase().equals(query.toLowerCase())) {
						matches = true;
						break;
					}
				}
			}
			if (!matches)
				continue;

			String t = (String) d.get("type");
			t = t.replaceAll("_", " ");
			for (String t2 : types) {
				if (t2.equals(t)) {
					t = null;
					break;
				}
			}
			if (t == null)
				continue;
			types.add(t);
		}
		return types;
	}

	public Collection<String> getTypes(String query) {
		// Manual blacklist (protein thiS)
		if (query.toLowerCase().equals("this"))
			return null;

		String response = fetchQuery(query);
		if (response == null)
			return null;
		return extractTypes(query, response);
	}
}
