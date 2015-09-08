/*
 *  Copyright 2012 Carnegie Mellon University
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package cz.brmlab.yodaqa.provider.solr;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Solr implements Closeable {
	final Logger logger = LoggerFactory.getLogger(Solr.class);

	protected final SolrServer server;
	protected String url;

	boolean embedded;

	public Solr(String serverUrl) throws Exception {
		this.server = createSolrServer(serverUrl);
	}
	public Solr(String serverUrl, Integer serverPort, Boolean embedded,
			String core) throws Exception {
		if (embedded != null && embedded.booleanValue()) {
			this.server = createEmbeddedSolrServer(core);
			this.embedded = true;
			this.url = "";
		} else {
			logger.info("Running Solr retrieval on remote mode");
			this.server = createSolrServer(serverUrl);
		}
	}

	private SolrServer createSolrServer(String url) throws Exception {
		SolrServer server = new HttpSolrServer(url);
		// server.ping();
		this.url = url;
		return server;
	}

	private SolrServer createEmbeddedSolrServer(String core) throws Exception {
		System.setProperty("solr.solr.home", core);
		CoreContainer.Initializer initializer = new CoreContainer.Initializer();
		CoreContainer coreContainer = initializer.initialize();
		return new EmbeddedSolrServer(coreContainer, "");
	}

	public SolrDocumentList runQuery(String q, int results)
			throws SolrServerException {
		SolrQuery query = new SolrQuery();
		query.setQuery(escapeQuery(q));
		query.setRows(results);
		query.setFields("*", "score");
		QueryResponse rsp;
		while (true) {
			try {
				rsp = server.query(query, METHOD.POST);
				break; // Success!
			} catch (SolrServerException e) {
				if (e.getRootCause() instanceof IOException)
					notifyRetry(e);
				else
					throw e;
			}
		}
		return rsp.getResults();
	}

	public SolrDocumentList runQuery(SolrQuery query, int results)
			throws SolrServerException {
		QueryResponse rsp;
		while (true) {
			try {
				rsp = server.query(query);
				break; // Success!
			} catch (SolrServerException e) {
				if (e.getRootCause() instanceof IOException)
					notifyRetry(e);
				else
					throw e;
			}
		}
		return rsp.getResults();
	}

	public String getDocText(String id) throws SolrServerException {
		String q = "id:" + id;
		SolrQuery query = new SolrQuery();
		query.setQuery(q);
		query.setFields("text");
		QueryResponse rsp;
		while (true) {
			try {
				rsp = server.query(query);
				break; // Success!
			} catch (SolrServerException e) {
				if (e.getRootCause() instanceof IOException)
					notifyRetry(e);
				else
					throw e;
			}
		}

		String docText = "";
		if (rsp.getResults().getNumFound() > 0) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			ArrayList<String> results = (ArrayList) rsp.getResults().get(0)
					.getFieldValues("text");
			docText = results.get(0);
		}
		return docText;
	}

	public String escapeQuery(String term) {
		term = term.replace('?', ' ');
		term = term.replace('[', ' ');
		term = term.replace(']', ' ');
		term = term.replace('/', ' ');
		term = term.replace("\\", "");
		term = term.replaceAll("\'", "");
		return term;
	}

	public void close() {
		if (embedded) {
			((EmbeddedSolrServer) server).shutdown();
		}
	}


	public SolrDocumentList runQuery(Collection<SolrTerm> terms, int nResults, SolrQuerySettings settings)
			throws SolrServerException {
		return runQuery(terms, nResults, settings, logger);
	}
	public SolrDocumentList runQuery(Collection<SolrTerm> terms, int nResults, SolrQuerySettings settings,
			Logger cLogger) throws SolrServerException {
		String query = formulateQuery(terms, settings, cLogger);
		return runQuery(query, nResults);
	}

	protected String formulateQuery(Collection<SolrTerm> terms, SolrQuerySettings settings, Logger cLogger) {
		StringBuffer result = new StringBuffer();
		for (SolrTerm term : terms) {
			if (settings.isProximityOnly())
				continue;
			if (term.isRequired() && settings.areCluesAllRequired())
				result.append("+");
			// drop quote characters; more escaping is done in escapeQuery()
			String keyterm = term.getTermStr().replace("\"", "");
			result.append("(");
			boolean isFirstField = true;
			for (String prefix : settings.getSearchPrefixes()) {
				if (!isFirstField)
					result.append(" OR ");
				else
					isFirstField = false;
				result.append(prefix + "\"" + keyterm + "\"");
			}
			result.append(")^" + term.getWeight() + " ");
		}
		for (int i = 0; i < settings.getProximityNum(); i++)
			for (String prefix : settings.getSearchPrefixes())
				formulateProximityQuery(terms, settings, prefix, result, i);
		String query = result.toString();
		cLogger.info(" QUERY: " + query);
		return query;
	}

	protected void formulateProximityQuery(Collection<SolrTerm> terms, SolrQuerySettings settings, String prefix, StringBuffer result, int degree) {
		result.append(" (" + prefix + "\"");

		int n_terms = 0;
		double sumWeight = 0;
		for (SolrTerm term : terms) {
			// ignore optional terms in proximity queries, except
			// when that'd mean we drop them completely
			if (!term.isRequired() && !settings.isProximityOnly())
				continue;
			// drop quote characters; more escaping is done in escapeQuery()
			String keyterm = term.getTermStr().replace("\"", "");
			result.append(keyterm + " ");
			sumWeight += term.getWeight();
			n_terms += 1;
		}

		int finalDist = settings.getProximityBaseDist()
			* ((int) Math.pow(settings.getProximityBaseFactor(), degree))
			* n_terms;
		double finalWeight = (sumWeight / Math.pow(2, degree));
		result.append("\"~" + finalDist + ")^" + finalWeight);
	}

	public SolrDocumentList runIDQuery(Collection<Integer> IDs, int nResults,
			Logger cLogger) throws SolrServerException {
		String query = formulateIDQuery(IDs, cLogger);
		return runQuery(query, nResults);
	}

	protected String formulateIDQuery(Collection<Integer> IDs, Logger cLogger) {
		StringBuffer result = new StringBuffer();
		for (Integer ID : IDs)
			result.append("id:" + ID + " ");
		String query = result.toString();
		cLogger.info(" QUERY: " + query);
		return query;
	}


	protected void notifyRetry(Exception e) {
		e.printStackTrace();
		System.err.println("*** " + url + " Solr Query (temporarily?) failed, retrying in a moment...");
		try {
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e2) { // oof...
			e2.printStackTrace();
		}
	}
}
