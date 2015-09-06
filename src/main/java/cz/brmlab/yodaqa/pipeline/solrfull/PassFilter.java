package cz.brmlab.yodaqa.pipeline.solrfull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import cz.brmlab.yodaqa.flow.asb.MultiThreadASB;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.flow.dashboard.snippet.AnsweringPassage;
import cz.brmlab.yodaqa.flow.dashboard.snippet.SnippetIDGenerator;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasMultiplier_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
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
 * A CAS merger-multiplier that collects SearchResultCASes, keeps all
 * passages, and converts the top N to PassageCASes.
 *
 * Prospectively, we might want to keep also some surrounding sentences,
 * though. */

public class PassFilter extends JCasMultiplier_ImplBase {
	final Logger logger = LoggerFactory.getLogger(PassFilter.class);

	/** Number of passages to pick for detailed analysis.
	 * Keep in sync with the data/ml/passextract-train.py num_picked
	 * variable. */
	public static final String PARAM_NUM_PICKED = "num-picked";
	@ConfigurationParameter(name = PARAM_NUM_PICKED, mandatory = false, defaultValue = "36")
	private int numPicked;

	/** Number of CASes marked as isLast required to encounter before
	 * the final merging is performed.  When multiple independent CAS
	 * multipliers are generating CASes, they each eventually produce
	 * one with an isLast marker. */
	public static final String PARAM_ISLAST_BARRIER = "islast-barrier";
	@ConfigurationParameter(name = PARAM_ISLAST_BARRIER, mandatory = false, defaultValue = "1")
	protected int isLastBarrier;

	/* Tracking stats to find out at what point did we acquire all CASes
	 * to merge. */
	/* #of "last CAS" seen; this counts towards the isLastBarrier */
	int isLast;
	/* #of total CASes seen, and CASes we need to see.  This is because
	 * with asynchronous CAS flow, the last generated CAS (marked with
	 * isLast) is not the last received CAS. */
	int seenCases, needCases;
	/* Number of produced PassageCases. */
	int producedCases;

	/* An "intermediate CAS" where gathered passage annotations are
	 * hoarded.  Each search result is stored in a different view,
	 * which seems to be the only way to amass and carry over
	 * annotations. */
	JCas iCas;

	protected class PassageInfo {
		public ResultInfo ri;
		public JCas iView;
		public Passage psg;

		public PassageInfo(ResultInfo ri, JCas iView, Passage psg) {
			this.ri = ri;
			this.iView = iView;
			this.psg = psg;
		}

		public ResultInfo getRi() { return ri; }
		public JCas getIView() { return iView; }
		public Passage getPsg() { return psg; }
	};

	protected List<PassageInfo> passages;

	public synchronized void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		try {
			reset();
		} catch (AnalysisEngineProcessException e) {
			throw new ResourceInitializationException(e);
		}
	}

	protected void reset() throws AnalysisEngineProcessException {
		passages = new ArrayList<>();
		isLast = 0;
		seenCases = 0;
		needCases = 0;
		producedCases = 0;

		if (iCas != null) {
			iCas.release();
			iCas = null;
		}
	}

	public synchronized void process(JCas jcas) throws AnalysisEngineProcessException {
		/* "Load" the input CAS. */
		JCas questionView, resultView, passagesView;
		try {
			questionView = jcas.getView("Question");
			resultView = jcas.getView("Result");
			passagesView = jcas.getView("Passages");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		ResultInfo ri = JCasUtil.selectSingle(resultView, ResultInfo.class);
		seenCases++;
		if (ri.getIsLast() > 0) {
			isLast++;
			needCases += ri.getIsLast();
		}
		logger.debug("in: <<{}>> (seen {}, need {}, last {})", ri.getDocumentTitle(), seenCases, needCases, isLast);

		/* Setup the intermediate CAS. */
		JCas iResultView;
		try {
			if (seenCases == 1) {
				/* Set things up. */
				iCas = getEmptyJCas();
				JCas iQuestionView = iCas.createView("Question");
				copyQuestion(questionView, iQuestionView);
			}
			iResultView = iCas.createView("SearchResult" + seenCases);
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
		CasCopier copier = new CasCopier(jcas.getCas(), iCas.getCas());
		copier.copyCasView(passagesView.getCas(), iResultView.getCas(), true);
		ResultInfo ri2 = copyResultInfo(new CasCopier(resultView.getCas(), iResultView.getCas()), ri, ri.getIsLast());

		for (Passage psg : JCasUtil.select(iResultView, Passage.class)) {
			PassageInfo pi = new PassageInfo(ri2, iResultView, psg);
			passages.add(pi);
		}
		if (JCasUtil.select(iResultView, Passage.class).isEmpty()) {
			/* Ok, add a dummy result so that we carry this through
			 * and all the isLast counts add up. */
			passages.add(new PassageInfo(ri2, iResultView, null));
		}

		if (isFilled()) {
			/* That's all CASes in for now. */
			QuestionInfo qi = JCasUtil.selectSingle(questionView, QuestionInfo.class);
			processAllPassages(qi);
		}
	}

	protected void processAllPassages(QuestionInfo qi) {
		/* Sort by score, from highest, and keep just top N. */
		/* XXX: Deduplicate? We might have a single passage
		 * coming in esp. by different searches generating
		 * the same search result... */
		Collections.sort(passages, new Comparator<PassageInfo>(){ @Override
			public int compare(PassageInfo pi1, PassageInfo pi2){
				return Double.valueOf(pi2.getPsg().getScore()).compareTo(Double.valueOf(pi1.getPsg().getScore()));
			} } );
		if (passages.size() > numPicked)
			passages = passages.subList(0, numPicked);

		/* Record statistics on picked passages. */
		recordStatistics(qi, passages);
	}

	/** Return true when we had eaten all CASes we should. */
	protected boolean isFilled() {
		return isLast >= isLastBarrier && seenCases >= needCases;
	}

	/** Return true when we have some CASes to produce. */
	protected boolean canProduce() {
		return !passages.isEmpty() || (isFilled() && producedCases == 0);
	}

	public synchronized boolean hasNext() throws AnalysisEngineProcessException {
		return isFilled() && canProduce();
	}

	public synchronized AbstractCas next() throws AnalysisEngineProcessException {
		PassageInfo pi = passages.isEmpty() ? null : passages.remove(0);
		producedCases++;

		JCas iQuestionView;
		try {
			iQuestionView = iCas.getView("Question");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		JCas jcas = getEmptyJCas();
		try {
			JCas pQuestionView = jcas.createView("Question");
			copyQuestion(iQuestionView, pQuestionView);

			JCas pPassageView = jcas.createView("Passage");
			int pIsLast = canProduce() ? 0 : producedCases;
			setupPassage(iQuestionView, pPassageView, pi, pIsLast);
		} catch (Exception e) {
			jcas.release();
			throw new AnalysisEngineProcessException(e);
		}

		if (!canProduce()) {
			/* That's all CASes to produce now. */
			reset();
		}

		return jcas;
	}

	protected void setupPassage(JCas questionView, JCas outPassageView, PassageInfo pi, int pIsLast) throws Exception {
		if (pi.getPsg() == null) {
			/* Produce just a dummy passage. */
			outPassageView.setDocumentText("");
			outPassageView.setDocumentLanguage(pi.getIView().getDocumentLanguage());
			CasCopier copier = new CasCopier(pi.getIView().getCas(), outPassageView.getCas());
			copyResultInfo(copier, pi.getRi(), pIsLast);
			logger.debug("dummy : isLast " + pIsLast);
			return;
		}

		/* Dashboard setup. */
		int sourceID = pi.getRi().getSourceID();
		AnsweringPassage ap = new AnsweringPassage(SnippetIDGenerator.getInstance().generateID(), sourceID, pi.getPsg().getCoveredText());
		QuestionDashboard.getInstance().get(questionView).addSnippet(ap);

		/* Init the Passage view. */
		outPassageView.setDocumentText(pi.getPsg().getCoveredText());
		outPassageView.setDocumentLanguage(pi.getIView().getDocumentLanguage());

		/* Copy over the Passage and stuff. */
		CasCopier copier = new CasCopier(pi.getIView().getCas(), outPassageView.getCas());
		copyResultInfo(copier, pi.getRi(), pIsLast);
		Passage p2 = copyShiftPassage(copier, pi.getPsg(), 0);

		/* Count tokens, just for a debug print.
		 * This is relevant because of StanfordParser
		 * MAX_TOKENS limit. */
		int n_tokens = JCasUtil.selectCovered(Token.class, p2).size();
		logger.debug(p2.getScore() + " | " + p2.getCoveredText() + " | " + n_tokens + " : isLast " + pIsLast);
	}

	protected void copyQuestion(JCas src, JCas dest) {
		CasCopier copier = new CasCopier(src.getCas(), dest.getCas());
		copier.copyCasView(src.getCas(), dest.getCas(), true);
	}

	protected ResultInfo copyResultInfo(CasCopier copier, ResultInfo ri, int pIsLast) {
		ResultInfo ri2 = (ResultInfo) copier.copyFs(ri);
		ri2.setIsLast(pIsLast);
		ri2.addToIndexes();
		return ri2;
	}

	protected Passage copyShiftPassage(CasCopier copier, Passage psg, int dst_ofs) {
		int src_ofs = psg.getBegin();
		Passage p2 = (Passage) copier.copyFs(psg);
		p2.setBegin(p2.getBegin() - src_ofs + dst_ofs);
		p2.setEnd(p2.getEnd() - src_ofs + dst_ofs);
		p2.addToIndexes();

		/* Also recursively copy annotations - we need
		 * to have Sentences in the Passage view
		 * to run a parser. */
		for (Annotation a : JCasUtil.selectCovered(Annotation.class, psg)) {
			Annotation a2 = (Annotation) copier.copyFs(a);
			a2.setBegin(a2.getBegin() - src_ofs + dst_ofs);
			a2.setEnd(a2.getEnd() - src_ofs + dst_ofs);
			a2.addToIndexes();
		}

		return p2;
	}

	protected void recordStatistics(QuestionInfo qi, List<PassageInfo> passages) {
		Pattern apat = null;
		if (qi.getAnswerPattern() != null)
			apat = Pattern.compile(qi.getAnswerPattern(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

		int n_picked = 0, n_gspicked = 0;
		for (PassageInfo pi : passages) {
			n_picked += 1;
			if (apat != null && apat.matcher(pi.getPsg().getCoveredText()).find())
				n_gspicked += 1;
		}

		qi.removeFromIndexes();
		qi.setPassE_picked(qi.getPassE_picked() + n_picked);
		qi.setPassE_gspicked(qi.getPassE_gspicked() + n_gspicked);
		qi.addToIndexes();
	}

	@Override
	public int getCasInstancesRequired() {
		return MultiThreadASB.maxJobs * 3;
	}
}
