package cz.brmlab.yodaqa.analysis.passextract;

import java.util.Collection;
import java.util.Map;

import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.flow.dashboard.snippet.AnsweringPassage;
import cz.brmlab.yodaqa.flow.dashboard.snippet.SnippetIDGenerator;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.SearchResult.Passage;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Generate Passages from Sentences that contain some Clue in Question
 * and copy them over to the Passages view.
 *
 * Prospectively, we might want to keep some surrounding sentences,
 * though. */

@SofaCapability(
	inputSofas = { "Passages" },
	outputSofas = { "PickedPassages" }
)


public class PassFilter extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(PassFilter.class);

	/** Number of passages to pick for detailed analysis.
	 * Keep in sync with the data/ml/passextract-train.py num_picked
	 * variable. */
	public static final String PARAM_NUM_PICKED = "num-picked";
	@ConfigurationParameter(name = PARAM_NUM_PICKED, mandatory = false, defaultValue = "3")
	private int numPicked;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas resultView, passagesView, pickedPassagesView,questionView;
		try {
			resultView = jcas.getView("Result");
			passagesView = jcas.getView("Passages");
			jcas.createView("PickedPassages");
			questionView = jcas.getView("Question");
			pickedPassagesView = jcas.getView("PickedPassages");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}
		pickedPassagesView.setDocumentText(passagesView.getDocumentText());
		pickedPassagesView.setDocumentLanguage(passagesView.getDocumentLanguage());

		int sourceID = JCasUtil.selectSingle(resultView, ResultInfo.class).getSourceID();

		/* Pre-index covering info. */
		Map<Passage, Collection<Annotation>> covering =
			JCasUtil.indexCovered(passagesView, Passage.class, Annotation.class);

		/* Just copy over the first N ones. */
		FSIndex idx = passagesView.getJFSIndexRepository().getIndex("SortedPassages");
		FSIterator passages = idx.iterator();
		int i = 0;
		CasCopier copier = new CasCopier(passagesView.getCas(), pickedPassagesView.getCas());
		while (passages.hasNext() && i++ < numPicked) {
			Passage passage = (Passage) passages.next();
			AnsweringPassage ap = new AnsweringPassage(SnippetIDGenerator.getInstance().generateID(), sourceID, passage.getCoveredText());
			QuestionDashboard.getInstance().get(questionView).addSnippet(ap);

			Passage p2 = (Passage) copier.copyFs(passage);
			p2.setSnippetID(ap.getSnippetID());
			p2.addToIndexes();

			/* Also recursively copy annotations - we need
			 * to have Sentences in the PickedPassages view
			 * to run a parser. */
			for (Annotation a : covering.get(passage)) {
				if (!copier.alreadyCopied(a)) {
					Annotation a2 = (Annotation) copier.copyFs(a);
					a2.addToIndexes();
				}
			}

			/* Count tokens, just for a debug print.
			 * This is relevant because of StanfordParser
			 * MAX_TOKENS limit. */
			int n_tokens = JCasUtil.selectCovered(Token.class, passage).size();
			logger.debug(passage.getScore() + " | " + passage.getCoveredText() + " | " + n_tokens);
		}
	}
}
