package cz.brmlab.yodaqa.flow.dashboard;

/**
 * This class should generate unique ID's for sources.
 * The ID's are unique for the session
 * It is thread-safe */

public final class SourceIDGenerator {
	/* Singleton */
	private int IDCounter = 0;
	private static SourceIDGenerator instance = new SourceIDGenerator();
	private SourceIDGenerator() {}
	public static synchronized SourceIDGenerator getInstance() {return instance;}
	public synchronized int generateID() {
		return IDCounter++;
	}
}

