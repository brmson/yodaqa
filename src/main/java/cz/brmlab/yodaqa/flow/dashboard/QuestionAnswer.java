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
	protected String source;
	protected List<Integer> passageIDs= new ArrayList<>();
	public QuestionAnswer(String text, double confidence) {
		this.text = text;
		this.confidence = confidence;
	}
	public List<Integer> getPassageIDList() {return passageIDs;}
	public void addToPassageList(int passageID){
		passageIDs.add(passageID);
	}
	public List<Integer> getPassageList(){
		return passageIDs;
	}
	/** @return the text */
	public String getText() { return text;}
	/** @return the confidence */
	public double getConfidence() { return confidence; }
	public void setID(int ID) {this.ID = ID;}
	public int getID(){return ID;}
	public void setSource(String source_){this.source=source_;}
}
