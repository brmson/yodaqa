package cz.brmlab.yodaqa.provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Property file located in src/main/resources/cz/brmlab/yodaqa/provider/addresses.property
 */
public class PrivateResources {
	private static PrivateResources instance = new PrivateResources();
	private Properties props;

	private PrivateResources() {
		props = new Properties();
		try {
			InputStream is = PrivateResources.class.getResourceAsStream("addresses.property");
			if (is == null)
				return;
			props.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static PrivateResources getInstance() {
		return instance;
	}

	public String getResource(String key) {
		return (String)props.get(key);
	}
}
