package cz.brmlab.yodaqa.flow.dashboard;

/** An enwiki-based answer source (wiki document). */
public class AnswerSourceEnwiki extends AnswerSource {
	public static final String ORIGIN_FULL = "fulltext";
	public static final String ORIGIN_TITLE = "title-in-clue";
	public static final String ORIGIN_DOCUMENT = "document title";
	protected String origin;

	protected int pageId;
	protected boolean isConcept = false;

	public AnswerSourceEnwiki(String origin, String title, int pageId) {
		super("enwiki", title);
		this.origin = origin;
		this.pageId = pageId;
	}

	public int getPageId() { return pageId; }

	public boolean isConcept() { return isConcept; }
	public void setConcept(boolean isConcept) {
		this.isConcept = isConcept;
	}
};
