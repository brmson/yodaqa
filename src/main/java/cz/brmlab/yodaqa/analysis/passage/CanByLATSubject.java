package cz.brmlab.yodaqa.analysis.passage;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.TreeUtil;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
import cz.brmlab.yodaqa.model.TyCor.LAT;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
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

public class CanByLATSubject extends CandidateGenerator {
	public CanByLATSubject() {
		logger = LoggerFactory.getLogger(CanByLATSubject.class);
	}


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

				Annotation base = TreeUtil.widestCoveringNP(nsubj.getGovernor());
				if (base == null)
					base = nsubj.getGovernor(); // cheat :)
				if (base instanceof Token) {
					String pos = ((Token) base).getPos().getPosValue();
					if (pos.matches("^V.*")) {
						logger.debug("Ignoring verb governor {} {}", pos, base.getCoveredText());
						continue;
					}
				}

				AnswerFV fv = new AnswerFV(ri.getAnsfeatures());
				fv.merge(new AnswerFV(p.getAnsfeatures()));
				/* This is both origin and tycor feature, essentially. */
				fv.setFeature(AF.OriginPsgNPByLATSubj, 1.0);

				addCandidateAnswer(passagesView, p, base, fv);
			}
		}
	}
}
