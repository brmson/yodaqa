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

	/* state 0: not processed, 1: being processed, 2: done */
	protected int state = 0;

	public AnswerSource(String type, String origin, String title) {
		this.type = type;
		this.origin = origin;
		this.title = title;
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
};
