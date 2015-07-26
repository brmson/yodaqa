package cz.brmlab.yodaqa.flow.dashboard;

/** An ABC for a possible source of answers for a given question. */
public abstract class AnswerSource {
	/** The AnswerSource type denotes the kind of the AnswerSource:
	 * whether it's a database, fulltext corpus, web search...
	 * This may be "enwiki", "bing", ...  Ideally, each AnswerSource
	 * subclass should correspond to a single type field value. */
	protected String type;
	/** The strategy used to obtain the AnswerSource from the knowledge base.
	 * E.g. "fulltext", "property", ... */
	protected String origin;

	protected String title;
	protected int sourceID;
	protected String URL;

	/* state 0: not processed, 1: being processed, 2: done */
	protected int state = 0;

	public AnswerSource(String type, String origin, String title, String url) {
		this.type = type;
		this.origin = origin;
		this.title = title;
		this.URL = url;
	}

	public String getType() { return type; }
	public String getOrigin() { return origin; }
	public String getTitle() { return title; }

	public int getState() { return state; }
	public void setState(int state) {
		this.state = state;
	}
	public int getSourceID() { return sourceID; }
	public void setSourceID(int sourceID) {
		/* XXX: This should really be a read-only constructor time
		 * parameter, or even obtained internally within the class. */
		this.sourceID = sourceID;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AnswerSource that = (AnswerSource) o;

		if (!type.equals(that.type)) return false;
		if (!origin.equals(that.origin)) return false;
		if (!title.equals(that.title)) return false;
		return URL.equals(that.URL);

	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + origin.hashCode();
		result = 31 * result + title.hashCode();
		result = 31 * result + URL.hashCode();
		return result;
	}
}
