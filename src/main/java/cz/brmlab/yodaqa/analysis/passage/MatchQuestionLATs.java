package cz.brmlab.yodaqa.analysis.passage;

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

import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.model.SearchResult.QuestionLATMatch;
import cz.brmlab.yodaqa.model.TyCor.LAT;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Create QuestionLATMatch annotations within Passages that contain
 * a word that corresponds to some LAT in the Question view.
 *
 * This can help during type coercion of answers generated from
 * that Passage, nearby this annotation. */

@SofaCapability(
	inputSofas = { "Question", "PickedPassages" },
	outputSofas = { "PickedPassages" }
)

public class MatchQuestionLATs extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(MatchQuestionLATs.class);

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

		for (Passage p: JCasUtil.select(passagesView, Passage.class)) {
			Token bestT = null;
			LAT bestQlat = null;

			for (LAT qlat : JCasUtil.select(questionView, LAT.class)) {
				String text = qlat.getText().toLowerCase();
				String textalt = qlat.getCoveredText().toLowerCase();

				for (Token t : JCasUtil.selectCovered(Token.class, p)) {
					String toktext = t.getLemma().getValue().toLowerCase();
					String toktextalt = t.getCoveredText().toLowerCase();
					// logger.debug("LAT " + text + textalt + " tok " + toktext + toktextalt);
					if (!toktext.equals(text) && !toktext.equals(textalt) && !toktextalt.equals(text) && !toktextalt.equals(textalt))
						continue;
					/* We have a match! But keep just the
					 * most specific LAT match within the
					 * passage. */
					if (bestQlat != null && bestQlat.getSpecificity() > qlat.getSpecificity())
						continue;
					bestT = t;
					bestQlat = qlat;
				}
			}

			if (bestT != null) {
				QuestionLATMatch qlm = new QuestionLATMatch(passagesView);
				qlm.setBegin(bestT.getBegin());
				qlm.setEnd(bestT.getEnd());
				qlm.setBaseToken(bestT);
				qlm.setBaseLAT(bestQlat);
				qlm.addToIndexes();
				// logger.debug("GEN " + qlm.getCoveredText());
			}
		}
	}
}
