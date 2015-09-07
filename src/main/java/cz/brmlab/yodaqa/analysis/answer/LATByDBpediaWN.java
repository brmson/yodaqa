package cz.brmlab.yodaqa.analysis.answer;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.Question.Focus;
import cz.brmlab.yodaqa.model.TyCor.DBpLAT;
import cz.brmlab.yodaqa.model.TyCor.DBpWNLAT;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.provider.rdf.DBpediaWNTypes;

/**
 * Generate LAT annotations in a CandidateAnswerCAS. This is based on the
 * answer focus (or NamedEntity) which is looked up in DBpedia and LATs
 * based on dbpedia2:wordnet_type entries are generated. */

public class LATByDBpediaWN extends LATByDBpedia {
	final Logger logger = LoggerFactory.getLogger(LATByDBpediaWN.class);

	final DBpediaWNTypes dbt = new DBpediaWNTypes();

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		dbt.initialize();
		fallbackOnly = false;
	}

	protected boolean addLATByLabel(JCas jcas, Focus focus, String label) throws AnalysisEngineProcessException {
		StringBuilder typelist = new StringBuilder();

		List<String> types = dbt.query(label, logger);

		for (String type : types) {
			addLATFeature(jcas, AF.LATDBpWNType);
			String[] typeE = type.split("/");
			addTypeLAT(jcas, new DBpWNLAT(jcas), focus, typeE[0], Long.parseLong(typeE[1]), typelist);

			/* Override any DBpLAT with the same text. */
			List<LAT> latrm = new ArrayList<LAT>();
			for (LAT dbplat : JCasUtil.select(jcas, DBpLAT.class)) {
				if (dbplat.getText().toLowerCase().equals(typeE[0].toLowerCase())) {
					latrm.add(dbplat);
				}
			}
			for (LAT lat : latrm) {
				logger.debug("overriding DBpLAT {}", lat.getText());
				lat.removeFromIndexes();
			}
		}

		if (typelist.length() > 0) {
			if (focus != null)
				logger.debug(".. Focus {} => DBpedia WN LATs {}", focus.getCoveredText(), typelist);
			else
				logger.debug(".. Ans {} => DBpedia WN LATs {}", label, typelist);
			return true;
		} else {
			return false;
		}
	}
}
