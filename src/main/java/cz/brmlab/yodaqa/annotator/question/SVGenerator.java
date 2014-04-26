package cz.brmlab.yodaqa.annotator.question;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.ROOT;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NSUBJ;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.Question.SV;

/**
 * Selective Verb annotations in a QuestionCAS. These represent the
 * coordinating verb of the question that "selects" the answer with regard to
 * other clues. E.g.  "Who has received the Nobel Prize for Physiology and
 * Medicine?" will have "received" as SV; "When were they born?" will have
 * "born"; "How many colors do you need to color a planar graph?" will have
 * "need".  SV is one of the primary clues but is found in a special way and
 * might (or might not) be used specially in answer selection.
 *
 * Prospectively, we will want to add multiple diverse SV annotators,
 * especially for dealing with corner cases. This one is based on Constituent
 * annotations. */

public class SVGenerator extends JCasAnnotator_ImplBase {
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		for (ROOT sentence : JCasUtil.select(jcas, ROOT.class)) {
			processSentence(jcas, sentence);
		}
	}

	public void processSentence(JCas jcas, Constituent sentence) throws AnalysisEngineProcessException {
		Annotation focus = null;
		for (Focus f : JCasUtil.selectCovered(Focus.class, sentence)) {
			focus = f.getBase();
			break;
		}

		if (focus == null) {
			/* No Focus means no SV. */
			return;
		}
		if (focus.getTypeIndexID() != NSUBJ.type) {
			/* NN Focus also means no SV. */
			return;
		}
		NSUBJ nsubj = (NSUBJ) focus;
		Token v = nsubj.getGovernor();

		/* Ok, we believe this verb is the Selecting Verb. */
		SV sv = new SV(jcas);
		sv.setBegin(v.getBegin());
		sv.setEnd(v.getEnd());
		sv.setBase(v);
		sv.addToIndexes();
	}
}
