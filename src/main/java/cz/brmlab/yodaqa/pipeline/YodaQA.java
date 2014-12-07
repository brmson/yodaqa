package cz.brmlab.yodaqa.pipeline;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.FlowControllerFactory;
import org.apache.uima.flow.impl.FixedFlowController;
import org.apache.uima.resource.ResourceInitializationException;

import cz.brmlab.yodaqa.analysis.answer.AnswerAnalysisAE;
import cz.brmlab.yodaqa.analysis.answer.AnswerScoringAE;
import cz.brmlab.yodaqa.analysis.question.QuestionAnalysisAE;
import cz.brmlab.yodaqa.flow.FixedParallelFlowController;
import cz.brmlab.yodaqa.pipeline.solrdoc.SolrDocAnswerProducer;
import cz.brmlab.yodaqa.pipeline.solrfull.SolrFullAnswerProducer;
import cz.brmlab.yodaqa.pipeline.dbpedia.DBpediaRelationAnswerProducer;
import cz.brmlab.yodaqa.pipeline.AnswerHitlistSerialize;
import cz.brmlab.yodaqa.provider.solr.SolrNamedSource;

import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolSegmenter;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpNameFinder;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordParser;

/**
 * The main YodaQA pipeline.
 *
 * This is an aggregate AE that will run all stages of a pipeline
 * that takes a fresh QuestionCAS on its input (from a collection
 * reader) and produces a AnswerHitlistCAS with final answer ranking
 * on its output (for the consumer). */

public class YodaQA /* XXX: extends AggregateBuilder ? */ {
	static {
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
			SolrNamedSource.register("enwiki", "collection1", "http://pasky.or.cz:8983/solr/");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("*** Exception caught during SolrNamedSource initialization. ***");
			System.err.println("You will get a fatal NullPointerException later, but the issue is above.");
		}

		/* Enable sharing of models for various NLP components. */
		System.setProperty("dkpro.core.resourceprovider.sharable." + LanguageToolSegmenter.class.getName(), "true");
		System.setProperty("dkpro.core.resourceprovider.sharable." + LanguageToolLemmatizer.class.getName(), "true");
		System.setProperty("dkpro.core.resourceprovider.sharable." + StanfordParser.class.getName(), "true");
		System.err.println("dkpro.core.resourceprovider.sharable." + StanfordParser.class.getName());
		System.setProperty("dkpro.core.resourceprovider.sharable." + OpenNlpNameFinder.class.getName(), "true");
	}

	public static AnalysisEngineDescription createEngineDescription() throws ResourceInitializationException {
		String answerSaveDir = System.getProperty("cz.brmlab.yodaqa.save_answerfvs");
		boolean answerSaveDo = answerSaveDir != null && !answerSaveDir.isEmpty();
		String answerLoadDir = System.getProperty("cz.brmlab.yodaqa.load_answerfvs");
		boolean answerLoadDo = answerLoadDir != null && !answerLoadDir.isEmpty();
		String answer1SaveDir = System.getProperty("cz.brmlab.yodaqa.save_answer1fvs");
		boolean answer1SaveDo = answer1SaveDir != null && !answer1SaveDir.isEmpty();
		String answer1LoadDir = System.getProperty("cz.brmlab.yodaqa.load_answer1fvs");
		boolean answer1LoadDo = answer1LoadDir != null && !answer1LoadDir.isEmpty();
		System.err.println("a1sd" + answer1SaveDo + " " + answer1SaveDir);

		AggregateBuilder builder = new AggregateBuilder();

		/* First stage - question analysis, generating answers,
		 * and basic analysis. */
		if (!answerLoadDo && !answer1LoadDo) {
			AnalysisEngineDescription questionAnalysis = QuestionAnalysisAE.createEngineDescription();
			builder.add(questionAnalysis);

			AnalysisEngineDescription answerProducer = createAnswerProducerDescription();
			builder.add(answerProducer);

			AnalysisEngineDescription answerAnalysis = AnswerAnalysisAE.createEngineDescription();
			builder.add(answerAnalysis);

			AnalysisEngineDescription answerMerger = AnalysisEngineFactory.createEngineDescription(
					AnswerMerger.class,
					AnswerMerger.PARAM_ISLAST_BARRIER, 4);
			builder.add(answerMerger);

			if (answerSaveDo) {
				AnalysisEngineDescription answerSerialize = AnalysisEngineFactory.createEngineDescription(
						AnswerHitlistSerialize.class,
						AnswerHitlistSerialize.PARAM_SAVE_DIR, answerSaveDir);
				builder.add(answerSerialize);
			}
		} else if (!answer1LoadDo) {
			AnalysisEngineDescription answerDeserialize = AnalysisEngineFactory.createEngineDescription(
					AnswerHitlistDeserialize.class,
					AnswerHitlistDeserialize.PARAM_LOAD_DIR, answerLoadDir);
			builder.add(answerDeserialize);
		}

		/* Next stage - initial scoring, evidence gathering */
		if (!answer1LoadDo) {
			AnalysisEngineDescription answerScoring = AnswerScoringAE.createEngineDescription();
			builder.add(answerScoring);

			/* Convert top N AnswerHitlist entries back to separate CASes;
			 * the original hitlist with the rest of questions is also
			 * still passed through.. */
			AnalysisEngineDescription answerSplitter = AnalysisEngineFactory.createEngineDescription(
					AnswerSplitter.class);
			builder.add(answerSplitter);

			AnalysisEngineDescription answerEvidencer = createAnswerEvidencerDescription();
			builder.add(answerEvidencer);

			AnalysisEngineDescription answerMerger = AnalysisEngineFactory.createEngineDescription(
					AnswerMerger.class,
					AnswerMerger.PARAM_ISLAST_BARRIER, 1,
					AnswerMerger.PARAM_HITLIST_REUSE, true);
			builder.add(answerMerger);

			if (answer1SaveDo) {
				AnalysisEngineDescription answerSerialize = AnalysisEngineFactory.createEngineDescription(
						AnswerHitlistSerialize.class,
						AnswerHitlistSerialize.PARAM_SAVE_DIR, answer1SaveDir);
				builder.add(answerSerialize);
			}
		} else {
			AnalysisEngineDescription answerDeserialize = AnalysisEngineFactory.createEngineDescription(
					AnswerHitlistDeserialize.class,
					AnswerHitlistDeserialize.PARAM_LOAD_DIR, answer1LoadDir);
			builder.add(answerDeserialize);
		}

		/* Next stage - final scoring */
		if (true) {
			AnalysisEngineDescription answerScoring = AnswerScoringAE.createEngineDescription();
			builder.add(answerScoring);
		}

		builder.setFlowControllerDescription(
				FlowControllerFactory.createFlowControllerDescription(
					FixedFlowController.class,
					FixedFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		if (!answerLoadDo)
			aed.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
		return aed;
	}

	public static AnalysisEngineDescription createAnswerProducerDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		/* The AEs below are all run "in parallel" - not necessarily
		 * runtime wise, but logically wise with regards to CAS
		 * multiplication, they all get the QuestionCAS and produce
		 * CandidateAnswerCASes. */

		/* Since each of these CAS multipliers will eventually produce
		 * a single CAS marked as "isLast", if you add another one
		 * here, you must also bump the AnswerMerger parameter
		 * PARAM_ISLAST_BARRIER. */

		AnalysisEngineDescription dbpRel = DBpediaRelationAnswerProducer.createEngineDescription();
		builder.add(dbpRel);

		AnalysisEngineDescription solrFull = SolrFullAnswerProducer.createEngineDescription();
		builder.add(solrFull);

		AnalysisEngineDescription solrDoc = SolrDocAnswerProducer.createEngineDescription();
		builder.add(solrDoc);

		builder.setFlowControllerDescription(
				FlowControllerFactory.createFlowControllerDescription(
					FixedParallelFlowController.class,
					FixedParallelFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
		return aed;
	}

	public static AnalysisEngineDescription createAnswerEvidencerDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		/* The AEs below are all run "in parallel" - not necessarily
		 * runtime wise. */

		/* N.B. We want to ignore the AnswerHitlistCAS in these
		 * annotators. */

		/* TODO */

		builder.setFlowControllerDescription(
				FlowControllerFactory.createFlowControllerDescription(
					FixedParallelFlowController.class,
					FixedParallelFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		//aed.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
		return aed;
	}
}
