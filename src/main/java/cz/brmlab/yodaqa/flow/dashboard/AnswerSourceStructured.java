package cz.brmlab.yodaqa.flow.dashboard;

/**
 * An answer source created during structured search (freebase, dbpedia)
 */
public class AnswerSourceStructured extends AnswerSource {
	protected String URL;
	protected String origin;
	public AnswerSourceStructured(String origin, String URL, String title) {
		super("structured", title);
		this.URL = URL;
		this.origin = origin;
	}
}
