package cz.brmlab.yodaqa.provider.rdf;

import cz.brmlab.yodaqa.provider.url.UrlConstants;
import cz.brmlab.yodaqa.provider.url.UrlManager;

/** This is an abstract base class for accessing Freebase. */

public abstract class FreebaseLookup extends CachedJenaLookup {
	public FreebaseLookup() {
		super(UrlManager.getInstance().getUrl(UrlConstants.FREEBASE),
			"PREFIX ns: <http://rdf.freebase.com/ns/>\n" +
			"");
	}
}
