package cz.brmlab.yodaqa.analysis.passage.biotagger;

import java.util.ArrayList;
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
import org.cleartk.ml.chunking.BioChunking;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Focus;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Following;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Ngram;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Preceding;
import org.cleartk.ml.feature.extractor.CombinedExtractor1;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.extractor.TypePathExtractor;

import cz.brmlab.yodaqa.model.SearchResult.AnswerBioMention;
import cz.brmlab.yodaqa.model.SearchResult.Passage;

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
 * We use ClearTK for the CRF model and its training, also unlike our
 * other machine learners.  We use it because of the CRFsuite interface
 * (I also looked at DKPro-TC, but it seems to be built just to run
 * experiments, not to actually *use* the models within a pipeline too)
 * but also to check if it's nice enough to convert AnswerFV to it too. */

@SofaCapability(
	inputSofas = { "Question", "Result", "PickedPassages" },
	outputSofas = { "PickedPassages" }
)

public class BIOTaggerCRF extends CleartkSequenceAnnotator<String> {
	protected FeatureExtractor1<Token> tokenFeatureExtractor;
	protected List<CleartkExtractor<Token, Token>> ngramFeatureExtractors;
	protected BioChunking<Token, AnswerBioMention> chunking;

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
		addNgramFeatureExtractor(posExtractor);
		addNgramFeatureExtractor(NETypeExtractor);
		/* TODO: The n-grams here should not be on token
		 * sequence but parse tree. Maybe. */
		addNgramFeatureExtractor(depExtractor);

		/* Tokens will be combined to form AnswerBioMentions,
		 * with labels from the "mentionType" attribute; this
		 * label is actually always "ans", so we get B-ans, I-ans,
		 * O-ans. */
		this.chunking = new BioChunking<Token, AnswerBioMention>(
				Token.class, AnswerBioMention.class);
	}

	protected void addNgramFeatureExtractor(FeatureExtractor1<Token> extractor) {
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

		/* Bigrams: */
		this.ngramFeatureExtractors.add(new CleartkExtractor<Token, Token>(Token.class, extractor,
				new Ngram(new Focus(), new Following(1))));
		this.ngramFeatureExtractors.add(new CleartkExtractor<Token, Token>(Token.class, extractor,
				new Ngram(new Following(2))));
		this.ngramFeatureExtractors.add(new CleartkExtractor<Token, Token>(Token.class, extractor,
				new Ngram(new Preceding(1), new Focus())));
		this.ngramFeatureExtractors.add(new CleartkExtractor<Token, Token>(Token.class, extractor,
				new Ngram(new Preceding(2))));

		/* Trigrams: */
		this.ngramFeatureExtractors.add(new CleartkExtractor<Token, Token>(Token.class, extractor,
				new Ngram(new Focus(), new Following(2))));
		this.ngramFeatureExtractors.add(new CleartkExtractor<Token, Token>(Token.class, extractor,
				new Ngram(new Preceding(2), new Focus())));
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, passagesView;
		try {
			questionView = jcas.getView("Question");
			passagesView = jcas.getView("PickedPassages");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		// for each sentence in the document, generate training/classification instances
		for (Passage p : JCasUtil.select(passagesView, Passage.class)) {
			processPassage(passagesView, p);
		}
	}

	protected void processPassage(JCas passagesView, Passage p) throws AnalysisEngineProcessException {
		List<List<Feature>> featureLists = new ArrayList<List<Feature>>();

		// for each token, extract features and the outcome
		List<Token> tokens = JCasUtil.selectCovered(passagesView, Token.class, p);
		for (Token token : tokens) {
			// apply the feature extractors
			List<Feature> tokenFeatures = new ArrayList<Feature>();
			tokenFeatures.addAll(this.tokenFeatureExtractor.extract(passagesView, token));
			for (CleartkExtractor<Token, Token> ngramExtractor : ngramFeatureExtractors)
				tokenFeatures.addAll(ngramExtractor.extractWithin(passagesView, token, p));
			// TODO combine with question LAT (all hypernymes?)
			featureLists.add(tokenFeatures);
		}

		if (this.isTraining()) {
			// during training, convert existing mentions in the CAS into expected classifier outcomes

			List<AnswerBioMention> abms = JCasUtil.selectCovered(passagesView, AnswerBioMention.class, p);
			// convert the mention annotations into token-level BIO outcome labels
			List<String> outcomes = this.chunking.createOutcomes(passagesView, tokens, abms);
			// write the features and outcomes as training instances
			this.dataWriter.write(Instances.toInstances(outcomes, featureLists));

		} else {
			// during classification, convert classifier outcomes into mentions in the CAS

			// get the predicted BIO outcome labels from the classifier
			List<String> outcomes = this.classifier.classify(featureLists);
			// create the AnswerBioMention annotations in the CAS
			this.chunking.createChunks(passagesView, tokens, outcomes);
		}
	}
}
