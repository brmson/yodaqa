package cz.brmlab.yodaqa.flow.dashboard.snippet;

/**
 * This class represents a passage snippet, it contains the text from a passage
 */
public class AnsweringPassage extends AnsweringSnippet {
	protected String passageText;
	public static final String type = "AnsweringPassage";
	public AnsweringPassage(int ID, int sourceID, String passageText) {
		super(ID, sourceID);
		this.passageText = passageText;
	}

	@Override
	public String toString() {
		return "Passage "+ super.getSnippetID()+" "+ passageText;
	}

}
