package cz.brmlab.yodaqa.flow.dashboard.snippet;


/**
 * This class should generate unique ID's for Snippets.
 * The ID's are unique for the session
 * It is thread-safe
 */
public final class SnippetIDGenerator {
    /* Singleton */
    private int IDCounter = 0;
    private static SnippetIDGenerator instance = new SnippetIDGenerator();
    private SnippetIDGenerator() {}
    public static synchronized SnippetIDGenerator getInstance() {return instance;}
    public synchronized int generateID() {
        return IDCounter++;
    }
}
