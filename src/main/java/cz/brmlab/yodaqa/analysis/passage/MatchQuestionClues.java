package cz.brmlab.yodaqa.analysis.passage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.passextract.PassByClue;
import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.model.SearchResult.QuestionClueMatch;
import cz.brmlab.yodaqa.model.Question.Clue;

/**
 * Create QuestionClueMatch annotations within Passages that contain
 * a word that corresponds to some Clue in the Question view.
 *
 * This can be a helpful answer feature. */

@SofaCapability(
	inputSofas = { "Question", "PickedPassages" },
	outputSofas = { "PickedPassages" }
)

public class MatchQuestionClues extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(MatchQuestionClues.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, passagesView;
		try {
			questionView = jcas.getView("Question");
			passagesView = jcas.getView("PickedPassages");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		/* XXX: Do not generate for about-clues? */

		for (Passage p: JCasUtil.select(passagesView, Passage.class)) {
			for (Clue qclue : JCasUtil.select(questionView, Clue.class)) {
				Matcher m = Pattern.compile(PassByClue.getClueRegex(qclue, false)).matcher(p.getCoveredText());
				while (m.find()) {
					/* We have a match! */
					QuestionClueMatch qlc = new QuestionClueMatch(passagesView);
					qlc.setBegin(p.getBegin() + m.start());
					qlc.setEnd(p.getBegin() + m.end());
					qlc.setBaseClue(qclue);
					qlc.addToIndexes();
					logger.debug("GEN {} / {}", qclue.getClass().getSimpleName(), qlc.getCoveredText());
				}
			}
		}
	}
}
