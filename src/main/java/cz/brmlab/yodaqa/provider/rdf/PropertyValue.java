package cz.brmlab.yodaqa.provider.rdf;

/** An (object, property, value) tuple, where object is its
 * English string label, property is an English phrase
 * describing the value ("population", "area total",
 * "country", "known for", ...) and value is a string with
 * some entity - name, quantity, ... */
public class PropertyValue {
	protected String object;
	protected String property;
	protected String value;

	PropertyValue(String object_, String property_, String value_) {
		object = object_;
		property = property_;
		value = value_;
	}

	public String getObject() { return object; }
	public String getProperty() { return property; }
	public String getValue() { return value; }
}
