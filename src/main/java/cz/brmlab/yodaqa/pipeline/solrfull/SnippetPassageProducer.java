package cz.brmlab.yodaqa.pipeline.solrfull;

import java.util.Iterator;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.component.JCasMultiplier_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_ResultLogScore;
import cz.brmlab.yodaqa.model.Question.Snippet;
import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;

/**
 * From the QuestionCAS, generate a bunch of PassageCAS
 * instances.  In this case, we generate one per Snippet
 * (provided on input already). */

public class SnippetPassageProducer extends JCasMultiplier_ImplBase {
	final Logger logger = LoggerFactory.getLogger(SnippetPassageProducer.class);

	protected JCas questionView;
	protected Iterator<Snippet> snippetIter;
	protected int i;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		questionView = jcas;
		snippetIter = JCasUtil.select(questionView, Snippet.class).iterator();
		i = 0;
	}


	@Override
	public boolean hasNext() throws AnalysisEngineProcessException {
		return snippetIter.hasNext() || i == 0;
	}

	@Override
	public AbstractCas next() throws AnalysisEngineProcessException {
		Snippet snippet = snippetIter.hasNext() ? snippetIter.next() : null;

		JCas jcas = getEmptyJCas();
		try {
			jcas.createView("Question");
			JCas canQuestionView = jcas.getView("Question");
			copyQuestion(questionView, canQuestionView);

			jcas.createView("Result");
			JCas resultView = jcas.getView("Result");
			resultView.setDocumentText(snippet != null ? snippet.getText() : "");
			resultView.setDocumentLanguage(questionView.getDocumentLanguage());
			AnswerFV afv_r = new AnswerFV();
			afv_r.setFeature(AF_ResultLogScore.class, Math.log(2));
			ResultInfo ri = new ResultInfo(resultView);
			ri.setDocumentId(snippet != null ? snippet.getDocument() : "");
			ri.setSource("snippets");
			ri.setRelevance(1);
			ri.setAnsfeatures(afv_r.toFSArray(resultView));
			ri.setIsLast(!snippetIter.hasNext());
			ri.addToIndexes();

			jcas.createView("PickedPassages");
			JCas passagesView = jcas.getView("PickedPassages");
			passagesView.setDocumentText(resultView.getDocumentText());
			passagesView.setDocumentLanguage(resultView.getDocumentLanguage());
			AnswerFV afv_p = new AnswerFV();
			Passage p = new Passage(passagesView);
			p.setBegin(0);
			p.setEnd(passagesView.getDocumentText().length());
			p.setScore(1);
			p.setAnsfeatures(afv_p.toFSArray(passagesView));
			p.addToIndexes();

		} catch (Exception e) {
			jcas.release();
			throw new AnalysisEngineProcessException(e);
		}
		i++;
		return jcas;
	}

	protected void copyQuestion(JCas src, JCas dest) throws Exception {
		CasCopier copier = new CasCopier(src.getCas(), dest.getCas());
		copier.copyCasView(src.getCas(), dest.getCas(), true);
	}
}
