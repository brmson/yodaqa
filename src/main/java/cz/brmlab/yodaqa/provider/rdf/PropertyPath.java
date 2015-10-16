package cz.brmlab.yodaqa.provider.rdf;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** RDF property path.  A sequence of properties that form a graph path,
 * where the properties are in the RDF format (dot.separated names).
 * E.g.:
 *   * tv.tv_program.tv_producer, tv.tv_producer_term.producer
 *   * type.object.name
 */
public class PropertyPath {
	protected List<String> properties;

	public PropertyPath(List<String> properties) {
		this.properties = properties;
	}

	/** Import property path from Freebase format.  There, we use
	 * slashes instead of dots (incl. a leading slash) and pipes
	 * to separate properties. */
	public static PropertyPath fromFreebasePath(String fbPath) {
		List<String> path = new ArrayList<>();
		for (String prop : fbPath.split("\\|")) {
			if (prop.length() == 0) continue;
			String rdfProp = prop.substring(1).replaceAll("/", "."); /* /x/y -> x.y */
			path.add(rdfProp);
		}
		return new PropertyPath(path);
	}

	public List<String> getProperties() { return properties; }
	/** Get the i-th property in the path. */
	public String get(int i) { return properties.get(i); }
	/** Get the path length. */
	public int size() { return properties.size(); }

	public boolean equals(PropertyPath p2) {
		if (this.properties.size() != p2.properties.size())
			return false;
		for (int i = 0; i < this.properties.size(); i++)
			if (!this.properties.get(i).equals(p2.properties.get(i)))
				return false;
		return true;
	}

	@Override public int hashCode() {
		return Objects.hash(properties.toArray());
	}
	@Override public String toString() {
		return properties.toString();
	}
}
