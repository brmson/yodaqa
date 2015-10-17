package cz.brmlab.yodaqa.analysis.passextract;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.PF_ClueMatch;
import cz.brmlab.yodaqa.model.SearchResult.PF_ClueWeight;
import cz.brmlab.yodaqa.model.SearchResult.PF_AboutClueMatch;
import cz.brmlab.yodaqa.model.SearchResult.PF_AboutClueWeight;
import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.model.SearchResult.PassageFeature;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;

/* XXX: The clue-specific features, ugh. */
import cz.brmlab.yodaqa.model.Question.*;
import cz.brmlab.yodaqa.model.CandidateAnswer.*;

/**
 * Generate Passages from Sentences that contain some Clue in Question
 * and copy them over to the Passages view.
 *
 * Prospectively, we might want to keep some surrounding sentences,
 * though. */

@SofaCapability(
	inputSofas = { "Question", "Result" },
	outputSofas = { "Passages" }
)


public class PassByClue extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(PassByClue.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, resultView, passagesView;
		try {
			questionView = jcas.getView("Question");
			resultView = jcas.getView("Result");
			passagesView = jcas.getView("Passages");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		/* Below, we match clues only as whole words (when we are
		 * looking for "grant", we don't want "vagrant").  However,
		 * this is suboptimal e.g. in case of "nickname" -> "nicknames"
		 * or "nickname" -> "nicknamed", and we didn't do lemmatization
		 * yet. Therefore, as a poor man's fuzzy match, we add an extra
		 * ".?" after the clue to catch simple English lemma forms (and
		 * little in terms of false positives). */

		/* Figure out which clues is the text generally about,
		 * i.e. contained in the title. */
		Map<Clue, Boolean> clueIsAbout = new HashMap<Clue, Boolean>();
		ResultInfo ri = JCasUtil.selectSingle(resultView, ResultInfo.class);
		for (Clue clue : JCasUtil.select(questionView, Clue.class))
			if (ri.getDocumentTitle().matches(getClueRegex(clue)))
				clueIsAbout.put(clue, true);

		/* Pre-index covering info. */
		Map<Sentence, Collection<Annotation>> covering =
			JCasUtil.indexCovered(resultView, Sentence.class, Annotation.class);

		/* Walk sentences, match them and copy what we like. */
		/* XXX: Ideally, we would match all clues in parallel,
		 * but for now we opt for a trivial O(M*N) approach. */
		CasCopier copier = new CasCopier(resultView.getCas(), passagesView.getCas());
		for (Sentence sentence : JCasUtil.select(resultView, Sentence.class)) {
			List<PassageFeature> features = new LinkedList<PassageFeature>();
			String featureStr = "";

			/* Collect answer features. */
			AnswerFV afv = new AnswerFV();
			afv.setFeature(AF.OriginPsg, 1.0);

			/* TODO: Put clues in a hierarchy so that we don't
			 * try to match word clues of phrase clues we already
			 * matched. */
			for (Clue clue : JCasUtil.select(questionView, Clue.class)) {
				if (!sentence.getCoveredText().matches(getClueRegex(clue)))
					continue;
				/* Match! */

				/* We will want to weight about-clues less than
				 * others to reduce the noise. If our clues are
				 * "Bob Marley" and "died", in article about
				 * "Bob Marley", the sentences talking about
				 * death are much more important than sentences
				 * about Marley. */
				try {
					if (clueIsAbout.containsKey(clue)) {
						clueFeatures(passagesView, features, PF_AboutClueMatch.class, PF_AboutClueWeight.class, clue);
						featureStr += "a";
					} else {
						clueFeatures(passagesView, features, PF_ClueMatch.class, PF_ClueWeight.class, clue);
						clueAnswerFeatures(afv, clue);
						featureStr += "m";
					}
				} catch (InstantiationException e) {
					throw new AnalysisEngineProcessException(e);
				} catch (IllegalAccessException e) {
					throw new AnalysisEngineProcessException(e);
				} catch (InvocationTargetException e) {
					throw new AnalysisEngineProcessException(e);
				} catch (NoSuchMethodException e) {
					throw new AnalysisEngineProcessException(e);
				}
			}

			if (!features.isEmpty()) {
				/* Annotate. */
				Passage passage = new Passage(passagesView);
				passage.setBegin(sentence.getBegin());
				passage.setEnd(sentence.getEnd());
				passage.setAnsfeatures(afv.toFSArray(passagesView));
				passage.setFeatures(FSCollectionFactory.createFSArray(jcas, features));
				passage.addToIndexes();

				logger.debug(featureStr + " | " + passage.getCoveredText());

				/* Copy */
				if (!copier.alreadyCopied(sentence)) {
					Sentence s2 = (Sentence) copier.copyFs(sentence);
					s2.addToIndexes();

					for (Annotation a : covering.get(sentence)) {
						if (!copier.alreadyCopied(a)) {
							Annotation a2 = (Annotation) copier.copyFs(a);
							a2.addToIndexes();
						}
					}
				}
			}
		}
	}

	public static String getClueRegex(Clue clue) {
		return getClueRegex(clue, true);
	}

	public static String getClueRegex(Clue clue, boolean anywhere) {
		/* XXX: Maybe we should just tokenize and lemmatize
		 * the passage instead? */
		String label = clue.getLabel();
		if (label.endsWith("y") && label.length() > 1) {
			/* empty -> empties too */
			label = label.substring(0, label.length() - 1) + "\\E[yi]\\Q";
		}
		String regex = "(?i)" + (anywhere ? ".*" : "") + "\\b\\Q" + label + "\\E(.|e?d|e?s|ing)?\\b" + (anywhere ? ".*" : "");
		// logger.debug("regex {}", regex);
		return regex;
	}

	protected void clueFeatures(JCas jcas, List<PassageFeature> features,
			Class<? extends PassageFeature> matchFeature,
			Class<? extends PassageFeature> weightFeature,
			Clue clue)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		PassageFeature matchF = matchFeature.getConstructor(JCas.class).newInstance(jcas);
		matchF.setValue(1.0);
		matchF.addToIndexes();
		features.add(matchF);

		PassageFeature weightF = weightFeature.getConstructor(JCas.class).newInstance(jcas);
		weightF.setValue(clue.getWeight());
		weightF.addToIndexes();
		features.add(weightF);
	}

	protected void clueAnswerFeatures(AnswerFV afv, Clue clue) {
		afv.setFeature(AF.OriginPsgBy_ClueType + clue.getType().getShortName(), 1.0);
		if (clue instanceof ClueSubject) {
			afv.setFeature(AF.OriginPsgBy_ClueType + "ClueSubject", 1.0);
		} else if (clue instanceof ClueConcept ) {
			double bestSourceRr = 0, bestLabelRr = 0, bestScore = 0;
			for (Concept concept : FSCollectionFactory.create(((ClueConcept) clue).getConcepts(), Concept.class)) {
				if (concept.getBySubject())
					afv.setFeature(AF.OriginPsgBy_ClueType + "ClueSubject", 1.0);
				if (concept.getByLAT())
					afv.setFeature(AF.OriginPsgBy_ClueType + "ClueLAT", 1.0);
				if (concept.getByNE())
					afv.setFeature(AF.OriginPsgBy_ClueType + "ClueNE", 1.0);
				if (concept.getByNgram())
					afv.setFeature(AF.OriginPsgBy_ClueType + "ClueNgram", 1.0);
				if (concept.getSourceRr() > bestSourceRr)
					bestSourceRr = concept.getSourceRr();
				if (concept.getLabelRr() > bestLabelRr)
					bestLabelRr = concept.getLabelRr();
				if (concept.getScore() > bestScore)
					bestScore = concept.getScore();
			}
			afv.setFeature(AF.PsgDist_ClueType + AF._clueType_ConceptSourceRR, bestSourceRr);
			afv.setFeature(AF.PsgDist_ClueType + AF._clueType_ConceptLabelRR, bestLabelRr);
			afv.setFeature(AF.OriginPsgBy_ClueType + AF._clueType_ConceptScore, bestScore);
		}
	}
}
