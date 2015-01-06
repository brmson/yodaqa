package cz.brmlab.yodaqa.analysis.answer;

import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATDBpWNType;
import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.TyCor.DBpWNLAT;
import cz.brmlab.yodaqa.provider.rdf.DBpediaWNTypes;

/**
 * Generate LAT annotations in a CandidateAnswerCAS. This is based on the
 * answer focus (or NamedEntity) which is looked up in DBpedia and LATs
 * based on dbpedia2:wordnet_type entries are generated. */

public class LATByDBpediaWN extends LATByDBpedia {
	final Logger logger = LoggerFactory.getLogger(LATByDBpediaWN.class);

	final DBpediaWNTypes dbt = new DBpediaWNTypes();

	protected boolean addLATByLabel(JCas jcas, Focus focus, String label) throws AnalysisEngineProcessException {
		StringBuilder typelist = new StringBuilder();

		List<String> types = dbt.query(label, logger);

		for (String type : types) {
			addLATFeature(jcas, AF_LATDBpWNType.class);
			addTypeLAT(jcas, new DBpWNLAT(jcas), focus, type, typelist);
		}

		if (typelist.length() > 0) {
			if (focus != null)
				logger.debug(".. Focus {} => DBpedia WN LATs/0 {}", focus.getCoveredText(), typelist);
			else
				logger.debug(".. Ans {} => DBpedia WN LATs/0 {}", label, typelist);
			return true;
		} else {
			return false;
		}
	}
}
