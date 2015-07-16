package cz.brmlab.yodaqa.flow.dashboard;

/**
 * This class should generate unique ID's for answer candidates
 * The ID's are unique for the session
 * It is thread-safe
 */
public final class AnswerIDGenerator {
    /* Singleton */
	private int IDCounter = 0;
    private static AnswerIDGenerator instance = new AnswerIDGenerator();
    private AnswerIDGenerator() {}
    public static synchronized AnswerIDGenerator getInstance() {return instance;}
    public synchronized int generateID() {
        return IDCounter++;
    }
}
