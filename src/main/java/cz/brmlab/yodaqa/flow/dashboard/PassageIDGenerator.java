package cz.brmlab.yodaqa.flow.dashboard;


/**
 * This class should generate unique ID's for Passages
 * The ID's are unique for the session
 * It is thread-safe
 */
public final class PassageIDGenerator {
    /*Singleton*/
    volatile int IDCounter = 0;
    private static PassageIDGenerator instance = new PassageIDGenerator();
    private PassageIDGenerator() {}
    public static synchronized PassageIDGenerator getInstance() {return instance;}
    public synchronized int generateID() {
        return IDCounter++;
    }
}
