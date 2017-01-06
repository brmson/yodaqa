package cz.brmlab.yodaqa.analysis.question;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import cz.brmlab.yodaqa.model.Question.ClueConcept;
import cz.brmlab.yodaqa.model.Question.Concept;
import cz.brmlab.yodaqa.provider.PrivateResources;
import cz.brmlab.yodaqa.provider.rdf.WikidataOntology;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.commons.lang.StringUtils;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Petr Marek on 4/6/2016.
 */
public class StepnickaToConcepts extends JCasAnnotator_ImplBase {

	private static final String STEPNICKA_ADDRES = PrivateResources.getInstance().getResource("stepnicka");

	final Logger logger = LoggerFactory.getLogger(StepnickaToConcepts.class);

	/** Concept blacklist. Created based on:
	 * grep Concept log_* | cut -d : -f 2- | cut -d- -f 2 | sort | uniq -c | sort -n  | grep '<[^ ]*>'
	 */
	String[] namebl_list = {
		/*  5 */ "Herec",
		/*  5 */ "Jmeniny",
		/*  5 */ "Korunovace",
		/*  6 */ "Citát",
		/*  7 */ "Autor",
		/*  8 */ "Choť",
		/*  8 */ "Prezident",
		/*  9 */ "Bydliště",
		/*  9 */ "Smrt",
		/* 21 */ "Dítě",
		/* 40 */ "Film",
	};

	String[] instance_whitelist = {
			"Q6256", //State
			"Q5", //Person
			"Q515", //City
			"Q15978299", //municipality with town privileges
			"Q35657", //state of the United States
			//XXX More general approach, superclass or something...
	};
	protected Set<String> namebl;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		if (namebl == null)
			namebl = new HashSet<String>(Arrays.asList(namebl_list));
	}

	@Override
	public void process(JCas resultView) throws AnalysisEngineProcessException {
		//Get tokens
		List<Token> tokens = new ArrayList<>(JCasUtil.select(resultView, Token.class));
		//Send to Stepnicka and get information for concepts
		List<StepnickaResult> stepnickaResults = getStepnickaResult(tokens);

		//create concept and clues
		ArrayList<Concept> concepts = (ArrayList<Concept>) createConcepts(resultView, stepnickaResults);
		for (int i = 0; i < concepts.size(); i++) {
			concepts.get(i).addToIndexes();
		}
	}

	public List<StepnickaResult> getStepnickaResult(Collection<Token> tokens) {
		List<StepnickaResult> res = new ArrayList<>();
		ArrayList<Token> rawTokens = new ArrayList<>(tokens);

		while (true) {
			try {
				res.addAll(stepnickaAsk(rawTokens));
				break; // Success!
			} catch (IOException e) {
				notifyRetry(e);
			}
		}
		res = filterConcepts(null, res, 0);
		List<StepnickaResult> subset;
		for (int i = Math.min(5, tokens.size() - 1); i > 0; i--) {
			for (int j = 0; j + i <= tokens.size(); j++) {
				List<Token> tokenSubset = rawTokens.subList(j, j+i);
				while (true) {
					try {
						subset = stepnickaAsk(tokenSubset);
						break; // Success!
					} catch (IOException e) {
						notifyRetry(e);
					}
				}
				res = filterConcepts(res, subset, j);
			}
		}
		return res;
	}

	public List<StepnickaResult> stepnickaAsk(Collection<Token> tokens) throws IOException {
		if (tokens.isEmpty())
			return new ArrayList<>();
		URL url = new URL(STEPNICKA_ADDRES);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		String input = buildRequestBody(tokens);
		OutputStream os = conn.getOutputStream();
		os.write(input.getBytes());
		os.flush();

		ArrayList<StepnickaResult> stepnickaResult = (ArrayList<StepnickaResult>) processResponse(conn.getInputStream(), tokens);
		conn.disconnect();
		return stepnickaResult;
	}

	private List<StepnickaResult> filterConcepts(List<StepnickaResult> oldConcepts, List<StepnickaResult> newConcepts, int shift) {
		List<StepnickaResult> notCovered = new ArrayList<>();
		if (oldConcepts == null)
			oldConcepts = new ArrayList<>();
		boolean covered;
		for (StepnickaResult c1: newConcepts) {
			covered = false;
			for (StepnickaResult c2: oldConcepts) {
				if (c1.getPosition().get(0) + shift >= c2.getPosition().get(0)
						&& c1.getPosition().get(1) + shift <= c2.getPosition().get(1)) {
					covered = true;
					break;
				}
			}
			if (!covered) notCovered.add(c1);
		}
		WikidataOntology wko = new WikidataOntology();
		List<StepnickaResult> filtered = new ArrayList<>();
		for (StepnickaResult res: notCovered) {
			ArrayList<UriList> newUriList = new ArrayList<>();
			for (int i = 0; i < res.getUriList().size(); i++) {
				Set<String> types = wko.getEntityType(res.getUriList().get(i).wiki_uri);
				if (types.size() > 0 && !Collections.disjoint(Arrays.asList(instance_whitelist), types)) {
					newUriList.add(res.getUriList().get(i));
				} else {
					logger.debug("Entity filtered: {}", res.getUriList().get(i).getConcept_name());
				}
			}
			if (newUriList.size() > 0) {
				filtered.add(new StepnickaResult(res.getPosition(), newUriList));
			}
		}
		oldConcepts.addAll(filtered);
		return oldConcepts;
	}

	private static String buildRequestBody(Collection<Token> tokens) {
		JsonObject jobject = new JsonObject();
		JsonArray jsonArray = new JsonArray();
		for (Token token : tokens) {
			if (token.getDiacritics() != null)
				jsonArray.add(new JsonPrimitive(token.getDiacritics().getValue()));
			else
				jsonArray.add(new JsonPrimitive(token.getCoveredText()));
		}
		jobject.add("query", jsonArray);
		return jobject.toString();
	}

	@SuppressWarnings("unchecked")
	private List<StepnickaResult> processResponse(InputStream stream, Collection<Token> tokens) {
		ArrayList<StepnickaResult> stepnickaResults = new ArrayList<>();
		JsonParser parser = new JsonParser();
		JsonArray jsonArray = parser.parse(new InputStreamReader(stream)).getAsJsonArray();
		for (int i = 0; i < jsonArray.size(); i++) {
			String matchedString = "";
			JsonObject jsonObject = (JsonObject) jsonArray.get(i);
			JsonArray position = jsonObject.getAsJsonArray("position");
			ArrayList<Integer> positions = new ArrayList<>();
			for (int j = 0; j < position.size(); j++) {
				positions.add(position.get(j).getAsInt());
			}
			try{
				int b = position.get(0).getAsInt();
				int e = position.get(1).getAsInt();
				List<Token> matchedSubList = new ArrayList<>(tokens).subList(b, e);
				List<String> stringSubList = new ArrayList<>();
				for (Token t: matchedSubList)
					stringSubList.add(t.getCoveredText());
				matchedString = StringUtils.join(stringSubList, " ");
			} catch (IndexOutOfBoundsException e) {
				logger.warn("Position array has only {} element(s)", position.size());
			}
			JsonArray uriList = jsonObject.getAsJsonArray("uri_list");
			ArrayList<UriList> uriLists = new ArrayList<>();
			for (int j = 0; j < uriList.size(); j++) {
				String wiki_uri = uriList.get(j).getAsJsonObject().get("wiki_uri").getAsString();
				String concept_name = uriList.get(j).getAsJsonObject().get("concept_name").getAsString().replace("_"," ");
				String uri = uriList.get(j).getAsJsonObject().get("uri").getAsString();
				float score = uriList.get(j).getAsJsonObject().get("score").getAsFloat();
				String dbpedia_uri = uriList.get(j).getAsJsonObject().get("dbpedia_uri").getAsString();
				int page_id = uriList.get(j).getAsJsonObject().get("page_id").getAsInt();
				String match_str = !matchedString.isEmpty() ? matchedString : jsonObject.get("match_str").getAsString();
				uriLists.add(new UriList(wiki_uri, concept_name, uri, score, dbpedia_uri, page_id, match_str));
			}
			StepnickaResult stepnickaResult = new StepnickaResult(positions, uriLists);
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
				String name = stepnickaResults.get(i).getUriList().get(j).getConcept_name();
				if (namebl.contains(name)) {
					logger.debug("ignoring blacklisted concept <<{}>>", name);
					continue;
				}
				Concept concept = new Concept(resultView);
				concept.setFullLabel(name);
				concept.setCookedLabel(name);
				concept.setMatchedStr(stepnickaResults.get(i).getUriList().get(j).getMatch_str());
				concept.setLabelProbability(stepnickaResults.get(i).getUriList().get(j).getScore());
				concept.setPageID(stepnickaResults.get(i).getUriList().get(j).getPage_id());
				concept.setWikiUrl(stepnickaResults.get(i).getUriList().get(j).getWiki_uri());
				//concept.setEditDistance(a.getDist());
				//concept.setLogPopularity(a.getPop());
				concept.setBySubject(false);
				concept.setByLAT(false);
				concept.setByNE(false);
				concept.setByNgram(false);
				concept.setByFuzzyLookup(false);
				concept.setByCWLookup(false);
				concept.setDescription("");

				int begin = resultView.getDocumentText().indexOf(concept.getMatchedStr());
				concept.setBegin(begin);
				concept.setEnd(begin + concept.getMatchedStr().length());

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

		public StepnickaResult(ArrayList<Integer> position, ArrayList<UriList> uriLists) {
			this.position = position;
			this.uriLists = uriLists;
		}

		public ArrayList<Integer> getPosition() {
			return position;
		}

		public ArrayList<UriList> getUriList() {
			return uriLists;
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
		private String match_str;

		public UriList(String wiki_uri, String concept_name, String uri, float score, String dbpedia_uri, int page_id, String match_str) {
			this.wiki_uri = wiki_uri;
			this.concept_name = concept_name;
			this.uri = uri;
			this.score = score;
			this.dbpedia_uri = dbpedia_uri;
			this.page_id = page_id;
			this.match_str = match_str;
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

		public String getMatch_str() {
			return match_str;
		}
	}
}
