package cz.brmlab.yodaqa.flow.dashboard;

/**
 * An answer source created during structured search (freebase, dbpedia)
 */
public class AnswerSourceStructured extends AnswerSource {
	protected String URL;
	public static final String ORIGIN_STRUCTURED = "property";

	public AnswerSourceStructured(String origin, String URL, String title) {
		/* XXX: Instead of getType(), we should just get this as
		 * a parameter or have specific sub-classes (e.g. carry
		 * a mid for Freebase). */
		super(getType(URL), origin, title);
		this.URL = URL;
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
