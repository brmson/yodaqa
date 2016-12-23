package cz.brmlab.yodaqa.provider.url;

/**
 * Created by bzhao on 12/23/16.
 * URL related constants, these are used as:
 *  - keys in the json dict map of backendUrls.conf
 *  - system property overrides from CLI,
 *  eg: -Dcz.brmlab.yodaqa.dbpediaurl=http://yodaqa.felk.cvut.cz/fuseki-dbp/dbpedia/query
 */
public class UrlConstants {
    public final static String DBPEDIA = "cz.brmlab.yodaqa.dbpediaurl";
    public final static String FREEBASE = "cz.brmlab.yodaqa.freebaseurl";
    public final static String DBPEDIA_LABEL = "cz.brmlab.yodaqa.dbpedialabelurl";
    public final static String DICTIONARY_LABEL = "cz.brmlab.yodaqa.dictionarylabelurl";
    public final static String SOLR = "cz.brmlab.yodaqa.solrurl";
}
