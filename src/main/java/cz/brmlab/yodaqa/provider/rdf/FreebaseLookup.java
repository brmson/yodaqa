package cz.brmlab.yodaqa.provider.rdf;

/** This is an abstract base class for accessing Freebase. */

public abstract class FreebaseLookup extends CachedJenaLookup {
	public FreebaseLookup() {
		super("http://147.32.85.33:3030/freebase/query",
			"PREFIX ns: <http://rdf.freebase.com/ns/>\n" +
			"");
	}
}
