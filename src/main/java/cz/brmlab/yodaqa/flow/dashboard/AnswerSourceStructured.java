package cz.brmlab.yodaqa.flow.dashboard;

/**
 * An answer source created during structured search (freebase, dbpedia)
 */
public class AnswerSourceStructured extends AnswerSource {
	protected String URL;
	protected String origin;
	public static final String ORIGIN_STRUCTURED = "property";

	public AnswerSourceStructured(String origin, String URL, String title) {
		super(getType(URL), title);
		this.URL = URL;
		this.origin = origin;
	}

	public static String getType(String URL) {
		if (URL.contains("freebase")) {
			return "freebase";
		} else if (URL.contains("dbpedia")) {
			return "dbpedia";
		} else {
			return "unknown";
		}
	}
}
