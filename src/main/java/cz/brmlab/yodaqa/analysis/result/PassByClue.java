package cz.brmlab.yodaqa.analysis.result;

import java.util.Collection;
import java.util.Map;
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

import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.Passage;

/**
 * Generate Passages from Sentences that contain some Clue in Question
 * and copy them over to the Passages view.
 *
 * Prospectively, we might want to keep some surrounding sentences,
 * though. */

@SofaCapability(
	inputSofas = { "Question", "Result" },
	outputSofas = { "Passages" }
)


public class PassByClue extends JCasAnnotator_ImplBase {
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, resultView, passagesView;
		try {
			questionView = jcas.getView("Question");
			resultView = jcas.getView("Result");
			jcas.createView("Passages");
			passagesView = jcas.getView("Passages");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}
		passagesView.setDocumentText(resultView.getDocumentText());
		passagesView.setDocumentLanguage(resultView.getDocumentLanguage());
		int totalLength = resultView.getDocumentText().length();

		/* Pre-index covering info. */
		Map<Sentence, Collection<Annotation>> covering =
			JCasUtil.indexCovered(resultView, Sentence.class, Annotation.class);

		/* Walk sentences, match them and copy what we like. */
		/* XXX: Ideally, we would match all clues in parallel,
		 * but for now we opt for a trivial O(M*N) approach. */
		CasCopier copier = new CasCopier(resultView.getCas(), passagesView.getCas());
		for (Sentence sentence : JCasUtil.select(resultView, Sentence.class)) {
			/* TODO: Rate clues themselves. */
			/* TODO: Put clues in a hierarchy so that we don't
			 * try to match word clues of phrase clues we already
			 * matched. */
			int matches = 0;
			for (Clue clue : JCasUtil.select(questionView, Clue.class)) {
				/* Match */
				if (!sentence.getCoveredText().contains(clue.getCoveredText()))
					continue;

				matches += clue.getCoveredText().length(); // XXX: rather #ofwords?
			}

			if (matches > 0) {
				/* Annotate. */
				Passage passage = new Passage(passagesView);
				passage.setBegin(sentence.getBegin());
				passage.setEnd(sentence.getEnd());

				/* Score slowly raises with number of matched
				 * clues (TODO this, the sqrt() is completely
				 * arbitrary, and we may overemphasize long
				 * clues here), and goes down (by at most 1.0)
				 * with the offset in the document (more
				 * important things come first). */
				double score = Math.sqrt(matches);
				score -= Math.pow((double) sentence.getBegin() / totalLength, 2);
				passage.setScore(score);

				passage.addToIndexes();

				/* Copy */
				if (!copier.alreadyCopied(sentence)) {
					Sentence s2 = (Sentence) copier.copyFs(sentence);
					s2.addToIndexes();

					for (Annotation a : covering.get(sentence)) {
						if (!copier.alreadyCopied(a)) {
							Annotation a2 = (Annotation) copier.copyFs(a);
							a2.addToIndexes();
						}
					}
				}
			}
		}
	}
}
