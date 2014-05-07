package cz.brmlab.yodaqa.analysis.answer;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.TyCor.LAT;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Generate LAT annotations in a CandidateAnswerCAS. This is based on the
 * answer focus and the result LAT texts should be compatible with Question.LAT
 * but the process of their generation might be different in details. */

public class LATByFocus extends JCasAnnotator_ImplBase {
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
	}

	protected void addFocusLAT(JCas jcas, Focus focus) {
		/* Convert focus to its lemma. */
		Token ftok = focus.getToken();
		String text = ftok.getLemma().getValue();
		double spec = 0.0;

		/* Focus may be a number... */
		if (ftok.getPos().getPosValue().matches("^CD")) {
			text = "quantity";
			spec -= 2;
		}

		addLAT(jcas, focus.getBegin(), focus.getEnd(), focus, text, spec);
	}

	protected boolean addNELAT(JCas jcas, Focus focus) {
		boolean ne_found = false;
		for (NamedEntity ne : JCasUtil.selectCovering(NamedEntity.class, focus)) {
			ne_found = true;
			/* Positive specificity is a bit of a cheat;
			 * we use it to boost named entities score
			 * since named entities are awesome answers.
			 * But XXX: do this in a more consistent way! */
			addLAT(jcas, ne.getBegin(), ne.getEnd(), ne, ne.getValue(), 1.0);
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
	}
}
