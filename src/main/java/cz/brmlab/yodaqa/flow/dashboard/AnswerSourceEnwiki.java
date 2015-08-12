package cz.brmlab.yodaqa.flow.dashboard;

/** An enwiki-based answer source (wiki document). */
public class AnswerSourceEnwiki extends AnswerSource {
	/* Keep the following in sync with doc/REST-API.md */

	public static final String ORIGIN_FULL = "fulltext";
	public static final String ORIGIN_TITLE = "title-in-clue";
	public static final String ORIGIN_DOCUMENT = "document title";

	protected int pageId;
	protected boolean isConcept = false;

	public AnswerSourceEnwiki(String origin, String title, int pageId) {
		super("enwiki", origin, title, "http://en.wikipedia.org/?curid="+pageId);
		this.pageId = pageId;
	}

	public int getPageId() { return pageId; }

	public boolean isConcept() { return isConcept; }
	public void setConcept(boolean isConcept) {
		this.isConcept = isConcept;
	}
};
