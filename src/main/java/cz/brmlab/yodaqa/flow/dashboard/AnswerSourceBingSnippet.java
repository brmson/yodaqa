package cz.brmlab.yodaqa.flow.dashboard;

/**
 * Created by honza on 16.7.15.
 */
public class AnswerSourceBingSnippet extends AnswerSource {
	private final String origin;

	public AnswerSourceBingSnippet(String title) {
		super("bing-snippet", title);
		this.origin = "fulltext";
	}
}
