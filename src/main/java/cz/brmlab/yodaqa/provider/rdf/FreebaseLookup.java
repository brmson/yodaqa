package cz.brmlab.yodaqa.provider.rdf;

/** This is an abstract base class for accessing Freebase. */

public abstract class FreebaseLookup extends CachedJenaLookup {
	public FreebaseLookup() {
		super("http://freebase.ailao.eu:8890/sparql",
			"PREFIX ns: <http://rdf.basekb.com/ns/>\n" +
			"");
	}
}
