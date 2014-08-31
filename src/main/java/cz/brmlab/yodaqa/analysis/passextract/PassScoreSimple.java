package cz.brmlab.yodaqa.analysis.passextract;

import java.util.LinkedList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.SearchResult.PF_ClueWeight;
import cz.brmlab.yodaqa.model.SearchResult.PF_AboutClueWeight;
import cz.brmlab.yodaqa.model.SearchResult.Passage;

/**
 * Annotate Passages view "Passage" objects with score based on the associated
 * PassageFeatures.  This particular implementation contains an extremely
 * simple ad hoc score computation with fixed weights. */


public class PassScoreSimple extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(PassScoreSimple.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas passagesView) throws AnalysisEngineProcessException {
		List<Passage> passages = new LinkedList<Passage>();

		for (Passage passage : JCasUtil.select(passagesView, Passage.class)) {
			PassageFV fv = new PassageFV(passage);

			int clueWeight_i = PassageFV.featureIndex(PF_ClueWeight.class);
			int aboutClueWeight_i = PassageFV.featureIndex(PF_AboutClueWeight.class);
			assert(clueWeight_i >= 0 && aboutClueWeight_i >= 0);

			double score = fv.getValues()[clueWeight_i] + 0.25 * fv.getValues()[aboutClueWeight_i];
			passage.setScore(score);
			passages.add(passage);
		}

		/* Reindex the touched passages. */
		for (Passage passage : passages) {
			passage.removeFromIndexes();
			passage.addToIndexes();
		}
	}
}
