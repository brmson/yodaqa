package cz.brmlab.yodaqa.pipeline;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.FlowControllerFactory;
import org.apache.uima.flow.impl.FixedFlowController;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.analysis.ansevid.AnswerEvidencingAE;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerScoringAE;
import cz.brmlab.yodaqa.analysis.answer.AnswerAnalysisAE;
import cz.brmlab.yodaqa.analysis.question.QuestionAnalysisAE;
import cz.brmlab.yodaqa.flow.FixedParallelFlowController;
import cz.brmlab.yodaqa.flow.asb.ParallelEngineFactory;
import cz.brmlab.yodaqa.pipeline.solrdoc.SolrDocAnswerProducer;
import cz.brmlab.yodaqa.pipeline.solrfull.SolrFullAnswerProducer;
import cz.brmlab.yodaqa.pipeline.structured.DBpediaOntologyAnswerProducer;
import cz.brmlab.yodaqa.pipeline.structured.DBpediaPropertyAnswerProducer;
import cz.brmlab.yodaqa.pipeline.structured.FreebaseOntologyAnswerProducer;
import cz.brmlab.yodaqa.pipeline.AnswerHitlistSerialize;
import cz.brmlab.yodaqa.provider.IPv6Check;
import cz.brmlab.yodaqa.provider.solr.SolrNamedSource;

import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpNameFinder;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordPosTagger;

/**
 * The main YodaQA pipeline.
 *
 * This is an aggregate AE that will run all stages of a pipeline
 * that takes a fresh QuestionCAS on its input (from a collection
 * reader) and produces a AnswerHitlistCAS with final answer ranking
 * on its output (for the consumer). */

public class YodaQA /* XXX: extends AggregateBuilder ? */ {
	static {
		/* Enable IPv6 usage (if available). */
		IPv6Check.enableIPv6IfItWorks();

		try {
			/* We have two options here - either use a local embedded
			 * instance based on a solr index in a given local directory,
			 * or connect to a remote instance (e.g. indexing Wikipedia).
			 *
			 * By default, we connect to a remote instance; see README
			 * for instructions on how to set up your own.  Uncomment
			 * the guten line below and comment the enwiki one to use
			 * a local solr core instead, again see README for
			 * instructions on how to obtain an example one. */

			//SolrNamedSource.register("guten", "data/guten", null);
			SolrNamedSource.register("enwiki", "collection1", "http://enwiki.ailao.eu:8983/solr/");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("*** Exception caught during SolrNamedSource initialization. ***");
			System.err.println("You will get a fatal NullPointerException later, but the issue is above.");
		}

		/* Enable sharing of models for various NLP components. */
		System.setProperty("dkpro.core.resourceprovider.sharable." + LanguageToolSegmenter.class.getName(), "true");
		System.setProperty("dkpro.core.resourceprovider.sharable." + LanguageToolLemmatizer.class.getName(), "true");
		System.setProperty("dkpro.core.resourceprovider.sharable." + StanfordParser.class.getName(), "true");
		System.setProperty("dkpro.core.resourceprovider.sharable." + StanfordPosTagger.class.getName(), "true");
		System.setProperty("dkpro.core.resourceprovider.sharable." + OpenNlpNameFinder.class.getName(), "true");
	}

	public static AnalysisEngineDescription createEngineDescription() throws ResourceInitializationException {
		/* Our multi-phase logic is that
		 *
		 * (i) If a load_answer*fvs property is set, we skip all
		 *     the previous phases
		 * (ii) If a save_answer*fvs property is set, we wrap up
		 *      that phase with a scoring step and stop early
		 *
		 * Specifying multiple load or multiple save properties
		 * triggers undefined behavior. */

		AggregateBuilder builder = new AggregateBuilder();

		boolean outputsNewCASes = buildPipeline(builder);

		builder.setFlowControllerDescription(
				FlowControllerFactory.createFlowControllerDescription(
					FixedFlowController.class,
					FixedFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().setName("cz.brmlab.yodaqa.pipeline.YodaQA");
		if (outputsNewCASes)
			aed.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
		return aed;
	}

	/** Build a multi-phase pipeline, possibly skipping some phases
	 * based on property setting. Returns whether the pipeline outputs
	 * new CASes. */
	protected static boolean buildPipeline(AggregateBuilder builder) throws ResourceInitializationException {
		boolean outputsNewCASes = false;

		String answerSaveDir = System.getProperty("cz.brmlab.yodaqa.save_answerfvs");
		boolean answerSaveDo = answerSaveDir != null && !answerSaveDir.isEmpty();
		String answerLoadDir = System.getProperty("cz.brmlab.yodaqa.load_answerfvs");
		boolean answerLoadDo = answerLoadDir != null && !answerLoadDir.isEmpty();
		String answer1SaveDir = System.getProperty("cz.brmlab.yodaqa.save_answer1fvs");
		boolean answer1SaveDo = answer1SaveDir != null && !answer1SaveDir.isEmpty();
		String answer1LoadDir = System.getProperty("cz.brmlab.yodaqa.load_answer1fvs");
		boolean answer1LoadDo = answer1LoadDir != null && !answer1LoadDir.isEmpty();
		String answer2SaveDir = System.getProperty("cz.brmlab.yodaqa.save_answer2fvs");
		boolean answer2SaveDo = answer2SaveDir != null && !answer2SaveDir.isEmpty();
		String answer2LoadDir = System.getProperty("cz.brmlab.yodaqa.load_answer2fvs");
		boolean answer2LoadDo = answer2LoadDir != null && !answer2LoadDir.isEmpty();

		int loadPhase = -1;
		if (answerLoadDo) loadPhase = 0;
		else if (answer1LoadDo) loadPhase = 1;
		else if (answer2LoadDo) loadPhase = 2;

		/* First stage - question analysis, generating answers,
		 * and basic analysis. */
		if (loadPhase < 0) {
			System.err.println("0");
			outputsNewCASes = true;

			AnalysisEngineDescription questionAnalysis = QuestionAnalysisAE.createEngineDescription();
			builder.add(questionAnalysis);

			AnalysisEngineDescription answerProducer = createAnswerProducerDescription();
			builder.add(answerProducer);

			AnalysisEngineDescription answerAnalysis = AnswerAnalysisAE.createEngineDescription();
			builder.add(answerAnalysis);

			AnalysisEngineDescription answerCASMerger = AnalysisEngineFactory.createEngineDescription(
					AnswerCASMerger.class,
					AnswerCASMerger.PARAM_ISLAST_BARRIER, 7,
					AnswerCASMerger.PARAM_PHASE, 0,
					ParallelEngineFactory.PARAM_NO_MULTIPROCESSING, 1);
			builder.add(answerCASMerger);

		/* (Serialization / scoring point #0.) */
		} else if (loadPhase == 0) {
			System.err.println("l0");
			AnalysisEngineDescription answerDeserialize = AnalysisEngineFactory.createEngineDescription(
					AnswerHitlistDeserialize.class,
					AnswerHitlistDeserialize.PARAM_LOAD_DIR, answerLoadDir);
			builder.add(answerDeserialize);
		}
		if (answerSaveDo) {
			AnalysisEngineDescription answerSerialize = AnalysisEngineFactory.createEngineDescription(
					AnswerHitlistSerialize.class,
					AnswerHitlistSerialize.PARAM_SAVE_DIR, answerSaveDir);
			builder.add(answerSerialize);
		}
		if (loadPhase <= 0) {
			AnalysisEngineDescription answerScoring = AnswerScoringAE.createEngineDescription("");
			builder.add(answerScoring);
		}
		if (answerSaveDo)
			return outputsNewCASes;


		/* Next stage - initial scoring, and pruning all but the top N
		 * answers; this should help us differentiate better selection
		 * with most of the noisy low quality answers wed out. Also
		 * merge textually equivalent answers. */
		if (loadPhase < 1) {
			System.err.println("1");
			//outputsNewCASes = true;

			/* Convert top N AnswerHitlist entries back to separate CASes,
			 * then back to a hitlist, to get rid of the rest.  This is a bit
			 * convoluted, but easiest dirty way. */
			AnalysisEngineDescription answerCASSplitter = AnalysisEngineFactory.createEngineDescription(
					AnswerCASSplitter.class,
					AnswerCASSplitter.PARAM_TOPLISTLEN, 25,
					AnswerCASSplitter.PARAM_HITLIST_EMIT, false);
			builder.add(answerCASSplitter);

			AnalysisEngineDescription answerCASMerger = AnalysisEngineFactory.createEngineDescription(
					AnswerCASMerger.class,
					AnswerCASMerger.PARAM_ISLAST_BARRIER, 1,
					AnswerCASMerger.PARAM_HITLIST_REUSE, false,
					AnswerCASMerger.PARAM_PHASE, 1,
					ParallelEngineFactory.PARAM_NO_MULTIPROCESSING, 1);
			builder.add(answerCASMerger);

			/* XXX: Move the following to a separate scoring phase
			 * so that we already capture the single correct answer
			 * scoring preference. */

			/* Merge textually equivalent answers. */
			AnalysisEngineDescription answerTextMerger = AnalysisEngineFactory.createEngineDescription(
					AnswerTextMerger.class);
			builder.add(answerTextMerger,
				CAS.NAME_DEFAULT_SOFA, "AnswerHitlist");

			/* Diffuse scores between textually similar answers. */
			AnalysisEngineDescription evidenceDiffusion = AnalysisEngineFactory.createEngineDescription(
					EvidenceDiffusion.class);
			builder.add(evidenceDiffusion,
				CAS.NAME_DEFAULT_SOFA, "AnswerHitlist");

		/* (Serialization / scoring point #1.) */
		} else if (loadPhase == 1) {
			System.err.println("l1");
			AnalysisEngineDescription answerDeserialize = AnalysisEngineFactory.createEngineDescription(
					AnswerHitlistDeserialize.class,
					AnswerHitlistDeserialize.PARAM_LOAD_DIR, answer1LoadDir);
			builder.add(answerDeserialize);
		}
		if (answer1SaveDo) {
			AnalysisEngineDescription answerSerialize = AnalysisEngineFactory.createEngineDescription(
					AnswerHitlistSerialize.class,
					AnswerHitlistSerialize.PARAM_SAVE_DIR, answer1SaveDir);
			builder.add(answerSerialize);
		}
		if (loadPhase <= 1) {
			AnalysisEngineDescription answerScoring = AnswerScoringAE.createEngineDescription("1");
			builder.add(answerScoring);
		}
		if (answer1SaveDo)
			return outputsNewCASes;


		/* Next stage - initial scoring, evidence gathering */
		if (loadPhase < 2) {
			System.err.println("2");
			outputsNewCASes = true;

			/* Convert top N AnswerHitlist entries back to separate CASes;
			 * the original hitlist with the rest of questions is also
			 * still passed through.. */
			AnalysisEngineDescription answerCASSplitter = AnalysisEngineFactory.createEngineDescription(
					AnswerCASSplitter.class);
			builder.add(answerCASSplitter);

			AnalysisEngineDescription answerEvidencing = AnswerEvidencingAE.createEngineDescription();
			builder.add(answerEvidencing);

			AnalysisEngineDescription answerCASMerger = AnalysisEngineFactory.createEngineDescription(
					AnswerCASMerger.class,
					AnswerCASMerger.PARAM_ISLAST_BARRIER, 1,
					AnswerCASMerger.PARAM_HITLIST_REUSE, true,
					AnswerCASMerger.PARAM_PHASE, 2,
					ParallelEngineFactory.PARAM_NO_MULTIPROCESSING, 1);
			builder.add(answerCASMerger);

		/* (Serialization / scoring point #2.) */
		} else if (loadPhase == 2) {
			System.err.println("l2");
			AnalysisEngineDescription answerDeserialize = AnalysisEngineFactory.createEngineDescription(
					AnswerHitlistDeserialize.class,
					AnswerHitlistDeserialize.PARAM_LOAD_DIR, answer2LoadDir);
			builder.add(answerDeserialize);
		}
		if (answer2SaveDo) {
			AnalysisEngineDescription answerSerialize = AnalysisEngineFactory.createEngineDescription(
					AnswerHitlistSerialize.class,
					AnswerHitlistSerialize.PARAM_SAVE_DIR, answer2SaveDir);
			builder.add(answerSerialize);
		}
		if (loadPhase <= 2) {
			AnalysisEngineDescription answerScoring = AnswerScoringAE.createEngineDescription("2");
			builder.add(answerScoring);
		}
		return outputsNewCASes;
	}


	public static AnalysisEngineDescription createAnswerProducerDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		/* The AEs below are all run "in parallel" - not necessarily
		 * runtime wise, but logically wise with regards to CAS
		 * multiplication, they all get the QuestionCAS and produce
		 * CandidateAnswerCASes. */

		/* Since each of these CAS multipliers will eventually produce
		 * a single CAS marked as "isLast", if you add another one
		 * here, you must also bump the AnswerCASMerger parameter
		 * PARAM_ISLAST_BARRIER. */

		/* Structured search: */
		AnalysisEngineDescription dbpOnt = DBpediaOntologyAnswerProducer.createEngineDescription();
		builder.add(dbpOnt);
		AnalysisEngineDescription dbpProp = DBpediaPropertyAnswerProducer.createEngineDescription();
		builder.add(dbpProp);
		AnalysisEngineDescription fbOnt = FreebaseOntologyAnswerProducer.createEngineDescription();
		builder.add(fbOnt);

		/* Full-text search: */
		/* XXX: These aggregates have "Solr" in name but do not
		 * necessarily use just Solr, e.g. Bing. */
		AnalysisEngineDescription solrFull = SolrFullAnswerProducer.createEngineDescription();
		builder.add(solrFull); /* This one is worth 3 isLasts. */
		AnalysisEngineDescription solrDoc = SolrDocAnswerProducer.createEngineDescription();
		builder.add(solrDoc);

		builder.setFlowControllerDescription(
				FlowControllerFactory.createFlowControllerDescription(
					FixedParallelFlowController.class,
					FixedParallelFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().setName("cz.brmlab.yodaqa.pipeline.YodaQA.AnswerProducer");
		aed.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
		return aed;
	}
}
