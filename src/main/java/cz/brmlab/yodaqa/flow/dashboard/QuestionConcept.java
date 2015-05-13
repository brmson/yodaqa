package cz.brmlab.yodaqa.flow.dashboard;

/** A question concept representation for QuestionSummary. */
public class QuestionConcept {
	String title;
	int pageId;
	public QuestionConcept(String title, int pageId) {
		this.title = title;
		this.pageId = pageId;
	}
	public String getTitle() { return title; }
	public int getPageId() { return pageId; }
};
