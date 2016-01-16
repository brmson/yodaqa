package cz.brmlab.yodaqa.analysis.question;

import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.TreeUtil;
import cz.brmlab.yodaqa.analysis.answer.SyntaxCanonization;
import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.TyCor.ImplicitQLAT;
import cz.brmlab.yodaqa.model.TyCor.QuestionWordLAT;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.provider.OpenNlpNamedEntities;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.NN;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.NP;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.NSUBJ;

/**
 * Generate LAT annotations in a QuestionCAS. These are words that should
 * be type-coercable to the answer term. E.g. "Who starred in Moon?" should
 * generate LATs "who", "actor", possibly "star".  Candidate answers will be
 * matched against LATs to acquire score.  Focus is typically always also an
 * LAT.
 *
 * Prospectively, we will want to add multiple diverse LAT annotators. This
 * one simply generates a single LAT from the Focus. */

public class LATByFocus extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(LATByFocus.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		/* A Focus is also an LAT. */
		for (Focus focus : JCasUtil.select(jcas, Focus.class)) {
			/* ...however, prefer an overlapping named entity. */
			if (!addNELAT(jcas, focus))
				addFocusLAT(jcas, focus);
		}

		/* TODO: Also derive an LAT from SV subject nominalization
		 * using wordnet. */
	}

	protected void addFocusLAT(JCas jcas, Focus focus) {
		String text = focus.getToken().getLemma().getValue().toLowerCase();
		POS pos = focus.getToken().getPos();

		/* If focus is the question word, convert to an appropriate
		 * concept word or give up. */
		/* N.B. since these LATs are generated with a precise synset,
		 * they are not generalized and therefore any answer matches
		 * will need to be qhits. */
		if (text.equals("who") || text.equals("whom")) {
			/* (6833){00007846} <noun.Tops>[03] S: (n) person#1 (person%1:03:00::), individual#1 (individual%1:03:00::), someone#1 (someone%1:03:00::), somebody#1 (somebody%1:03:00::), mortal#1 (mortal%1:03:00::), soul#2 (soul%1:03:00::) (a human being) "there was too much for oneperson to do" */
			addFocusLAT(jcas, focus, "person", null, 7846, 0.0, new QuestionWordLAT(jcas));

		} else if (text.equals("when")) {
			/* (114){15147173} <noun.time>[28] S: (n) time#3 (time%1:28:00::) (an indefinite period (usually marked by specific attributes or activities)) "the time of year for planting"; "he was a great actor in his time" */
			addFocusLAT(jcas, focus, "time", null, 15147173, 0.0, new QuestionWordLAT(jcas));
			/* (23){15184543} <noun.time>[28] S: (n) date#1 (date%1:28:00::), day of the month#1 (day_of_the_month%1:28:00::) (the specified day of the month) "what is the date today?" */
			addFocusLAT(jcas, focus, "date", null, 15184543, 0.0, new QuestionWordLAT(jcas));

		} else if (text.equals("where")) {
			/* (992){00027365} <noun.Tops>[03] S: (n) location#1 (location%1:03:00::) (a point or extent in space) */
			addFocusLAT(jcas, focus, "location", null, 27365, 0.0, new QuestionWordLAT(jcas));

		} else if (text.equals("many") || text.equals("much")) {
			/* (15){00033914} <noun.Tops>[03] S: (n) measure#2 (measure%1:03:00::), quantity#1 (quantity%1:03:00::), amount#3 (amount%1:03:00::) (how much there is or how many there are of something that you can quantify) */
			addFocusLAT(jcas, focus, "amount", null, 33914, 0.0, new QuestionWordLAT(jcas));

		} else if (isAmbiguousQLemma(text)) {
			logger.info("?! Skipping focus LAT for ambiguous qlemma {}", text);

		} else {
			/* Generate an LAT, but since we do not specify
			 * a synset, it will also be generalized by
			 * LATByWordnet - this shall allow "city"-"town"
			 * matches, matching various locations when answering
			 * "From which site did X start his first flight?"
			 * and so on. */

			/* Primarily generate the non-lemmatized LAT.  For
			 * example for "species", we also generate the original
			 * form in addition to the (wrongly) lemmatized
			 * "specie". */
			String realText = focus.getCoveredText().toLowerCase();
			addFocusLAT(jcas, focus, realText, pos, 0, 0.0, new LAT(jcas));

			if (!text.equals(realText))
				addFocusLAT(jcas, focus, text, pos, 0, 0.0, new ImplicitQLAT(jcas));

			/* Also try to generate a "main character" LAT in
			 * addition to "character", etc. */

			NP np = TreeUtil.shortestCoveringNP(focus.getToken());
			if (np != null) {
				String npText = SyntaxCanonization.getCanonText(np.getCoveredText().toLowerCase());
				if (!npText.equals(realText)) {
					logger.debug("NP coverage: <<{}>>", npText);
					addLAT(new LAT(jcas), np.getBegin(), np.getEnd(), np, npText, pos, 0, 1.0);
				}
			}
		}
	}

	protected void addFocusLAT(JCas jcas, Focus focus, String text, POS pos, long synset, double spec, LAT lat) {
		if (pos == null) {
			/* We have a synthetic focus noun, synthetize
			 * a POS tag for it. */
			pos = new NN(jcas);
			pos.setBegin(focus.getBegin());
			pos.setEnd(focus.getEnd());
			pos.setPosValue("NNS");
			pos.addToIndexes();
		}

		addLAT(lat, focus.getBegin(), focus.getEnd(), focus, text, pos, synset, spec);
	}

	protected boolean addNELAT(JCas jcas, Focus focus) {
		/* N.B. since these LATs are generated with a precise synset,
		 * they are not generalized and therefore any answer matches
		 * will need to be qhits. */
		boolean ne_found = false;
		for (NamedEntity ne : JCasUtil.selectCovering(NamedEntity.class, focus)) {
			ne_found = true;
			long synset = OpenNlpNamedEntities.neValueToSynset(ne.getValue());
			addLAT(new LAT(jcas), ne.getBegin(), ne.getEnd(), ne, ne.getValue(), focus.getToken().getPos(), synset, -2.0);
		}
		return ne_found;
	}

	protected void addLAT(LAT lat, int begin, int end, Annotation base, String text, POS pos, long synset, double spec) {
		lat.setBegin(begin);
		lat.setEnd(end);
		lat.setBase(base);
		lat.setPos(pos);
		lat.setText(text);
		lat.setSpecificity(spec);
		lat.setSynset(synset);
		lat.addToIndexes();
		logger.debug("new LAT by {}: <<{}>>/{}", base.getType().getShortName(), text, synset);
	}

	/** Decide whether the given question lemma has no derivable
	 * semantic sense. */
	public static boolean isAmbiguousQLemma(String text) {
		return text.matches("^what|why|how|which|name$");
	}
}
