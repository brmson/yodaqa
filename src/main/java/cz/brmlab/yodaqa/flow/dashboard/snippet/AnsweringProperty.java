package cz.brmlab.yodaqa.flow.dashboard.snippet;

/**
 * This class represents a property snippet, it contains the text from a property
 */
public class AnsweringProperty extends AnsweringSnippet {
	protected String propertyLabel;

	public AnsweringProperty(int ID, int sourceID, String propertyLabel) {
		super(ID, sourceID);
		this.propertyLabel = propertyLabel;
	}

	public String getPropertyLabel(){
		return  propertyLabel;
	}

}
