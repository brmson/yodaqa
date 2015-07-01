package cz.brmlab.yodaqa.provider.rdf;

import com.hp.hpl.jena.rdf.model.Literal;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class saves already found results in a thread safe HashMap.
 * The key is the query expression used to retrieve it, the value is the retrieved list
 */
public class RDFCache {

    private ConcurrentHashMap<String, List<Literal[]>> cache = new ConcurrentHashMap<>();

    public boolean contains(String query){
        return cache.containsKey(query);
    }

    public List<Literal[]> retrieve(String query){
        return cache.get(query);
    }

    public void add(String query, List<Literal[]> results){
        cache.put(query,results);
    }
}
