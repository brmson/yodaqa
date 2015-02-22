package cz.brmlab.yodaqa.provider.rdf;

/** This is an abstract base class for accessing Freebase. */

public abstract class FreebaseLookup extends CachedJenaLookup {
	public FreebaseLookup() {
		super("http://[2001:718:2:1638:801f:a7ff:fe97:83ed]:3030/freebase/query",
			"PREFIX ns: <http://rdf.freebase.com/ns/>\n" +
			"");
	}
}
