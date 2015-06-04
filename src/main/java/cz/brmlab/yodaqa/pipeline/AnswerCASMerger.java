package cz.brmlab.yodaqa.pipeline;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.flow.dashboard.QuestionAnswer;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginDBpOntology;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginDBpProperty;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginFreebaseOntology;
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
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerResource;
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

	/** A compound representation of an answer.  This is a container
	 * of all the featurestructures related to an answer, living in
	 * the finalHitlist view but not indexed yet (because of anticipated
	 * merging). */
	protected class AnswerFeatures {
		Answer answer;
		AnswerFV fv;
		List<LAT> lats;
		List<AnswerResource> resources;

		public AnswerFeatures(Answer answer_, AnswerFV fv_, List<LAT> lats_, List<AnswerResource> resources_) {
			answer = answer_;
			fv = fv_;
			lats = lats_;
			resources = resources_;
		}

		/** * @return the answer */
		public Answer getAnswer() { return answer; }
		/** * @return the lats */
		public List<LAT> getLats() { return lats; }
		/** * @return the fv */
		public AnswerFV getFV() { return fv; }
		public List<AnswerResource> getResources() { return resources; }
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

	/** Record an answer (in the compound AnswerFeatures representation)
	 * in the internal memory of the CASMerger. */
	protected void addAnswer(AnswerFeatures afs) {
		String text = afs.getAnswer().getText();
		List<AnswerFeatures> answers = answersByText.get(text);
		if (answers == null) {
			answers = new LinkedList<AnswerFeatures>();
			answersByText.put(text, answers);
		}
		answers.add(afs);
	}

	/** Load an AnswerHitlistCAS to our internal memory. */
	protected void loadHitlist(JCas inputHitlist, JCas outputHitlist) {
		CasCopier copier = new CasCopier(inputHitlist.getCas(), outputHitlist.getCas());
		for (Answer inAnswer : JCasUtil.select(inputHitlist, Answer.class)) {
			Answer outAnswer = (Answer) copier.copyFs(inAnswer);
			// logger.debug("in: hitlist answer {}", outAnswer.getText());

			List<LAT> lats = new ArrayList<>();
			// XXX wahh, didn't find any better way than
			// a for loop, java is sick!
			for (FeatureStructure latfs : inAnswer.getLats().toArray()) {
				lats.add((LAT) copier.copyFs(latfs));
			}
			List<AnswerResource> resources = new ArrayList<>();
			if (inAnswer.getResources() != null)
				for (FeatureStructure resfs : inAnswer.getResources().toArray())
					resources.add((AnswerResource) copier.copyFs(resfs));

			addAnswer(new AnswerFeatures(outAnswer, new AnswerFV(outAnswer), lats, resources));
		}
	}

	/** Check whether the answer (in given AnswerCAS) has the
	 * isLast flag set. */
	protected boolean isAnswerLast(JCas canAnswer, AnswerInfo ai) {
		ResultInfo ri;
		try {
			ri = JCasUtil.selectSingle(canAnswer, ResultInfo.class);
		} catch (IllegalArgumentException e) {
			ri = null;
		}
		return ai.getIsLast() && (ri == null || ri.getIsLast());
	}

	/** Convert given AnswerCAS to an Answer FS in an AnswerHitlistCAS. */
	protected Answer makeAnswer(JCas canAnswer, AnswerInfo ai, JCas hitlistCas) {
		Answer answer = new Answer(hitlistCas);
		answer.setText(canAnswer.getDocumentText());
		answer.setCanonText(ai.getCanonText());

		/* Store the Focus. */
		for (Focus focus : JCasUtil.select(canAnswer, Focus.class)) {
			answer.setFocus(focus.getCoveredText());
			break;
		}
		return answer;
	}

	/** Load and generate a compound representation (AnswerFeatures)
	 * for answer stored in the given AnswerCAS. */
	protected AnswerFeatures loadAnswer(JCas canAnswer, AnswerInfo ai, JCas hitlistCas) throws AnalysisEngineProcessException {
		Answer answer = makeAnswer(canAnswer, ai, hitlistCas);
		AnswerFV fv = new AnswerFV(ai);

		/* Store the LATs. */
		List<LAT> latlist = new ArrayList<>();
		for (LAT lat : JCasUtil.select(canAnswer, LAT.class)) {
			/* We cannot just copy the LAT since it would bring
			 * in the complete parse tree and things would be
			 * getting pretty huge. */
			LAT finalLAT;
			try {
				finalLAT = lat.getClass().getConstructor(JCas.class).newInstance(hitlistCas);
			} catch (Exception e) {
				throw new AnalysisEngineProcessException(e);
			}
			finalLAT.setText(lat.getText());
			finalLAT.setSpecificity(lat.getSpecificity());
			finalLAT.setSynset(lat.getSynset());
			// TODO: Carry over baseLAT
			finalLAT.setIsHierarchical(lat.getIsHierarchical());
			latlist.add(finalLAT);
		}
		/* Store the resources. */
		List<AnswerResource> resources = new ArrayList<>();
		if (ai.getResources() != null) {
			for (FeatureStructure resfs : ai.getResources().toArray()) {
				// XXX: Use CasCopier?
				AnswerResource res = new AnswerResource(hitlistCas);
				res.setIri(((AnswerResource) resfs).getIri());
				resources.add(res);
			}
		}

		return new AnswerFeatures(answer, fv, latlist, resources);
	}

	public synchronized void process(JCas canCas) throws AnalysisEngineProcessException {
		JCas canQuestion;
		try { canQuestion = canCas.getView("Question"); } catch (Exception e) { throw new AnalysisEngineProcessException(e); }

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
		}

		if (doReuseHitlist && isFirst) {
			/* AnswerHitlistCAS */
			isFirst = false;
			JCas canAnswerHitlist;
			try { canAnswerHitlist = canCas.getView("AnswerHitlist"); } catch (Exception e) { throw new AnalysisEngineProcessException(e); }

			loadHitlist(canAnswerHitlist, finalAnswerHitlistView);

		} else {
			/* AnswerCAS */
			isFirst = false;
			JCas canAnswer;
			try { canAnswer = canCas.getView("Answer"); } catch (Exception e) { throw new AnalysisEngineProcessException(e); }
			AnswerInfo ai = JCasUtil.selectSingle(canAnswer, AnswerInfo.class);

			isLast += isAnswerLast(canAnswer, ai) ? 1 : 0;
			// logger.debug("in: canAnswer {}, isLast {}", canAnswer.getDocumentText(), isLast);

			if (canAnswer.getDocumentText() == null)
				return; // we received a dummy CAS

			AnswerFeatures afs = loadAnswer(canAnswer, ai, finalAnswerHitlistView);
			addAnswer(afs);
			// System.err.println("AR process: " + afs.getAnswer().getText());

			QuestionAnswer qa = new QuestionAnswer(afs.getAnswer().getText(), 0);
			QuestionDashboard.getInstance().get(finalQuestionView).addAnswer(qa);
		}
	}

	public synchronized boolean hasNext() throws AnalysisEngineProcessException {
		return isLast >= isLastBarrier;
	}

	public AbstractCas next() throws AnalysisEngineProcessException {
		if (isLast < isLastBarrier)
			throw new AnalysisEngineProcessException();

		/* Deduplicate Answer objects and index them. */
		for (Entry<String, List<AnswerFeatures>> entry : answersByText.entrySet()) {
			Answer mainAns = null;
			AnswerFV mainFV = null;
			List<LAT> mainLats = null;
			List<AnswerResource> mainResources = null;
			for (AnswerFeatures af : entry.getValue()) {
				Answer answer = af.getAnswer();
				/* In case of hitlist-reuse, keep overriding
				 * early Answer records instead of merging. */
				if (mainAns == null || doReuseHitlist) {
					mainAns = answer;
					mainFV = af.getFV();
					mainLats = af.getLats();
					mainResources = af.getResources();
					continue;
				}
				logger.debug("hitlist merge " + mainAns.getText() + "|" + answer.getText());
				mainFV.merge(af.getFV());

				/* Merge LATs: */
				for (LAT lat : af.getLats()) {
					boolean alreadyHave = false;
					for (LAT mLat : mainLats) {
						if (mLat.getClass() == lat.getClass() && mLat.getText().equals(lat.getText())) {
							alreadyHave = true;
							break;
						}
					}
					if (!alreadyHave)
						mainLats.add(lat);
				}

				/* Merge resources: */
				for (AnswerResource res : af.getResources()) {
					boolean alreadyHave = false;
					for (AnswerResource mRes : mainResources) {
						if (mRes.getIri().equals(res.getIri())) {
							alreadyHave = true;
							break;
						}
					}
					if (!alreadyHave)
						mainResources.add(res);
				}
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
			    + mainFV.getFeatureValue(AF_OriginDBpOntology.class)
			    + mainFV.getFeatureValue(AF_OriginDBpProperty.class)
			    + mainFV.getFeatureValue(AF_OriginFreebaseOntology.class) > 1.0)
				mainFV.setFeature(AF_OriginMultiple.class, 1.0);
			/* Also restore confidence value if we already
			 * determined it before. */
			if (phaseNum == 1)
				mainAns.setConfidence(mainFV.getFeatureValue(AF_Phase0Score.class));
			else if (phaseNum == 2)
				mainAns.setConfidence(mainFV.getFeatureValue(AF_Phase1Score.class));

			mainAns.setFeatures(mainFV.toFSArray(finalAnswerHitlistView));
			for (LAT lat : mainLats)
				lat.addToIndexes();
			mainAns.setLats(FSCollectionFactory.createFSArray(finalAnswerHitlistView, mainLats));
			for (AnswerResource res : mainResources)
				res.addToIndexes();
			mainAns.setResources(FSCollectionFactory.createFSArray(finalAnswerHitlistView, mainResources));
			mainAns.addToIndexes();
		}

		JCas outputCas = finalCas;
		reset();
		return outputCas;
	}
}
