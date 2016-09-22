package cz.brmlab.yodaqa.provider.rdf;

import cz.brmlab.yodaqa.provider.UrlManager;

/** This is an abstract base class for accessing Freebase. */

public abstract class FreebaseLookup extends CachedJenaLookup {
	public FreebaseLookup() {
		super(UrlManager.lookUpUrl(UrlManager.DataBackends.FREEBASE.ordinal()),
			"PREFIX ns: <http://rdf.freebase.com/ns/>\n" +
			"");
	}
}
