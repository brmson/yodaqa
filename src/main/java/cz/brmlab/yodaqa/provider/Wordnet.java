package cz.brmlab.yodaqa.provider;

import org.apache.uima.resource.ResourceInitializationException;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.dictionary.Dictionary;

/** A singleton that provides a shared dictionary instance. */
public class Wordnet {
	/* Singleton. */
	private Wordnet() {}

	private static Dictionary dictionary = null;
	public synchronized static Dictionary getDictionary() throws ResourceInitializationException {
		if (dictionary == null) {
			try {
				dictionary = Dictionary.getDefaultResourceInstance();
			} catch (JWNLException e) {
				throw new ResourceInitializationException(e);
			}
		}
		return dictionary;
	}
}
