package cz.brmlab.yodaqa.provider.solr;

public class SolrQuerySettings {
	protected int proximityNum;
	protected int proximityBaseDist, proximityBaseFactor;
	protected String[] searchFields;

	/* searchField "" means document body; searchField "titleText"
	 * means the title. */
	public SolrQuerySettings(int proximityNum_, int proximityBaseDist_, int proximityBaseFactor_, String[] searchFields_) {
		proximityNum = proximityNum_;
		proximityBaseDist = proximityBaseDist_;
		proximityBaseFactor = proximityBaseFactor_;
		searchFields = searchFields_;
	}

	/**
	 * @return the proximityNum
	 */
	public int getProximityNum() {
		return proximityNum;
	}

	/**
	 * @return the proximityBaseDist
	 */
	public int getProximityBaseDist() {
		return proximityBaseDist;
	}

	/**
	 * @return the proximityBaseFactor
	 */
	public int getProximityBaseFactor() {
		return proximityBaseFactor;
	}

	/**
	 * @return the searchFields
	 */
	public String[] getSearchFields() {
		return searchFields;
	}

	public String[] getSearchPrefixes() {
		String[] prefixes = new String[searchFields.length];
		for (int i = 0; i < searchFields.length; i++) {
			String field = searchFields[i];
			prefixes[i] = field != "" ? field + ":" : "";
		}
		return prefixes;
	}
}
