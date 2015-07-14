package cz.brmlab.yodaqa.provider.rdf;

import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;

/** An (object, property, propres, value, valres, originFeat, score) tuple.
 * * object is its English string label
 * * property is an English phrase describing the value ("population",
 *   "area total", "country", "known for", ...)
 * * propres is a raw property id
 * * value is a string with some entity - name, quantity, ...
 * * valres is a resource IRI in case value is resource label
 * * originFeat is an AF class indicating how was this property generated
 *   (XXX: this is technically a package layering violation; might be
 *    (mostly) solved as we move to string features)
 * * score is a numeric confidence value by some auxiliary mechanism */
public class PropertyValue {
	protected String object;
	protected String property;
	protected String propRes; // ok to be unset
	protected String value;
	protected String valRes;
	Class<? extends AnswerFeature> originFeat;
	protected Double score; // ok to be unset

	PropertyValue(String object_, String property_,
			String value_, String valRes_,
			Class<? extends AnswerFeature> originFeat_) {
		object = object_;
		property = property_;
		propRes = null;
		value = value_;
		valRes = valRes_;
		originFeat = originFeat_;
		score = null;
	}

	public String getObject() { return object; }
	public String getProperty() { return property; }
	public String getValue() { return value; }
	public String getValRes() { return valRes; }
	public Class<? extends AnswerFeature> getOriginFeat() { return originFeat; }

	public String getPropRes() { return propRes; }
	public void setPropRes(String propRes) { this.propRes = propRes; }

	public Double getScore() { return score; }
	public void setScore(double score) { this.score = score; }
}
