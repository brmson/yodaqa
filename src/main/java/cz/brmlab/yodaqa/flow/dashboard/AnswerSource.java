package cz.brmlab.yodaqa.flow.dashboard;

/** An ABC for a possible source of answers for a given question. */
public abstract class AnswerSource {
	protected String type;
	protected String title;

	public AnswerSource(String type, String title) {
		this.type = type;
		this.title = title;
	}

	public String getType() { return type; }
	public String getTitle() { return title; }
};
