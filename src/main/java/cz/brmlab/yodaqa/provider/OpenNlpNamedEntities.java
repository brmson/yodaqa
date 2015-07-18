package cz.brmlab.yodaqa.provider;

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
					SyncOpenNlpNameFinder.class,
					SyncOpenNlpNameFinder.PARAM_VARIANT, variant));
		}

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().setName("cz.brmlab.yodaqa.provider.OpenNlpNamedEntities");
		return aed;
	}

	/** Convert a given NamedEntity value feature (ne.getValue())
	 * to the most appropriate WordNet synset. */
	public static long neValueToSynset(String value)
	{
		if (value.equals("date")) {
			/* (23){15184543} <noun.time>[28] S: (n) date#1 (date%1:28:00::), day of the month#1 (day_of_the_month%1:28:00::) (the specified day of the month) "what is the date today?" */
			return 15184543;
		} else if (value.equals("location")) {
			/* (992){00027365} <noun.Tops>[03] S: (n) location#1 (location%1:03:00::) (a point or extent in space) */
			return 27365;
		} else if (value.equals("money")) {
			/* (77){13405730} <noun.possession>[21] S: (n) money#1 (money%1:21:00::) (the most common medium of exchange; functions as legal tender) "we tried to collect the money he owed us" */
			return 13405730;
		} else if (value.equals("organization")) {
			/* (29){08024893} <noun.group>[14] S: (n) organization#1 (organization%1:14:00::), organisation#2 (organisation%1:14:00::) (a group of people who work together) */
			return 8024893;
		} else if (value.equals("percentage")) {
			/* {13837954} <noun.relation>[24] S: (n) proportion#1 (proportion%1:24:00::) (the quotient obtained when the magnitude of a part is divided by the magnitude of the whole) */
			/* (percentage is too specific when asking for e.g. "rate" etc.) */
			return 13837954;
		} else if (value.equals("person")) {
			/* (6833){00007846} <noun.Tops>[03] S: (n) person#1 (person%1:03:00::), individual#1 (individual%1:03:00::), someone#1 (someone%1:03:00::), somebody#1 (somebody%1:03:00::), mortal#1 (mortal%1:03:00::), soul#2 (soul%1:03:00::) (a human being) "there was too much for one person to do" */
			return 7846;
		} else if (value.equals("time")) {
			/* (7){15154879} <noun.time>[28] S: (n) clock time#1 (clock_time%1:28:00::), time#7 (time%1:28:03::) (a reading of a point in time as given by a clock) "do you know what time it is?"; "the time is 10 o'clock" */
			return 15154879;
		} else {
			assert(false);
			return 0;
		}
	}
}
