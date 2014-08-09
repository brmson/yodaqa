package cz.brmlab.yodaqa.analysis.question;

import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.TyCor.LAT;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NSUBJ;

/**
 * Generate LAT annotations in a QuestionCAS. These are words that should
 * be type-coercable to the answer term. E.g. "Who starred in Moon?" should
 * generate LATs "who", "actor", possibly "star".  Candidate answers will be
 * matched against LATs to acquire score.  Focus is typically always also an
 * LAT.
 *
 * Prospectively, we will want to add multiple diverse LAT annotators. This
 * one simply generates a single LAT from the Focus. */

public class LATByFocus extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATByFocus.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* A Focus is also an LAT. */
		for (Focus focus : JCasUtil.select(jcas, Focus.class)) {
			/* ...however, prefer an overlapping named entity. */
			if (!addNELAT(jcas, focus))
				addFocusLAT(jcas, focus);
		}

		/* TODO: Also derive an LAT from SV subject nominalization
		 * using wordnet. */
	}

	protected void addFocusLAT(JCas jcas, Focus focus) {
		String text = focus.getToken().getLemma().getValue().toLowerCase();
		double spec = 0.0;

		/* If focus is the question word, convert to an appropriate
		 * concept word or give up. */
		if (text.equals("who") || text.equals("whom")) {
			text = "person";
			spec--;
		} else if (text.equals("when")) {
			text = "time";
			spec--;
		} else if (text.equals("where")) {
			text = "location";
			spec--;

		} else if (text.matches("^what|why|how|which|name$")) {
			logger.info("?! Skipping focus LAT for ambiguous qlemma {}", text);
			return;
		}

		addLAT(jcas, focus.getBegin(), focus.getEnd(), focus, text, spec);
	}

	protected boolean addNELAT(JCas jcas, Focus focus) {
		boolean ne_found = false;
		for (NamedEntity ne : JCasUtil.selectCovering(NamedEntity.class, focus)) {
			ne_found = true;
			addLAT(jcas, ne.getBegin(), ne.getEnd(), ne, ne.getValue(), -2.0);
		}
		return ne_found;
	}

	protected void addLAT(JCas jcas, int begin, int end, Annotation base, String text, double spec) {
		LAT lat = new LAT(jcas);
		lat.setBegin(begin);
		lat.setEnd(end);
		lat.setBase(base);
		lat.setText(text);
		lat.setSpecificity(spec);
		lat.addToIndexes();
		logger.debug("new LAT by {}: <<{}>>", base.getType().getShortName(), text);
	}
}
