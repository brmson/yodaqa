package cz.brmlab.yodaqa.analysis.question;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import cz.brmlab.yodaqa.model.Question.ClueConcept;
import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.provider.PrivateResources;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
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
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by Petr Marek on 4/6/2016.
 */
public class StepnickaToConcepts extends JCasAnnotator_ImplBase {

	private static final String STEPNICKA_ADDRES = PrivateResources.getInstance().getResource("stepnicka");

	final Logger logger = LoggerFactory.getLogger(StepnickaToConcepts.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	@Override
	public void process(JCas resultView) throws AnalysisEngineProcessException {
		//Get tokens
		Collection<Token> tokens = JCasUtil.select(resultView, Token.class);

		//Send to Stepnicka and get information for concepts
		List<StepnickaResult> stepnickaResults = getStepnickaResult(tokens);


		//create concept and clues
		ArrayList<Concept> concepts = (ArrayList<Concept>) createConcepts(resultView, stepnickaResults);
		for (int i = 0; i < concepts.size(); i++) {
			concepts.get(i).addToIndexes();
		}
	}

	public List<StepnickaResult> getStepnickaResult(Collection<Token> tokens) {
		ArrayList<StepnickaResult> res = null;
		while (true) {
			try {
				res = (ArrayList<StepnickaResult>) stepnickaAsk(tokens);
				break; // Success!
			} catch (IOException e) {
				notifyRetry(e);
			}
		}
		return res;
	}

	public List<StepnickaResult> stepnickaAsk(Collection<Token> tokens) throws IOException {
		URL url = new URL(STEPNICKA_ADDRES);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");

		String input = buildRequestBody(tokens);
		OutputStream os = conn.getOutputStream();
		os.write(input.getBytes("ascii"));
		os.flush();

		ArrayList<StepnickaResult> stepnickaResult = (ArrayList<StepnickaResult>) processResponse(conn.getInputStream());
		conn.disconnect();
		return stepnickaResult;
	}

	private static String buildRequestBody(Collection<Token> tokens) {
		JsonObject jobject = new JsonObject();
		JsonArray jsonArray = new JsonArray();
		for (Token token : tokens) {
			jsonArray.add(new JsonPrimitive(token.getCoveredText()));
		}
		jobject.add("query", jsonArray);
		return jobject.toString();
	}

	@SuppressWarnings("unchecked")
	private List<StepnickaResult> processResponse(InputStream stream) {
		ArrayList<StepnickaResult> stepnickaResults = new ArrayList<>();
		JsonParser parser = new JsonParser();
		JsonArray jsonArray = parser.parse(new InputStreamReader(stream)).getAsJsonArray();
		for (int i = 0; i < jsonArray.size(); i++) {
			JsonObject jsonObject = (JsonObject) jsonArray.get(i);
			JsonArray position = jsonObject.getAsJsonArray("position");
			ArrayList<Integer> positions = new ArrayList<>(); //IMPORTANT
			for (int j = 0; j < position.size(); j++) {
				positions.add(position.get(j).getAsInt());
			}
			JsonArray uriList = jsonObject.getAsJsonArray("uri_list");
			ArrayList<UriList> uriLists = new ArrayList<>(); //IMPORTANT
			for (int j = 0; j < uriList.size(); j++) {
				String wiki_uri = uriList.get(j).getAsJsonObject().get("wiki_uri").getAsString();
				String concept_name = uriList.get(j).getAsJsonObject().get("concept_name").getAsString();
				String uri = uriList.get(j).getAsJsonObject().get("uri").getAsString();
				float score = uriList.get(j).getAsJsonObject().get("score").getAsFloat();
				String dbpedia_uri = uriList.get(j).getAsJsonObject().get("dbpedia_uri").getAsString();
				int page_id = uriList.get(j).getAsJsonObject().get("page_id").getAsInt();
				uriLists.add(new UriList(wiki_uri, concept_name, uri, score, dbpedia_uri, page_id));
			}
			String match_str = jsonObject.get("match_str").getAsString();
			StepnickaResult stepnickaResult = new StepnickaResult(positions, uriLists, match_str);
			stepnickaResults.add(stepnickaResult);
		}

		return stepnickaResults;
	}

	protected static void notifyRetry(Exception e) {
		e.printStackTrace();
		System.err.println("*** " + STEPNICKA_ADDRES + " Stepnicka Query (temporarily?) failed, retrying in a moment...");
		try {
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e2) { // oof...
			e2.printStackTrace();
		}
	}

	/**
	 * Create concepts
	 * @param resultView
	 * @param stepnickaResults
	 * @return
	 */
	private List<Concept> createConcepts(JCas resultView, List<StepnickaResult> stepnickaResults) {
		ArrayList<Concept> concepts = new ArrayList<>();
		for (int i = 0; i < stepnickaResults.size(); i++) {
			ArrayList<UriList> uriLists = stepnickaResults.get(i).getUriList();
			ArrayList<Concept> conceptsForClue = new ArrayList<>();
			for (int j = 0; j < uriLists.size(); j++) {
				Concept concept = new Concept(resultView);
				concept.setBegin(stepnickaResults.get(i).getPosition().get(0));
				concept.setEnd(stepnickaResults.get(i).getPosition().get(1));
				concept.setFullLabel(stepnickaResults.get(i).getUriList().get(j).getConcept_name());
				concept.setCookedLabel(stepnickaResults.get(i).getUriList().get(j).getConcept_name().replace("_"," "));
				concept.setLabelProbability(stepnickaResults.get(i).getUriList().get(j).getScore());
				concept.setPageID(stepnickaResults.get(i).getUriList().get(j).getPage_id());
				//concept.setEditDistance(a.getDist());
				//concept.setLogPopularity(a.getPop());
				concept.setBySubject(false);
				concept.setByLAT(false);
				concept.setByNE(false);
				concept.setByNgram(false);
				concept.setByFuzzyLookup(false);
				concept.setByCWLookup(false);
				concept.setDescription("");
				concepts.add(concept);
				conceptsForClue.add(concept);

				logger.debug("creating concept <<{}>> cooked <<{}>> --> {}, d={}, lprob={}, logpop={}",
						concept.getFullLabel(), concept.getCookedLabel(),
						String.format(Locale.ENGLISH, "%.3f", concept.getScore()),
						String.format(Locale.ENGLISH, "%.2f", concept.getEditDistance()),
						String.format(Locale.ENGLISH, "%.3f", concept.getLabelProbability()),
						String.format(Locale.ENGLISH, "%.3f", concept.getLogPopularity()));
			}
			createClueConcept(resultView, conceptsForClue);
		}

		return concepts;
	}

	private void createClueConcept(JCas resultView, List<Concept> concepts) {
		if (concepts.size() > 0) {
			ClueConcept clue = new ClueConcept(resultView);
			clue.setBegin(concepts.get(0).getBegin());
			clue.setEnd(concepts.get(0).getEnd());
			//clue.setBase();
			FSList conceptFSList = FSCollectionFactory.createFSList(resultView, concepts);
			clue.setConcepts(conceptFSList);
			clue.setWeight(concepts.get(0).getScore());
			clue.setLabel(concepts.get(0).getFullLabel());
			clue.setIsReliable(true);
			clue.addToIndexes();
		}
	}

	/**
	 * Class handling stepnicka return
	 */
	public class StepnickaResult {
		private ArrayList<Integer> position;
		private ArrayList<UriList> uriLists;
		private String match_str;

		public StepnickaResult(ArrayList<Integer> position, ArrayList<UriList> uriLists, String match_str) {
			this.position = position;
			this.match_str = match_str;
			this.uriLists = uriLists;
		}

		public ArrayList<Integer> getPosition() {
			return position;
		}

		public ArrayList<UriList> getUriList() {
			return uriLists;
		}

		public String getMatch_str() {
			return match_str;
		}
	}

	/**
	 *
	 */
	private class UriList {
		private String wiki_uri;
		private String concept_name;
		private String uri;
		private float score;
		private String dbpedia_uri;
		private int page_id;

		public UriList(String wiki_uri, String concept_name, String uri, float score, String dbpedia_uri, int page_id) {
			this.wiki_uri = wiki_uri;
			this.concept_name = concept_name;
			this.uri = uri;
			this.score = score;
			this.dbpedia_uri = dbpedia_uri;
			this.page_id = page_id;
		}

		public String getWiki_uri() {
			return wiki_uri;
		}

		public String getConcept_name() {
			return concept_name;
		}

		public String getUri() {
			return uri;
		}

		public float getScore() {
			return score;
		}

		public String getDbpedia_uri() {
			return dbpedia_uri;
		}

		public int getPage_id() {
			return page_id;
		}
	}
}
