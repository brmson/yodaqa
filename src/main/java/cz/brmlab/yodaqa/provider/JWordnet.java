package cz.brmlab.yodaqa.provider;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.dictionary.Dictionary;

/**
 * This class serves as a wrapper of the JWNL interface
 * of the JWordnet library.  We just initialize it and
 * keep around a single dictionary reference as a singleton.
 */

public final class JWordnet {
	private static final String PROPERTIES_RES = "wordnet.xml";

	protected static boolean initialized = false;

	protected static boolean initialize() throws Exception {
		if (initialized)
			return false;

		//JWNL.initialize(new FileInputStream(PROPERTIES_PATH));
		JWNL.initialize(JWordnet.class.getResourceAsStream(
			/* cz/brmlab/yodaqa/provider */
			PROPERTIES_RES));

		initialized = true;
		return true;
	}

	public static Dictionary getDictionary() throws Exception {
		initialize();
		return Dictionary.getInstance();
	}
};
