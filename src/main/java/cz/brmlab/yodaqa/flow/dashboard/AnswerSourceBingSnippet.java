package cz.brmlab.yodaqa.flow.dashboard;

/**
 * Created by honza on 16.7.15.
 */
public class AnswerSourceBingSnippet extends AnswerSource {
	protected String URL;

	public AnswerSourceBingSnippet(String title, String url) {
		super("bing", "fulltext", title);
		this.URL = url;
	}
}
