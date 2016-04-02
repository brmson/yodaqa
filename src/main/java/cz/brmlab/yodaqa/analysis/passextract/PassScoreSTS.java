package cz.brmlab.yodaqa.analysis.passextract;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.provider.STSScoring;

/**
 * Annotate Passages view "Passage" objects with score based on an external
 * STS scorer.  The PassageFeatures generated earlier are actually ignored
 * during this scoring. */


public class PassScoreSTS extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(PassScoreSimple.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	protected class PassScore {
		Passage passage;
		double score;

		public PassScore(Passage passage_, double score_) {
			passage = passage_;
			score = score_;
		}
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, passagesView;
		try {
			questionView = jcas.getView("Question");
			passagesView = jcas.getView("Passages");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		List<String> ptexts = new ArrayList<String>();
		for (Passage passage : JCasUtil.select(passagesView, Passage.class)) {
			ptexts.add(passage.getCoveredText());
		}

		List<PassScore> passages = new ArrayList<PassScore>();
		List<Double> scores = STSScoring.getScores(questionView.getDocumentText(), ptexts);
		Iterator<Double> scoreIt = scores.iterator();
		Iterator<Passage> psgIt = JCasUtil.select(passagesView, Passage.class).iterator();
		while (scoreIt.hasNext() && psgIt.hasNext()) {
			double score = scoreIt.next();
			Passage passage = psgIt.next();
			score = 1. / (1. + Math.exp(-score)); // sigmoid
			passages.add(new PassScore(passage, score));
		}

		/* Reindex the touched passages. */
		for (PassScore ps : passages) {
			ps.passage.removeFromIndexes();
			ps.passage.setScore(ps.score);
			ps.passage.addToIndexes();
		}
	}
}
