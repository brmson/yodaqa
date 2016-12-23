package cz.brmlab.yodaqa.provider.url;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Provider of various API URLs we depend on.  This offers a single place
 * for managing the endpoints used, and overriding them with your local
 * deployments.
 * <p>
 * In theory, we support multiple backend sets, but offer no UI to switch
 * between these yet. (TODO)
 * <p>
 */
public class UrlManager {
    private static UrlManager instance = null;

    public static UrlManager getInstance() {
        if (instance == null) {
            instance = new UrlManager();
            return instance;
        } else {
            return instance;
        }
    }

    private List<BackendUrlGroup> urlLookUpTable;
    private BackendUrlGroup currentBackend;

    private UrlManager() {
        updateUrlTable();
        setCurrentBackend(0); //Default is 0: ailao.eu, 1: loopback, different if custom tables loaded
    }

    public List<BackendUrlGroup> getUrlLookUpTable() {
        return urlLookUpTable;
    }

    public void setUrlLookUpTable(List<BackendUrlGroup> newLookUpTable) {
        urlLookUpTable = newLookUpTable;
    }

    public BackendUrlGroup getCurrentBackend() {
        return currentBackend;
    }

    public boolean setCurrentBackend(int currentBackendIndex) {
        if (currentBackendIndex < urlLookUpTable.size()) {
            currentBackend = urlLookUpTable.get(currentBackendIndex);
            return true;
        } else {
            return false;
        }
    }

    public String printState() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Currently selected backend: ");
        stringBuilder.append(urlLookUpTable.indexOf(currentBackend));
        stringBuilder.append("\n\nCurrent Lookup Table:\n");
        stringBuilder.append(currentBackend.toString());
        stringBuilder.append("\nCurrent properties:");
        stringBuilder.append("System.getProperty(\"cz.brmlab.yodaqa.dbpediaurl\") ");
        stringBuilder.append(System.getProperty("cz.brmlab.yodaqa.dbpediaurl"));
        stringBuilder.append("\nSystem.getProperty(\"cz.brmlab.yodaqa.freebaseurl\") ");
        stringBuilder.append(System.getProperty("cz.brmlab.yodaqa.freebaseurl"));
        stringBuilder.append("\nSystem.getProperty(\"cz.brmlab.yodaqa.label1url\") ");
        stringBuilder.append(System.getProperty("cz.brmlab.yodaqa.label1url"));
        stringBuilder.append("\nSystem.getProperty(\"cz.brmlab.yodaqa.label2url\") ");
        stringBuilder.append(System.getProperty("cz.brmlab.yodaqa.label2url"));
        stringBuilder.append("\nSystem.getProperty(\"cz.brmlab.yodaqa.solrurl\") ");
        stringBuilder.append(System.getProperty("cz.brmlab.yodaqa.solrurl"));
        stringBuilder.append("\n\nURL each backend receives:");
        stringBuilder.append("\nDBpedia: ");
        stringBuilder.append(currentBackend.getUrl(UrlConstants.DBPEDIA));
        stringBuilder.append("\nFreebase: ");
        stringBuilder.append(currentBackend.getUrl(UrlConstants.FREEBASE));
        stringBuilder.append("\nDBPedia Label Service: ");
        stringBuilder.append(currentBackend.getUrl(UrlConstants.DBPEDIA_LABEL));
        stringBuilder.append("\nDictionary Wiki Label Service: ");
        stringBuilder.append(currentBackend.getUrl(UrlConstants.DICTIONARY_LABEL));
        stringBuilder.append("\nSolr: ");
        stringBuilder.append(currentBackend.getUrl(UrlConstants.SOLR));
        return stringBuilder.toString();
    }


    /**
     * Reads configuration file
     *
     * @return Raw content of configuration file (should be JSON)
     */
    private String readConfigurationFile() {
        StringBuilder configurationJson = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("conf/backendURLs.json"))) {
            String currentLine;
            while ((currentLine = bufferedReader.readLine()) != null) {
                configurationJson.append(currentLine);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return configurationJson.toString();
    }

    /**
     * Parses configuration to two-dimensional String array, where each subarray contains
     * DBpedia, Freebase, Label Service 1, Label Service 2, Solr/enwiki
     *
     * @return Two-dimensional array with URLs
     */
    private List<BackendUrlGroup> parseConfiguration(String jsonString) {

        Gson gson = new GsonBuilder().create();

        // Could get it as list or proceed manually...
        // List<String> backends = gson.fromJson(jsonObject.get("offline"), new TypeToken<List<String>>(){}.getType());
        //JsonParser jsonParser  = new JsonParser();
        //JsonElement jsonElement = jsonParser.parse(jsonString);
        // JsonArray  jsonArray = jsonElement.isJsonArray()?jsonElement.getAsJsonArray():null;

        // ...but we can load it directly
        Type type = new TypeToken<List<BackendUrlGroup>>() {
        }.getType();
        return gson.fromJson(jsonString, type);
    }

    /**
     * Reads and parses configuration file
     *
     * @return Success state
     */
    private boolean updateUrlTable() {
        try {
            List<BackendUrlGroup> result = parseConfiguration(readConfigurationFile());
            if (result != null) {
                urlLookUpTable = result;
                return true;
            }
            return false;
        } catch (JsonParseException jpe) {
            jpe.printStackTrace();
            return false;
        }
    }

    /**
     * Looks up entire URL set for a backend (DBpedia, Freebase, Label Service 1, Label Service 2
     * and then Solr/enwiki)
     *
     * @param id Backend to return all URLs for
     * @return All backends in order
     */
    public BackendUrlGroup lookUpUrlGroup(int id) {
        if (urlLookUpTable != null && id < urlLookUpTable.size()) {
            return urlLookUpTable.get(id);
        }
        return null;
    }

    /**
     * Looks up particular URL in current backend, used by cz.brmlab.yodaqa.provider.rdf.FreebaseLookup,
     * cz.brmlab.yodaqa.provider.rdf.DBpediaTitles.java cz.brmlab.yodaqa.provider.rdf.DBpediaLookup and
     * cz.brmlab.yodaqa.pipeline.YodaQA
     *
     * @param urlkey: the UrlConstants constant string that maps to a system property or dict map key
     *                in the config file
     * @return URL for that particular backend
     */
    public String getUrl(String urlkey) {
        String overridingUrl = System.getProperty(urlkey);
        return overridingUrl == null ? currentBackend.getUrl(urlkey) : overridingUrl;
    }

}
