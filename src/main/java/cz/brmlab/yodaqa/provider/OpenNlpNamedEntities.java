package cz.brmlab.yodaqa.provider;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpNameFinder;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;

public class OpenNlpNamedEntities {
	public static AnalysisEngineDescription createEngineDescription()
		throws ResourceInitializationException {

		AggregateBuilder builder = new AggregateBuilder();

		String[] ner_variants = {
			"date", "location", "money", "organization",
			"percentage", "person", "time"
		};
		for (String variant : ner_variants) {
			builder.add(AnalysisEngineFactory.createEngineDescription(
					OpenNlpNameFinder.class,
					OpenNlpNameFinder.PARAM_VARIANT, variant));
		}

		return builder.createAggregateDescription();
	}
}
