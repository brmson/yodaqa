package cz.brmlab.yodaqa.flow.dashboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import cz.brmlab.yodaqa.flow.dashboard.snippet.AnsweringProperty;
import cz.brmlab.yodaqa.flow.dashboard.snippet.AnsweringSnippet;

/** A stateful question.  This question has been asked, can be referred to
 * and may or may not have been answered already.
 *
 * Important: Multiple threads can look and poke at a Question at once.
 * Be careful, always deal with it only through synchronized methods. */
public class Question {
	protected String id;
	protected String text;
	protected QuestionSummary summary = null;
	protected HashMap<Integer, AnswerSource> sources = new HashMap<>();
	protected List<QuestionAnswer> answers = new ArrayList<>();
	protected Map<Integer, AnsweringSnippet> snippets = new HashMap<>(); //key = ID of snippet, value = the actual snippet
	protected boolean finished = false;
	protected List<QuestionConcept> artificialConcepts = new ArrayList<>();
	protected boolean hasOnlyArtificialConcept=false;
	/* Generation counts for various fields above, incremented every
	 * time they are modified. */
	protected int gen_sources = 0;
	protected int gen_answers = 0;

	protected static Gson gson = new Gson();

	public Question(String id, String text) {
		this.id = id;
		this.text = text;
	}

	public Question(String id, String text, List<QuestionConcept> artificialConcepts, boolean hasOnlyArtificialConcept) {
		this.id = id;
		this.text = text;
		this.artificialConcepts = artificialConcepts;
		this.hasOnlyArtificialConcept=hasOnlyArtificialConcept;
	}

	/** @return the id */
	public synchronized String getId() { return id; }
	/** @return the text */
	public synchronized String getText() { return text; }
	/** @return manually added concepts */
	public synchronized List<QuestionConcept> getArtificialConcepts(){ return artificialConcepts; }
	/** @return if question is using artificial Concepts only */
	public synchronized boolean getHasOnlyArtificialConcept(){ return hasOnlyArtificialConcept; }

	/** @return the summary */
	public synchronized QuestionSummary getSummary() { return summary; }
	/** @param summary the summary to set */
	public synchronized void setSummary(QuestionSummary summary) {
		this.summary = summary;
	}

	public synchronized void addSnippet(AnsweringSnippet snippet) {
		snippets.put(snippet.getSnippetID(),snippet);
	}

	public synchronized void setSourceState(int sourceID, int state) {
		sources.get(sourceID).setState(state);
		gen_sources++;
	}

	/** @return the answer */
	public synchronized List<QuestionAnswer> getAnswers() { return answers; }
	public synchronized void addAnswer(QuestionAnswer qa) {
		/* Do trivial text-based deduplication. */
		QuestionAnswer dupe = null;
		for (QuestionAnswer qa2 : answers) {
			if (qa2.getText().equals(qa.getText())) {
				dupe = qa2;
				break;
			}
		}
		if (dupe != null)
			answers.remove(dupe);

		// pre-pend this answer, so that the latest are at the top
		this.answers.add(0, qa);
		gen_answers ++;
	}

	/** Stores and deduplicates unique AnswerSource and returns its ID. */
	public synchronized int storeAnswerSource(AnswerSource as) {
		gen_sources++;
		for (Map.Entry<Integer, AnswerSource> savedSource : sources.entrySet()) {
			if (savedSource.getValue().equals(as)) {
				as.setSourceID(savedSource.getValue().getSourceID());
				return savedSource.getKey();
			}
		}
		int sourceID = SourceIDGenerator.getInstance().generateID();
		as.setSourceID(sourceID);
		sources.put(sourceID, as);
		return sourceID;
	}
	/** Reset the answer list, typically when have scored them.
	 * @param answer the answer to set */
	public synchronized void setAnswers(List<QuestionAnswer> answers) {
		this.answers = answers;
		gen_answers ++;
	}

	/** @return the finished */
	public synchronized boolean isFinished() { return finished; }
	/** @param finished the finished to set */
	protected synchronized void setFinished(boolean finished) {
		this.finished = finished;
	}

	public synchronized String toJson() {
		JsonElement j = gson.toJsonTree(this);
		String answerSentence = this.getAnswerSentence();
		if (answerSentence != null)
			j.getAsJsonObject().addProperty("answerSentence", answerSentence);
		return gson.toJson(j);
	}

	/** Autogenerate a full sentence describing the answer.
	 * This is designed mainly for answers coming from databases,
	 * though a baseline version for fulltext answers shouldn't be
	 * that difficult either. */
	public synchronized String getAnswerSentence() {
		try {
			// top answer
			QuestionAnswer a0 = this.answers.get(0);

			// pick the bottom snippet (to skip awards; XXX,
			// sort them by something tangible), but
			// preferring a witness-carrying snippet
			List<Integer> revSIDs = Lists.reverse(a0.getSnippetIDs());
			AnsweringSnippet s0 = this.snippets.get(revSIDs.get(0));
			for (Integer wsid : revSIDs) {
				AnsweringSnippet sw = this.snippets.get(wsid);
				if (sw instanceof AnsweringProperty
				    && ((AnsweringProperty) sw).getWitnessLabel() != null) {
					s0 = sw;
					break;
				}
			}
			AnswerSource src0 = this.sources.get(s0.getSourceID());

			if (s0 instanceof AnsweringProperty) {
				AnsweringProperty ap0 = (AnsweringProperty) s0;
				boolean showIsBeforeProperty = ap0.getPropertyLabel().toLowerCase().endsWith(" by");

				StringBuilder sb = new StringBuilder();
				sb.append(src0.getTitle());
				sb.append(" ");
				if (showIsBeforeProperty)
					sb.append("is ");
				sb.append(ap0.getPropertyLabel().replaceAll(".*: ", "").toLowerCase());
				sb.append(" ");
				if (ap0.getWitnessLabel() != null) {
					sb.append("(for ");
					sb.append(ap0.getWitnessLabel());
					sb.append(") ");
				}
				if (!showIsBeforeProperty)
					sb.append("is ");
				sb.append(a0.getText());
				sb.append(".");
				return sb.toString();
			} else {
				// TODO
				return null;
			}

		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
}
