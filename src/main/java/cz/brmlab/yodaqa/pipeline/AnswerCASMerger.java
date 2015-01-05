package cz.brmlab.yodaqa.pipeline;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.component.JCasMultiplier_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginDBpRelation;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginDocTitle;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginMultiple;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginPsgFirst;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginPsgNE;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginPsgNP;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginPsgNPByLATSubj;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_Phase0Score;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_Phase1Score;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerFeature;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;

/**
 * Take a set of per-answer CandidateAnswerCAS and merge them to
 * an AnswerHitlistCAS.
 *
 * We also deduplicate answers with identical text.
 *
 * Otherwise, do not confuse with AnswerTextMerger, which merges answers
 * in the AnswerHitlist that are textually different but heuristically
 * equivalent. */

public class AnswerCASMerger extends JCasMultiplier_ImplBase {
	final Logger logger = LoggerFactory.getLogger(AnswerCASMerger.class);

	/** Number of CASes marked as isLast required to encounter before
	 * the final merging is performed.  When multiple independent CAS
	 * multipliers are generating CASes, they each eventually produce
	 * one with an isLast marker. */
	public static final String PARAM_ISLAST_BARRIER = "islast-barrier";
	@ConfigurationParameter(name = PARAM_ISLAST_BARRIER, mandatory = false, defaultValue = "1")
	protected int isLastBarrier;

	/** Reuse the first CAS received as the AnswerHitlistCAS instead
	 * of building one from scratch. This parameter is also overloaded
	 * to mean that CandidateAnswerCAS will override same-text answers
	 * in the hitlist, instead of merging with them. */
	public static final String PARAM_HITLIST_REUSE = "hitlist-reuse";
	@ConfigurationParameter(name = PARAM_HITLIST_REUSE, mandatory = false, defaultValue = "false")
	protected boolean doReuseHitlist;

	/** The phase number. If non-zero, confidence of the answer is
	 * pre-set to AF_Phase(n-1)Score. */
	public static final String PARAM_PHASE = "phase";
	@ConfigurationParameter(name = PARAM_PHASE, mandatory = false, defaultValue = "0")
	protected int phaseNum;

	protected class AnswerFeatures {
		Answer answer;
		AnswerFV fv;

		public AnswerFeatures(Answer answer_, AnswerFV fv_) {
			answer = answer_;
			fv = fv_;
		}

		/** * @return the answer */
		public Answer getAnswer() { return answer; }
		/** * @return the fv */
		public AnswerFV getFV() { return fv; }
	}

	Map<String, List<AnswerFeatures>> answersByText;
	JCas finalCas, finalQuestionView, finalAnswerHitlistView;
	boolean isFirst;
	int isLast;

	protected void reset() {
		answersByText = new HashMap<String, List<AnswerFeatures>>();
		finalCas = null;
		isFirst = true;
		isLast = 0;
	}

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		reset();
	}

	public void process(JCas canCas) throws AnalysisEngineProcessException {
		if (doReuseHitlist && isFirst) {
			/* AnswerHitlist initialized, reset list of answers
			 * and bail out for now. */
			isFirst = false;

			finalCas = getEmptyJCas();
			CasCopier.copyCas(canCas.getCas(), finalCas.getCas(), true);
			try {
				finalQuestionView = finalCas.getView("Question");
				finalAnswerHitlistView = finalCas.getView("AnswerHitlist");
			} catch (Exception e) {
				throw new AnalysisEngineProcessException(e);
			}

			for (Answer answer : JCasUtil.select(finalAnswerHitlistView, Answer.class)) {
				String text = answer.getText();
				List<AnswerFeatures> answers = answersByText.get(text);
				if (answers == null) {
					answers = new LinkedList<AnswerFeatures>();
					answersByText.put(text, answers);
				}
				answers.add(new AnswerFeatures(answer, new AnswerFV(answer)));
			}

			for (Entry<String, List<AnswerFeatures>> entry : answersByText.entrySet()) {
				for (AnswerFeatures afs : entry.getValue()) {
					for (FeatureStructure fs : afs.getAnswer().getFeatures().toArray()) {
						AnswerFeature af = (AnswerFeature) fs;
						af.removeFromIndexes();
					}
					afs.getAnswer().removeFromIndexes();
				}
			}
			return;
		}

		JCas canQuestion, canAnswer;
		try {
			canQuestion = canCas.getView("Question");
			canAnswer = canCas.getView("Answer");
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

		if (finalCas == null) {
			finalCas = getEmptyJCas();
			try {
				finalQuestionView = finalCas.createView("Question");
				finalAnswerHitlistView = finalCas.createView("AnswerHitlist");
			} catch (Exception e) {
				throw new AnalysisEngineProcessException(e);
			}
		}

		if (isFirst) {
			/* Copy QuestionInfo */
			CasCopier copier = new CasCopier(canQuestion.getCas(), finalQuestionView.getCas());
			copier.copyCasView(canQuestion.getCas(), finalQuestionView.getCas(), true);
			isFirst = false;
		}

		AnswerInfo ai = JCasUtil.selectSingle(canAnswer, AnswerInfo.class);
		ResultInfo ri;
		try {
			ri = JCasUtil.selectSingle(canAnswer, ResultInfo.class);
		} catch (IllegalArgumentException e) {
			ri = null;
		}
		isLast += (ai.getIsLast() && (ri == null || ri.getIsLast()) ? 1 : 0);
		// logger.debug("in: canAnswer {}, isLast {}", canAnswer.getDocumentText(), isLast);

		if (canAnswer.getDocumentText() == null)
			return; // we received a dummy CAS

		AnswerFV fv = new AnswerFV(ai);
		Answer answer = new Answer(finalAnswerHitlistView);
		String text = canAnswer.getDocumentText();
		answer.setText(text);
		answer.setCanonText(ai.getCanonText());

		// System.err.println("AR process: " + answer.getText());

		List<AnswerFeatures> answers = answersByText.get(text);
		if (answers == null) {
			answers = new LinkedList<AnswerFeatures>();
			answersByText.put(text, answers);
		}
		answers.add(new AnswerFeatures(answer, fv));
	}

	public boolean hasNext() throws AnalysisEngineProcessException {
		return isLast >= isLastBarrier;
	}

	public AbstractCas next() throws AnalysisEngineProcessException {
		if (isLast < isLastBarrier)
			throw new AnalysisEngineProcessException();

		/* Deduplicate Answer objects and index them. */
		for (Entry<String, List<AnswerFeatures>> entry : answersByText.entrySet()) {
			Answer mainAns = null;
			AnswerFV mainFV = null;
			for (AnswerFeatures af : entry.getValue()) {
				Answer answer = af.getAnswer();
				/* In case of hitlist-reuse, keep overriding
				 * early Answer records instead of merging. */
				if (mainAns == null || doReuseHitlist) {
					mainAns = answer;
					mainFV = af.getFV();
					continue;
				}
				logger.debug("hitlist merge " + mainAns.getText() + "|" + answer.getText());
				mainFV.merge(af.getFV());
			}

			/* XXX: Code duplication with AnswerTextMerger */
			/* At this point we can generate some features
			 * to be aggregated over all individual answer
			 * instances. */
			if (mainFV.getFeatureValue(AF_OriginPsgFirst.class)
			    + mainFV.getFeatureValue(AF_OriginPsgNP.class)
			    + mainFV.getFeatureValue(AF_OriginPsgNE.class)
			    + mainFV.getFeatureValue(AF_OriginPsgNPByLATSubj.class)
			    + mainFV.getFeatureValue(AF_OriginDocTitle.class)
			    + mainFV.getFeatureValue(AF_OriginDBpRelation.class) > 1.0)
				mainFV.setFeature(AF_OriginMultiple.class, 1.0);
			/* Also restore confidence value if we already
			 * determined it before. */
			if (phaseNum == 1)
				mainAns.setConfidence(mainFV.getFeatureValue(AF_Phase0Score.class));
			else if (phaseNum == 2)
				mainAns.setConfidence(mainFV.getFeatureValue(AF_Phase1Score.class));

			mainAns.setFeatures(mainFV.toFSArray(finalAnswerHitlistView));
			mainAns.addToIndexes();
		}

		JCas outputCas = finalCas;
		reset();
		return outputCas;
	}
}
