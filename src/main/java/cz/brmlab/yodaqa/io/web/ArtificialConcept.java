package cz.brmlab.yodaqa.io.web;

/**
 * Created by Petr Marek on 22.10.2015.
 */
public class ArtificialConcept {
	int pageID;
	String fullLabel;

	public ArtificialConcept(int pageID, String fullLabel) {
		this.pageID = pageID;
		this.fullLabel = fullLabel;
	}

	public int getPageID() {
		return pageID;
	}

	public String getFullLabel() {
		return fullLabel;
	}
}
