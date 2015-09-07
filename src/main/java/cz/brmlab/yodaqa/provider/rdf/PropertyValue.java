package cz.brmlab.yodaqa.provider.rdf;

/** An (object, property, propres, value, valres, originFeat, score) tuple.
 * * object is its English string label
 * * property is an English phrase describing the value ("population",
 *   "area total", "country", "known for", ...)
 * * propres is a raw property id
 * * value is a string with some entity - name, quantity, ...
 * * valres is a resource IRI in case value is resource label
 * * originFeat is an AF indicating how was this property generated
 * * score is a numeric confidence value by some auxiliary mechanism */
public class PropertyValue {
	protected String object;
	protected String objRes;
	protected String property;
	protected String propRes; // ok to be unset
	protected String value;
	protected String valRes;

	String originFeat;
	String origin;  /* AnswerSourceStructured origin field. */

	protected Double score; // ok to be unset //////////specificscorexxxscore

	PropertyValue(String object_,
			String objRes_, String property_,
			String value_, String valRes_,
			String originFeat_,
			String origin_) {
		object = object_;
		objRes = objRes_;
		property = property_;
		propRes = null;
		value = value_;
		valRes = valRes_;
		originFeat = originFeat_;
		origin = origin_;
		score = null;
	}

	public String getObject() { return object; }
	public String getObjRes() { return objRes; }
	public String getProperty() { return property; }
	public String getValue() { return value; }
	public String getValRes() { return valRes; }
	public String getOriginFeat() { return originFeat; }
	public String getOrigin() { return origin; }

	public String getPropRes() { return propRes; }
	public void setPropRes(String propRes) { this.propRes = propRes; }

	public Double getScore() { return score; }
	public void setScore(double score) { this.score = score; }
}
