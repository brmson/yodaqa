package cz.brmlab.yodaqa.flow.dashboard;

import java.util.List;

/** A question analysis summary, consumer-ready.  This carries interesting
 * facts about the question, like LATs and concept clues. */
public class QuestionSummary {
	protected List<String> lats;
	protected List<QuestionConcept> concepts;

	public QuestionSummary(List<String> lats, List<QuestionConcept> concepts) {
		this.lats = lats;
		this.concepts = concepts;
	}

	/** @return the lats */
	public List<String> getLats() { return lats; }
	/** @return the concepts */
	public List<QuestionConcept> getConcepts() { return concepts; }
};
