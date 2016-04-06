package cz.brmlab.yodaqa.analysis.question;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;

import cz.brmlab.yodaqa.provider.PrivateResources;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CzechPOSTagger extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(CzechPOSTagger.class);
	private static String URL_STRING;
	private HttpURLConnection conn = null;

	private static class Response {
		@SerializedName("lemmas")
		List<String> lemmas;
		@SerializedName("pos_tags")
		List<String> posTags;
		@SerializedName("diacritics")
		List<String> diacritics;
	}

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		URL_STRING = PrivateResources.getInstance().getResource("lemma_url");
		if (URL_STRING == null) {
			logger.warn("Czech pos tagger not used (URL not specified)");
		}
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		if (URL_STRING == null)
			return;
		logger.debug("Czech pos tagger");
		List<Token> tokens = new ArrayList<>(JCasUtil.select(jCas, Token.class));
		URL url;
		try {
			url = new URL(URL_STRING);
			conn = (HttpURLConnection) url.openConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String jsonRequest = createRequest(tokens);
		InputStream is = sendRequest(jsonRequest);
		Response response = processResponse(is);
		addTagToTokens(jCas, tokens, response);
		conn.disconnect();
		ROOT r = new ROOT(jCas, 0, jCas.getDocumentText().length());
		r.addToIndexes();
	}

	private String createRequest(List<Token> tokens) {
		JsonObject jObject = new JsonObject();
		JsonArray jArray = new JsonArray();
		for(Token tok: tokens) {
			jArray.add(new JsonPrimitive(tok.getLemma().getValue()));
		}
		jObject.add("query", jArray);
		return jObject.toString();
	}

	private InputStream sendRequest(String json) {
		try {
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");

			OutputStream os = conn.getOutputStream();
			os.write(json.getBytes());
			os.flush();
			return conn.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Response processResponse(InputStream is) {
		Gson gson = new Gson();
		JsonReader reader = new JsonReader(new InputStreamReader(is));
		reader.setLenient(true);
		return gson.fromJson(reader, Response.class);
	}

	private void addTagToTokens(JCas jCas, List<Token> tokens, Response response) {
		//FIXME Add list size check
		for (int i = 0; i < tokens.size(); i++) {
			Token tok = tokens.get(i);
			POS pos = new POS(jCas);
//			if (tok.getPos() != null) tok.getPos();
			Lemma lemma = new Lemma(jCas);
//			if (tok.getLemma() != null) lemma = tok.getLemma();
			logger.debug("Token: " + response.lemmas.get(i) + " " + response.posTags.get(i));
			pos.setPosValue(response.posTags.get(i));
			lemma.setValue(response.diacritics.get(i));
			tok.setLemma(lemma);
			tok.setPos(pos);
		}
	}

}
