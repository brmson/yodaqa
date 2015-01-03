package cz.brmlab.yodaqa.analysis.passage;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.answer.AnswerFV;
import cz.brmlab.yodaqa.analysis.TreeUtil;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_Occurences;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginPsgNPByLATSubj;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_PassageLogScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorPassageDist;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorPassageInside;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_TyCorPassageSp;
import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;
import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.model.SearchResult.QuestionLATMatch;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
import cz.brmlab.yodaqa.model.TyCor.LAT;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.NP;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NSUBJ;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NSUBJPASS;

/**
 * Create CandidateAnswer for the largest NP covering governor of
 * NSUBJ where the dependent is the question LAT.
 *
 * In human language, the focus of question "What is the critical mass
 * of plutonium?" is "mass"; in passage "A bare critical mass of plutonium
 * at normal density is roughly 10 kilograms.", we find out that NSUBJ
 * dependent is "mass", so we look at the governor ("kilograms") and take
 * the largest covering NP ("roughly 10 kilograms").
 *
 * Originally, we thought of this as focus matching, but (sp=0) LAT
 * is better as we do desirable transformations like who->person or
 * hot->temperature when generating these, and they *are* focus based.
 *
 * Of course this is a special case of CanByNPSurprise but we should be
 * subduing those. */

@SofaCapability(
	inputSofas = { "Question", "Result", "PickedPassages" },
	outputSofas = { "PickedPassages" }
)

public class CanByLATSubject extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(CanByLATSubject.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, resultView, passagesView;
		try {
			questionView = jcas.getView("Question");
			resultView = jcas.getView("Result");
			passagesView = jcas.getView("PickedPassages");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		ResultInfo ri = JCasUtil.selectSingle(resultView, ResultInfo.class);

		for (NSUBJ nsubj : JCasUtil.select(passagesView, NSUBJ.class))
			processSubject(questionView, passagesView, ri, nsubj);
		for (NSUBJPASS nsubj : JCasUtil.select(passagesView, NSUBJPASS.class))
			processSubject(questionView, passagesView, ri, nsubj);
	}

	protected void processSubject(JCas questionView, JCas passagesView, ResultInfo ri, Dependency nsubj)
			throws AnalysisEngineProcessException {
		Passage p = JCasUtil.selectCovering(Passage.class, nsubj).get(0);
		String subjlemma = nsubj.getDependent().getLemma().getValue().toLowerCase();

		for (LAT l : JCasUtil.select(questionView, LAT.class)) {
			/* We consider only the primary LATs, and -1 sp LATs
			 * as those are the possible noun forms. TODO: tune */
			if (l.getSpecificity() < -1.0)
				continue;

			String latlemma = l.getText();
			// logger.debug("lat >{}< subj >{}<", latlemma, subjlemma);
			if (subjlemma.equals(latlemma)) {
				logger.debug("Passage subject {} matches question lat {}", subjlemma, latlemma);

				Annotation np = TreeUtil.widestCoveringNP(nsubj.getGovernor());
				if (np == null)
					np = nsubj.getGovernor(); // cheat :)
				addCandidateAnswer(passagesView, np, ri, p);
			}
		}
	}

	protected void addCandidateAnswer(JCas passagesView, Annotation np, ResultInfo ri, Passage p)
			throws AnalysisEngineProcessException {
		logger.info("caNPByLATSubj {}", np.getCoveredText());

		AnswerFV fv = new AnswerFV(ri.getAnsfeatures());
		fv.merge(new AnswerFV(p.getAnsfeatures()));
		fv.setFeature(AF_Occurences.class, 1.0);
		fv.setFeature(AF_PassageLogScore.class, Math.log(1 + p.getScore()));

		/* This is both origin and tycor feature, essentially. */
		fv.setFeature(AF_OriginPsgNPByLATSubj.class, 1.0);

		for (QuestionLATMatch qlm : JCasUtil.selectCovered(QuestionLATMatch.class, p)) {
			double distance = 1000;
			if (qlm.getBegin() >= np.getBegin() && qlm.getEnd() <= np.getEnd()) {
				distance = 0; // contained inside!
				fv.setFeature(AF_TyCorPassageInside.class, 1.0);
			} else if (qlm.getEnd() <= np.getBegin()) {
				distance = np.getBegin() - qlm.getEnd() - 1;
			} else if (qlm.getBegin() >= np.getEnd()) {
				distance = qlm.getBegin() - np.getEnd() - 1;
			}
			fv.setFeature(AF_TyCorPassageDist.class, Math.exp(-distance));
			fv.setFeature(AF_TyCorPassageSp.class, Math.exp(qlm.getBaseLAT().getSpecificity()) * Math.exp(-distance));
			logger.debug("Passage TyCor (d {}, contains {})", distance, qlm.getBaseLAT().getText());
			// this should be a singleton
			break;
		}

		CandidateAnswer ca = new CandidateAnswer(passagesView);
		ca.setBegin(np.getBegin());
		ca.setEnd(np.getEnd());
		ca.setPassage(p);
		ca.setBase(np);
		ca.setFeatures(fv.toFSArray(passagesView));
		ca.addToIndexes();
	}
}
