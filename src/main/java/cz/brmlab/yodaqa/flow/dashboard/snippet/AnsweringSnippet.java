package cz.brmlab.yodaqa.flow.dashboard.snippet;

/**
 * This abstract class contains it's own ID and the ID of its source
 */
public abstract class AnsweringSnippet {
	protected int snippetID;
	protected int sourceID;

	public AnsweringSnippet(int ID, int sourceID) {
		this.snippetID = ID;
		this.sourceID = sourceID;
	}
	public int getSnippetID() { return snippetID; }
	public int getSourceID() { return sourceID; }

}
