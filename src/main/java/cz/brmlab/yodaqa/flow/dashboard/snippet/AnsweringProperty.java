package cz.brmlab.yodaqa.flow.dashboard.snippet;

import cz.brmlab.yodaqa.provider.rdf.PropertyValue;

/**
 * This JSON-exported class represents a property snippet that contains
 * the property label.
 */
public class AnsweringProperty extends AnsweringSnippet {
	protected String propertyLabel;
	protected String witnessLabel;
	public static final String type = "AnsweringProperty";

	public AnsweringProperty(int ID, int sourceID, String propertyLabel, String witnessLabel) {
		super(ID, sourceID);
		this.propertyLabel = propertyLabel;
		this.witnessLabel = witnessLabel;
	}

	public AnsweringProperty(int ID, int sourceID, PropertyValue pv) {
		this(ID, sourceID, pv.getProperty(), pv.getWitness());
	}

	@Override
	public String toString(){
		return "Property " + super.getSnippetID() + " " + propertyLabel
			+ (witnessLabel != null ? " (" + witnessLabel + ")" : "");
	}

	public String getPropertyLabel() { return propertyLabel; }
	public String getWitnessLabel() { return witnessLabel; }
}
