package cz.brmlab.yodaqa.analysis.passage.biotagger;

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

import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.SearchResult.AnswerBioMention;
import cz.brmlab.yodaqa.model.SearchResult.Passage;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Create AnswerMantion annotations based on gold standard answer
 * pattern matches, when training the tagger.  If the tagger is not
 * being trained, this annotator is simply not a part of the pipeline
 * at all. */

@SofaCapability(
	inputSofas = { "Question", "PickedPassages" },
	outputSofas = { "PickedPassages" }
)

public class GSAnsBioMention extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(GSAnsBioMention.class);

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

		QuestionInfo qi = JCasUtil.selectSingle(questionView, QuestionInfo.class);
		assert(qi.getAnswerPattern() != null); // training the CRF makes no sense otherwise

		for (Passage p : JCasUtil.select(passagesView, Passage.class))
			processPassage(passagesView, qi, p);
	}

	protected void processPassage(JCas passagesView, QuestionInfo qi, Passage p)
			throws AnalysisEngineProcessException {
		Pattern ap = Pattern.compile(qi.getAnswerPattern(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		Matcher m = ap.matcher(p.getCoveredText());

		while (m.find()) {
			AnswerBioMention abm = new AnswerBioMention(passagesView);
			abm.setBegin(p.getBegin() + m.start());
			abm.setEnd(p.getBegin() + m.end());
			abm.addToIndexes();
			/* An imperfect check against partial-word matches
			 * (e.g. /Japan/ matching Japanese).  Such matches
			 * would also crash CanByAnsBioMention. */
			if (JCasUtil.selectCovered(Token.class, abm).isEmpty())
				abm.removeFromIndexes();
		}
	}
}
