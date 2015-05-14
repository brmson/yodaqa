package cz.brmlab.yodaqa.provider.rdf;

/** An (object, property, value, valres) tuple.
 * * object is its English string label
 * * property is an English phrase describing the value ("population",
 *   "area total", "country", "known for", ...)
 * * value is a string with some entity - name, quantity, ...
 * * valres is a resource IRI in case value is resource label */
public class PropertyValue {
	protected String object;
	protected String property;
	protected String value;
	protected String valRes;

	PropertyValue(String object_, String property_, String value_, String valRes_) {
		object = object_;
		property = property_;
		value = value_;
		valRes = valRes_;
	}

	public String getObject() { return object; }
	public String getProperty() { return property; }
	public String getValue() { return value; }
	public String getValRes() { return valRes; }
}
