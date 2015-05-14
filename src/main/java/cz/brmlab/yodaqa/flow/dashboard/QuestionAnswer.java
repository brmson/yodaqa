package cz.brmlab.yodaqa.flow.dashboard;

/** A question answer, consumer-ready.  This is a random answer to a question,
 * ready to be published. */
public class QuestionAnswer {
	protected String text;
	protected double confidence;

	public QuestionAnswer(String text, double confidence) {
		this.text = text;
		this.confidence = confidence;
	}

	/** @return the text */
	public String getText() { return text; }
	/** @return the confidence */
	public double getConfidence() { return confidence; }
};
