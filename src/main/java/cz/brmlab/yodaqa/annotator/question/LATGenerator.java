package cz.brmlab.yodaqa.annotator.question;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.Question.LAT;

/**
 * Generate LAT annotations in a QuestionCAS. These are words that should
 * be type-coercable to the answer term. E.g. "Who starred in Moon?" should
 * generate LATs "who", "actor", possibly "star".  Candidate answers will be
 * matched against LATs to acquire score.  Focus is typically always also an
 * LAT.
 *
 * Prospectively, we will want to add multiple diverse LAT annotators. This
 * one simply generates a single LAT from the Focus. */

public class LATGenerator extends JCasAnnotator_ImplBase {
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* A Focus is also an LAT. */
		for (Focus focus : JCasUtil.select(jcas, Focus.class))
			addLAT(jcas, focus.getBegin(), focus.getEnd(), focus);

		/* TODO: Also derive an LAT from SV subject nominalization. */

		/* TODO: Also derive an LAT from interesting pieces of the
		 * WH* constituent. */
	}

	protected void addLAT(JCas jcas, int begin, int end, Annotation base) {
		LAT lat = new LAT(jcas);
		lat.setBegin(begin);
		lat.setEnd(end);
		lat.setBase(base);
		lat.addToIndexes();
	}
}
