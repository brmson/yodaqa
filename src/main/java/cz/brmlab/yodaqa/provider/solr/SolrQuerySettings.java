package cz.brmlab.yodaqa.provider.solr;

public class SolrQuerySettings {
	protected int proximityNum;
	protected double proximityBaseDist, proximityBaseFactor;
	protected String[] searchFields;
	protected boolean cluesAllRequired;
	protected boolean proximityOnly;

	/* searchField "" means document body; searchField "titleText"
	 * means the title. */
	public SolrQuerySettings(int proximityNum_, double proximityBaseDist_, double proximityBaseFactor_, String[] searchFields_,
			boolean cluesAllRequired_) {
		proximityNum = proximityNum_;
		proximityBaseDist = proximityBaseDist_;
		proximityBaseFactor = proximityBaseFactor_;
		searchFields = searchFields_;
		cluesAllRequired = cluesAllRequired_;
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
	public double getProximityBaseDist() {
		return proximityBaseDist;
	}

	/**
	 * @return the proximityBaseFactor
	 */
	public double getProximityBaseFactor() {
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

	/**
	 * @return the cluesAllRequired
	 */
	public boolean areCluesAllRequired() {
		return cluesAllRequired;
	}

	/**
	 * @return the proximityOnly
	 */
	public boolean isProximityOnly() {
		return proximityOnly;
	}

	/**
	 * @param proximityOnly the proximityOnly to set
	 */
	public void setProximityOnly(boolean proximityOnly) {
		this.proximityOnly = proximityOnly;
	}
}
