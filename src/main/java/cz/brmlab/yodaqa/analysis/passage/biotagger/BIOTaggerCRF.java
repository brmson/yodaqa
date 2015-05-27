package cz.brmlab.yodaqa.analysis.passage.biotagger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkSequenceAnnotator;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instances;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Focus;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Following;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Ngram;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Preceding;
import org.cleartk.ml.feature.extractor.CombinedExtractor1;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.extractor.TypePathExtractor;

import approxlib.distance.EditDist;
import approxlib.tree.LblTree;

import cz.brmlab.yodaqa.analysis.answer.LATByQuantity;
import cz.brmlab.yodaqa.model.SearchResult.AnswerBioMention;
import cz.brmlab.yodaqa.model.SearchResult.Passage;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.model.TyCor.QuestionWordLAT;
import cz.brmlab.yodaqa.provider.crf.CRFSuite;
import cz.brmlab.yodaqa.provider.crf.CRFTagging;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/** A CRF-based token sequence annotator tagging tokens with B-I-O labels.
 * That is, "Begin"/"Inside"/"Outside".  I.e., "bio" does not relate
 * to anything biological.  This can be also perceived as basically
 * a custom "answer named entity" recognizer (that can use some
 * question-specific features).
 *
 * The B-I-O token labelling is done based on various token-specific
 * features that also take into account some question features.  These
 * features are passed through a sequence machine learned model; we use
 * CRF (Conditional Random Fields).  Overally, we are heavily inspired
 * by (Yao and van Durme, 2013a) here (aka JacanaQA).
 *
 * Unlike our other machine learners (well, the one for answer selection)
 * we do not re-train the model used here on each evaluation run; that's
 * a TODO item.  XXX: add re-training instructions
 *
 * We use ClearTK for the training of the CRF model, also unlike our
 * other machine learners.  We use it because of the CRFsuite interface
 * (I also looked at DKPro-TC, but it seems to be built just to run
 * experiments, not to actually *use* the models within a pipeline too)
 * but also to check if it's nice enough to convert AnswerFV to it too.
 *
 * XXX: ...and for *tagging* based on trained model, we use jcrfsuite
 * instead.  Yes, this is messy, but ClearTK interface for crfsuite
 * has three caveats:
 *
 *   * It is GPL2, unlike most of ClearTK
 *   * It does not support outputting tag probability as of now
 *     (but see https://groups.google.com/forum/#!topic/cleartk-users/n6xoaINnJu8)
 *   * It is rather slow as it executes new crfsuite process for
 *     each classify() call
 *
 * TODO: Either switch to ClearTK with some more of our machine
 * learning stuff and eventually contribute back a better crfsuite
 * wrapper, or use jcrfsuite for training too. */

@SofaCapability(
	inputSofas = { "Question", "Result", "PickedPassages" },
	outputSofas = { "PickedPassages" }
)

public class BIOTaggerCRF extends CleartkSequenceAnnotator<String> {
	protected FeatureExtractor1<Token> tokenFeatureExtractor;
	protected List<CleartkExtractor<Token, Token>> ngramFeatureExtractors;
	protected CRFBioChunking<Token, AnswerBioMention> chunking;

	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		FeatureExtractor1<Token> posExtractor = new TypePathExtractor<Token>(Token.class, "pos/PosValue");
		FeatureExtractor1<Token> NETypeExtractor = new CoveringNETypeExtractor<Token>();
		FeatureExtractor1<Token> depExtractor = new DependencyTypeExtractor<Token>();
		this.tokenFeatureExtractor = new CombinedExtractor1<Token>(
				// TODO: NumericTypeFeatureFunction?
				posExtractor,
				NETypeExtractor,
				depExtractor);

		this.ngramFeatureExtractors = new ArrayList<>();
		addNgramFeatureExtractor(posExtractor, 3);
		addNgramFeatureExtractor(NETypeExtractor, 3);
		/* TODO: The n-grams here should not be on token
		 * sequence but parse tree. Maybe. */
		addNgramFeatureExtractor(depExtractor, 2);

		/* Tokens will be combined to form AnswerBioMentions,
		 * with labels from the "mentionType" attribute; this
		 * label is actually always "ans", so we get B-ans, I-ans,
		 * O-ans. */
		this.chunking = new CRFBioChunking<Token, AnswerBioMention>(
				Token.class, AnswerBioMention.class);
	}

	protected void addNgramFeatureExtractor(FeatureExtractor1<Token> extractor, int n_context) {
		/* Context width: 3 */

		/* Unigrams (shifted): */
		this.ngramFeatureExtractors.add(new CleartkExtractor<Token, Token>(Token.class, extractor,
				new Ngram(new Following(0, 1))));
		this.ngramFeatureExtractors.add(new CleartkExtractor<Token, Token>(Token.class, extractor,
				new Ngram(new Following(1, 2))));
		this.ngramFeatureExtractors.add(new CleartkExtractor<Token, Token>(Token.class, extractor,
				new Ngram(new Preceding(0, 1))));
		this.ngramFeatureExtractors.add(new CleartkExtractor<Token, Token>(Token.class, extractor,
				new Ngram(new Preceding(1, 2))));
		if (n_context == 1)
			return;

		/* Bigrams: */
		this.ngramFeatureExtractors.add(new CleartkExtractor<Token, Token>(Token.class, extractor,
				new Ngram(new Focus(), new Following(1))));
		this.ngramFeatureExtractors.add(new CleartkExtractor<Token, Token>(Token.class, extractor,
				new Ngram(new Following(2))));
		this.ngramFeatureExtractors.add(new CleartkExtractor<Token, Token>(Token.class, extractor,
				new Ngram(new Preceding(1), new Focus())));
		this.ngramFeatureExtractors.add(new CleartkExtractor<Token, Token>(Token.class, extractor,
				new Ngram(new Preceding(2))));
		if (n_context == 2)
			return;

		/* Trigrams: */
		this.ngramFeatureExtractors.add(new CleartkExtractor<Token, Token>(Token.class, extractor,
				new Ngram(new Focus(), new Following(2))));
		this.ngramFeatureExtractors.add(new CleartkExtractor<Token, Token>(Token.class, extractor,
				new Ngram(new Preceding(2), new Focus())));
		if (n_context == 3)
			return;
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, passagesView;
		try {
			questionView = jcas.getView("Question");
			passagesView = jcas.getView("PickedPassages");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		/* We may want to generate question-specific features
		 * based on a question LAT.  Decide on the set of LATs
		 * (or rather just their synset ids) to use for this. */
		Collection<Long> lats = getSpecializingLATs(questionView);
		/* A tree representation of the question dependency tree. */
		LblTree qTree = LblTreeCASFactory.casToTree(questionView);

		// for each sentence in the document, generate training/classification instances
		for (Passage p : JCasUtil.select(passagesView, Passage.class)) {
			processPassage(passagesView, p, lats, qTree);
		}
	}

	protected Collection<Long> getSpecializingLATs(JCas questionView) {
		Collection<LAT> qLats = JCasUtil.select(questionView, LAT.class);
		Collection<Long> lats = new HashSet<Long>();
		for (LAT lat : qLats) {
			if (lat.getSynset() == 0)
				continue; // no synset
			if (lats.contains(lat.getSynset()))
				continue; // dupe

			if (lat instanceof QuestionWordLAT) {
				lats.add(lat.getSynset());
			} else if (LATByQuantity.latIsQuantity(lat)) {
				lats.add(2L /* special indicator for quantity LATs */);
			}
		}
		return lats;
	}

	protected void processPassage(JCas passagesView, Passage p, Collection<Long> lats, LblTree qTree)
			throws AnalysisEngineProcessException {
		List<List<Feature>> featureLists = new ArrayList<List<Feature>>();

		/* Compare the dependency tree of the passage and the question;
		 * this will be used to produce alignment-related features.
		 * In other words, we are trying to find a way to rewrite
		 * the passage to the question, and take note which tokens
		 * we can keep as they are, which we need to delete and which
		 * we should rename (i.e. change their tagging). */
		LblTree aTree = LblTreeCASFactory.spanToTree(passagesView, p);
		EditFeatureGenerator editExtractor = null;
		/* N.B. dependency tree may be missing, e.g. due to the #tokens
		 * limit parser hit. */
		if (aTree != null && qTree != null) {
			EditDist editDist = new EditDist(/* normalized */ true);
			editDist.treeDist(aTree, qTree);
			editDist.printHumaneEditScript();
			editExtractor = new EditFeatureGenerator(editDist);
		}

		// for each token, extract features and the outcome
		List<Token> tokens = JCasUtil.selectCovered(Token.class, p);
		int i = 0;
		for (Token token : tokens) {
			// apply the feature extractors
			List<Feature> tokenFeatures = new ArrayList<Feature>();
			tokenFeatures.addAll(this.tokenFeatureExtractor.extract(passagesView, token));
			for (CleartkExtractor<Token, Token> ngramExtractor : ngramFeatureExtractors)
				tokenFeatures.addAll(ngramExtractor.extractWithin(passagesView, token, p));
			// tokenFeatures.add(new Feature("lemma", token.getLemma().getValue())); // for debugging

			// apply the edit feature generator
			if (editExtractor != null)
				tokenFeatures.addAll(editExtractor.extract(tokenFeatures, i, token, aTree, qTree));

			/* Combine with question LAT info, so each feature
			 * will have specific weight for the given class
			 * of questions.  N.B. non-combined features are also
			 * still kept and used!  (The motivation is to provide
			 * a reasonable baseline for LATs unseen during
			 * training.) */
			tokenFeatures.addAll(expandFeaturesByLats(tokenFeatures, lats));
			featureLists.add(tokenFeatures);
			i++;
		}

		if (this.isTraining()) {
			// during training, convert existing mentions in the CAS into expected classifier outcomes

			List<AnswerBioMention> abms = JCasUtil.selectCovered(AnswerBioMention.class, p);
			if (!abms.isEmpty()) {
				/* Do not train on passages with no answer
				 * mentions, the set would be too negatively
				 * biased then. */
				// convert the mention annotations into token-level BIO outcome labels
				List<String> outcomes = this.chunking.createOutcomes(passagesView, tokens, abms);
				// write the features and outcomes as training instances
				this.dataWriter.write(Instances.toInstances(outcomes, featureLists));
			}

		} else {
			// during classification, convert classifier outcomes into mentions in the CAS

			// get the predicted BIO outcome labels from the classifier
			CRFTagging tagging = CRFSuite.getInstance().tag(featureLists);
			tagging.logProb(tokens);

			// create the AnswerBioMention annotations in the CAS
			this.chunking.createChunks(passagesView, tokens, tagging);
		}
	}

	protected List<Feature> expandFeaturesByLats(List<Feature> features, Collection<Long> lats) {
		List<Feature> xFeatures = new ArrayList<>();
		for (Feature f : features) {
			for (Long l : lats) {
				xFeatures.add(new Feature(Long.toString(l) + "|" + f.getName(), f.getValue()));
			}
		}
		return xFeatures;
	}
}
