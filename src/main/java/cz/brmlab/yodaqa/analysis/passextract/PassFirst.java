package cz.brmlab.yodaqa.analysis.passextract;

import java.lang.Math;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.Passage;

/**
 * Generate Passage from the first Sentence of the document.
 * In Wikipedia, this sentence often summarizes the topic. */

@SofaCapability(
	inputSofas = { "Question", "Result" },
	outputSofas = { "Passages", "PickedPassages" }
)


public class PassFirst extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(PassFirst.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, resultView, passagesView;
		try {
			questionView = jcas.getView("Question");
			resultView = jcas.getView("Result");
			passagesView = jcas.getView("Passages");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		/* We just do the first sentence. */

		CasCopier copier = new CasCopier(resultView.getCas(), passagesView.getCas());
		Sentence sentence = null;
		for (Sentence s2 : JCasUtil.select(resultView, Sentence.class)) {
			sentence = s2;
			break;
		}
		if (sentence == null)
			return;

		int numClues = JCasUtil.select(questionView, Clue.class).size();
		double weight = numClues; // proportional to #clues since so is PassByClue

		/* Generate features. */
		AnswerFV afv = new AnswerFV();
		afv.setFeature(AF.OriginPsg, 1.0);
		afv.setFeature(AF.OriginPsgFirst, 1.0);

		/* Annotate */
		Passage passage = new Passage(passagesView);
		passage.setBegin(sentence.getBegin());
		passage.setEnd(sentence.getEnd());
		passage.setScore(Math.sqrt(weight));
		passage.setAnsfeatures(afv.toFSArray(passagesView));
		passage.addToIndexes();

		logger.debug(passage.getScore() + " | " + passage.getCoveredText());

		/* Copy */
		Sentence s2 = (Sentence) copier.copyFs(sentence);
		s2.addToIndexes();

		for (Annotation a : JCasUtil.selectCovered(Annotation.class, sentence)) {
			if (copier.alreadyCopied(a))
				continue;
			Annotation a2 = (Annotation) copier.copyFs(a);
			a2.addToIndexes();
		}
	}
}
