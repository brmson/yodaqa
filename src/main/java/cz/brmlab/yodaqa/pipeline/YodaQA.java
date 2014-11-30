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
		AggregateBuilder builder = new AggregateBuilder();

		AnalysisEngineDescription questionAnalysis = QuestionAnalysisAE.createEngineDescription();
		builder.add(questionAnalysis);

		AnalysisEngineDescription answerProducer = createAnswerProducerDescription();
		builder.add(answerProducer);

		AnalysisEngineDescription answerAnalysis = AnswerAnalysisAE.createEngineDescription();
		builder.add(answerAnalysis);

		AnalysisEngineDescription answerMergeAndScore = createAnswerMergeAndScoreDescription();
		builder.add(answerMergeAndScore);

		builder.setFlowControllerDescription(
				FlowControllerFactory.createFlowControllerDescription(
					FixedFlowController.class,
					FixedFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
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

	public static AnalysisEngineDescription createAnswerMergeAndScoreDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		AnalysisEngineDescription answerMerger = AnalysisEngineFactory.createEngineDescription(
				AnswerMerger.class,
				AnswerMerger.PARAM_ISLAST_BARRIER, 4);
		builder.add(answerMerger);

		AnalysisEngineDescription answerScoring = AnswerScoringAE.createEngineDescription();
		builder.add(answerScoring);

		builder.setFlowControllerDescription(
				FlowControllerFactory.createFlowControllerDescription(
					FixedFlowController.class,
					FixedFlowController.PARAM_ACTION_AFTER_CAS_MULTIPLIER, "drop"));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().getOperationalProperties().setOutputsNewCASes(true);
		return aed;
	}
}
