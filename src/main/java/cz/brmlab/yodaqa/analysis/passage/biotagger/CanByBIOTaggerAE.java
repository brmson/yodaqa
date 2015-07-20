package cz.brmlab.yodaqa.analysis.passage.biotagger;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkSequenceAnnotator;
import org.cleartk.ml.crfsuite.CrfSuiteStringOutcomeDataWriter;
import org.cleartk.ml.jar.DefaultSequenceDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generate CandidateAnswer annotations of picked passage sentences
 * based on a token sequence annotator tagging tokens with B-I-O labels.
 *
 * This is a short aggregate AE; we cannot easily use a single annotators
 * as it would have to extend both CleartkSequenceAnnotator and
 * CandidateGenerator.  The finegrainedness also gives some illusion
 * of modularity. */

public class CanByBIOTaggerAE /* XXX: extends AggregateBuilder ? */ {
	final static Logger logger = LoggerFactory.getLogger(CanByBIOTaggerAE.class);

	public static AnalysisEngineDescription createEngineDescription() throws ResourceInitializationException {
		String trainAnsBioCRF_s = System.getProperty("cz.brmlab.yodaqa.train_ansbiocrf");
		boolean trainAnsBioCRF = trainAnsBioCRF_s != null && !trainAnsBioCRF_s.isEmpty();
		String modelDir = "data/ml/biocrf"; // XXX; also hardcoded in provider/crf/CRFSuite.java

		AggregateBuilder builder = new AggregateBuilder();

		/* Pre-generate AnswerBioMentions during tagger training. */
		if (trainAnsBioCRF)
			builder.add(AnalysisEngineFactory.createEngineDescription(GSAnsBioMention.class));

		/* Run the tagger that will generate AnswerBioMentions
		 * based on a bunch of features and a model, or prepare
		 * data for model training. */
		builder.add(AnalysisEngineFactory.createEngineDescription(BIOTaggerCRF.class,
			CleartkSequenceAnnotator.PARAM_IS_TRAINING, trainAnsBioCRF,
			GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				JarClassifierBuilder.getModelJarFile(modelDir),
			DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY, modelDir,
			DefaultSequenceDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
				CrfSuiteStringOutcomeDataWriter.class
		));

		/* Generate CandidateAnswers. */
		builder.add(AnalysisEngineFactory.createEngineDescription(CanByAnsBioMention.class));

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().setName("cz.brmlab.yodaqa.analysis.passage.biotagger.CanByBIOTaggerAE");
		return aed;
	}
}
