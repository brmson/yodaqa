package cz.brmlab.yodaqa.flow.dashboard.snippet;

/**
 * This class represents snippet, which contains only the document title
 */
public class AnsweringDocTitle extends AnsweringSnippet {
	public static final String type = "AnsweringDocumentTitle";
	public AnsweringDocTitle(int ID, int sourceID) {
		super(ID, sourceID);
	}

	public String toString(){
		return super.getSnippetID() + " DocumentTitle";
	}
}

