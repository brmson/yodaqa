package cz.brmlab.yodaqa.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by fp on 5/9/16.
 */
public class UrlManager {

    static {
        updateUrlTable();
    }

    public enum DataBackends {
        DBPEDIA, FREEBASE, LABEL1, LABEL2, SOLR
    }

    private static String[][] urlLookUpTable;
    //    {
//            {
//                    "http://dbpedia.ailao.eu:3030/dbpedia/query",
//                    "http://freebase.ailao.eu:3030/freebase/query",
//                    "http://dbp-labels.ailao.eu:5000",
//                    "http://dbp-labels.ailao.eu:5001",
//                    "http://enwiki.ailao.eu:8983/solr/"
//            },
//            {
//                    "http://127.0.0.1:3037/dbpedia/query",
//                    "http://127.0.0.1:3030/freebase/query",
//                    "http://127.0.0.1:5000",
//                    "http://127.0.0.1:5001",
//                    "http://127.0.0.1:8983/solr"
//            }
//    };
    public static String[][] getUrlLookUpTable() {
        return urlLookUpTable;
    }
    public static void setUrlLookUpTable(String[][] newLookUpTable) {
        urlLookUpTable = newLookUpTable;
    }

    private static int currentBackend = 0; //Default is 0: ailao.eu, 1: loopback, different if custom tables loaded
    public static int getCurrentBackend() {
        return currentBackend;
    }
    public static boolean setCurrentBackend(int currentBackend) {
        if (currentBackend < urlLookUpTable.length) {
            UrlManager.currentBackend = currentBackend;
            return true;
        } else {
            return false;
        }
    }

    public static String printState() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Currently selected backend: ");
        stringBuilder.append(Integer.toString(currentBackend));
        stringBuilder.append("\n\nCurrent Lookup Table:\n");
        for (int i=0; i<urlLookUpTable.length; i++){
            for(int j=0; j<urlLookUpTable[i].length; j++) {
                stringBuilder.append(urlLookUpTable[i][j]);
                stringBuilder.append(" ");
            }
            stringBuilder.append("\n");
        }
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
        stringBuilder.append(lookUpUrl(UrlManager.DataBackends.DBPEDIA.ordinal()));
        stringBuilder.append("\nFreebase: ");
        stringBuilder.append(lookUpUrl(UrlManager.DataBackends.FREEBASE.ordinal()));
        stringBuilder.append("\nLabel Service 1: ");
        stringBuilder.append(lookUpUrl(UrlManager.DataBackends.LABEL1.ordinal()));
        stringBuilder.append("\nLabel Service 2: ");
        stringBuilder.append(lookUpUrl(UrlManager.DataBackends.LABEL2.ordinal()));
        stringBuilder.append("\nSolr: ");
        stringBuilder.append(lookUpUrl(UrlManager.DataBackends.SOLR.ordinal()));
        return stringBuilder.toString();
    }

    /**
     * Reads configuration file
     * @return Raw content of configuration file (should be JSON)
     */
    private static String readConfigurationFile() {
        StringBuilder configurationJson = new StringBuilder();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("conf/backendURLs.json")))
        {
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
     * @return Two-dimensional array with URLs
     */
    private static String[][] parseConfiguration(String jsonString) {

        Gson gson = new GsonBuilder().create();

        // Could get it as list or proceed manually...
        // List<String> backends = gson.fromJson(jsonObject.get("offline"), new TypeToken<List<String>>(){}.getType());
        //JsonParser jsonParser  = new JsonParser();
        //JsonElement jsonElement = jsonParser.parse(jsonString);
        // JsonArray  jsonArray = jsonElement.isJsonArray()?jsonElement.getAsJsonArray():null;

        // ...but we can load it directly
        return gson.fromJson(jsonString, String[][].class);
    }

    /**
     * Reads and parses configuration file
     * @return Success state
     */
    public static boolean updateUrlTable() {
        try{
            String[][] result = parseConfiguration(readConfigurationFile());
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
     * @param id Backend to return all URLs for
     * @return All backends in order
     */
    public static String[] lookUpUrlSet(int id) {
        if(urlLookUpTable != null && id<urlLookUpTable.length) {
            return urlLookUpTable[id];
        }
        return null;
    }

    /**
     * Looks up particular URL in current backend, used by cz.brmlab.yodaqa.provider.rdf.FreebaseLookup,
     * cz.brmlab.yodaqa.provider.rdf.DBpediaTitles.java cz.brmlab.yodaqa.provider.rdf.DBpediaLookup and
     * cz.brmlab.yodaqa.pipeline.YodaQA
     * @param fieldId Field to lookup
     * @return URL for that particular backend
     */
    public static String lookUpUrl(int fieldId) {
        switch (fieldId) {
            case 0:
                return System.getProperty("cz.brmlab.yodaqa.dbpediaurl")!=null?
                        System.getProperty("cz.brmlab.yodaqa.dbpediaurl"):lookUpUrl(currentBackend, fieldId);
            case 1:
                return System.getProperty("cz.brmlab.yodaqa.freebaseurl")!=null?
                        System.getProperty("cz.brmlab.yodaqa.freebaseurl"):lookUpUrl(currentBackend, fieldId);
            case 2:
                return System.getProperty("cz.brmlab.yodaqa.label1url")!=null?
                        System.getProperty("cz.brmlab.yodaqa.label1url"):lookUpUrl(currentBackend, fieldId);
            case 3:
                return System.getProperty("cz.brmlab.yodaqa.label2url")!=null?
                        System.getProperty("cz.brmlab.yodaqa.label2url"):lookUpUrl(currentBackend, fieldId);
            case 4:
                return System.getProperty("cz.brmlab.yodaqa.solrurl")!=null?
                        System.getProperty("cz.brmlab.yodaqa.solrurl"):lookUpUrl(currentBackend, fieldId);
            default:
                return null;
        }
    }

    /**
     * Looks up URL by set and field
     * @param fieldId Field to lookup
     * @param setId URL set to use, number is position is JSON configuration under conf/backendURLs.json
     * @return URL for that particular set and field
     */
    public static String lookUpUrl(int setId, int fieldId) {
        if(urlLookUpTable != null && setId<urlLookUpTable.length &&
                fieldId<urlLookUpTable[setId].length) {
            return urlLookUpTable[setId][fieldId];
        }
        return null;
    }

}
