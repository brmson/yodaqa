package cz.brmlab.yodaqa.flow.dashboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** A question answer, consumer-ready.  This is a random answer to a question,
 * ready to be published. */
public class QuestionAnswer {
	protected String text;
	protected double confidence;
	protected int ID;
	protected List<Integer> snippetIDs= new ArrayList<>();
	public QuestionAnswer(String text, double confidence, int ID) {
		this.text = text;
		this.confidence = confidence;
		this.ID = ID;
	}
	public List<Integer> getSnippetIDs() {return snippetIDs;}
	public void addToSnippetIDList(int snippetID){
		snippetIDs.add(snippetID);
	}
 	/** @return the text */
	public String getText() { return text;}
	/** @return the confidence */
	public double getConfidence() { return confidence; }
	public int getID() { return ID; }
}
