package cz.brmlab.yodaqa.flow.dashboard;

/**
 * Created by nhl on 7/10/15.
 */
public class AnswerSourceStructured extends AnswerSource {
	protected String URL;
	protected String origin;
	public AnswerSourceStructured(String origin, String URL, String title) {
		super("structured", title);
		this.URL=URL;
		this.origin=origin;
	}
}
