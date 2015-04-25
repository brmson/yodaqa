package cz.brmlab.yodaqa.flow.dashboard;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/** A question tracking device.  This singleton class is used to keep
 * track of all questions posed and possibly answered so far, record
 * progress on answering them, and so on.  This is not useful for
 * the basic io modalities (like interactive or tsvgs), but gets
 * relevant for the web io where we can query the question answering
 * state asynchronously. */
/* N.B. this class must be thread-safe. */
public final class QuestionDashboard {
	/* Singleton. */
	private static QuestionDashboard qd = new QuestionDashboard();
	private QuestionDashboard() {}
	public static QuestionDashboard getInstance() {
		return qd;
	}

	/* All asked questions, by their id. */
	private Map<Integer, Question> questions = new HashMap<>();
	/* Questions that were not passed to the pipeline yet.
	 * .wait() and .notify() are used for signalization
	 * of any changes here. */
	/* XXX: This could be also a queue.  But we might want to do
	 * something smarter than FIFO in case of many questions, e.g.
	 * preferring questions from different remote hosts. */
	private Collection<Question> questionsToAnswer = new LinkedList<>();

	public synchronized void askQuestion(Question q) {
		questions.put(q.getId(), q);
		questionsToAnswer.add(q);
		notify();
	}

	/** Get a question to answer by the pipeline.  Blocks in case
	 * there isn't any right now. */
	public synchronized Question getQuestionToAnswer() {
		while (questionsToAnswer.isEmpty()) {
			try { wait(); } catch (InterruptedException e) { }
		}
		Question q = questionsToAnswer.iterator().next();
		questionsToAnswer.remove(q);
		return q;
	}

	public synchronized Question get(int id) {
		return questions.get(id);
	}
};
