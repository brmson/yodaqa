package cz.brmlab.yodaqa.provider;

import java.util.HashMap;
import java.util.Map;

import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpNameFinder;

import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/** OpenNlpNameFinder with synchronized processing.  It turns out
 * that opennlp uses thread-unsafe caching, so we need to make
 * sure only one instance runs at once.  (It is no big deal as
 * NE tagging is a quick process.)
 *
 * XXX: This essentially uses the namefinder as an UIMA annotator
 * delegate.  I couldn't find a proper example of doing this, so
 * the particulars are a bit hacky. */
public class SyncOpenNlpNameFinder extends JCasAnnotator_ImplBase {
	/**
	 * Variant of a model the model. Used to address a specific model if
	 * there are multiple models for one language.
	 */
	public static final String PARAM_VARIANT = ComponentParameters.PARAM_VARIANT;
	@ConfigurationParameter(name = PARAM_VARIANT, mandatory = true, defaultValue="person")
	protected String variant;

	static Map<String, Object> namefinder_locks = new HashMap<>();
	protected OpenNlpNameFinder nf;

	@Override
	public void initialize(UimaContext aContext)
		throws ResourceInitializationException
	{
		Object nflock = null;
		synchronized (namefinder_locks) {
			if (!namefinder_locks.containsKey(variant)) {
				nflock = new Object();
				namefinder_locks.put(variant, nflock);
			} else {
				nflock = namefinder_locks.get(variant);
			}
		}

		synchronized (nflock) {
			/* XXX: This is rather horrid, but the
			 * overconvoluted resource instantiation
			 * and configuration parameter framework
			 * of UIMA is kinda beyond me. */
			nf = new OpenNlpNameFinder();
			try {
				FieldUtils.writeField(nf, "variant", variant, true);
			} catch (Exception e) {
				throw new ResourceInitializationException(e);
			}
			nf.initialize(aContext);
		}
	}

	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
		Object nflock;
		synchronized (namefinder_locks) {
			nflock = namefinder_locks.get(variant);
		}

		synchronized (nflock) {
			nf.process(cas);
		}
	}

	@Override
	public void destroy() {
		Object nflock;
		synchronized (namefinder_locks) {
			nflock = namefinder_locks.get(variant);
		}

		synchronized (nflock) {
			nf.destroy();
		}
	}
};
