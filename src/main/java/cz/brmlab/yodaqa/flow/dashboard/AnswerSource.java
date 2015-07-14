package cz.brmlab.yodaqa.flow.dashboard;

/** An ABC for a possible source of answers for a given question. */
public abstract class AnswerSource {
	protected String type;
	protected String title;
	protected int sourceID;

	/* state 0: not processed, 1: being processed, 2: done */
	protected int state = 0;

	public AnswerSource(String type, String title) {
		this.type = type;
		this.title = title;
	}

	public String getType() { return type; }
	public String getTitle() { return title; }

	public int getState() { return state; }
	public void setState(int state) {
		this.state = state;
	}
	public int getSourceID() { return sourceID; }
	public void setSourceID(int sourceID) { this.sourceID = sourceID; }
};
