package cz.brmlab.yodaqa.analysis.ansevid;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.component.CasDumpWriter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.FindReqParse;
import cz.brmlab.yodaqa.analysis.tycor.LATByWordnet;
import cz.brmlab.yodaqa.analysis.tycor.LATMatchTyCor;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;

/**
 * Annotate the CandidateAnswerCAS with expensive features.
 *
 * This is an aggregate AE that will run a variety of potentially
 * expensive annotators on the CandidateAnswerCAS.
 *
 * N.B. the AnswerHitlistCAS also enters this AE!  All the components
 * should just pass it through (typically).
 *
 * XXX: So far, we did not manage to find a mode of answer confirmation
 * that would be beneficial, this is just performance intensive but
 * completely noisy.  Therefore, THIS PIPELINE PHASE DOES NOT DO ANYTHING
 * at this point.  Having some more useful search-based answer verification
 * is subject of further research. */

public class AnswerEvidencingAE /* XXX: extends AggregateBuilder ? */ {
	final static Logger logger = LoggerFactory.getLogger(AnswerEvidencingAE.class);

	public static AnalysisEngineDescription createEngineDescription() throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();

		/* Mark all answers that even *enter* this AE with
		 * a special feature. */
		builder.add(createPrimitiveDescription(AnswerTopMarker.class));

		/* Run a fulltext search for each answer + question clues and
		 * measure the number of hits. */
		builder.add(createPrimitiveDescription(SolrHitsCounter.class));


		/* Some debug dumps of the intermediate CAS. */
		if (false) {//logger.isDebugEnabled()) {
			builder.add(createPrimitiveDescription(
				CasDumpWriter.class,
				CasDumpWriter.PARAM_OUTPUT_FILE, "/tmp/yodaqa-aecas.txt"));
		}

		AnalysisEngineDescription aed = builder.createAggregateDescription();
		aed.getAnalysisEngineMetaData().setName("cz.brmlab.yodaqa.analysis.ansevid.AnswerEvidencingAE");
		return aed;
	}
}
