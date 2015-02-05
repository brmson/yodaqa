package cz.brmlab.yodaqa.pipeline.dbpedia;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.component.JCasMultiplier_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.analysis.passextract.PassByClue;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_LATDBpRelation;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_Occurences;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_OriginDBpRelation;
import cz.brmlab.yodaqa.model.CandidateAnswer.AF_ResultLogScore;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.Question.ClueConcept;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
import cz.brmlab.yodaqa.model.TyCor.DBpRelationLAT;
import cz.brmlab.yodaqa.model.TyCor.LAT;
import cz.brmlab.yodaqa.provider.rdf.DBpediaOntology;
import cz.brmlab.yodaqa.provider.rdf.DBpediaProperties;

/* XXX: The clue-specific features, ugh. */
import cz.brmlab.yodaqa.model.Question.*;
import cz.brmlab.yodaqa.model.CandidateAnswer.*;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.NN;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;

/**
 * From the QuestionCAS, generate a bunch of CandidateAnswerCAS
 * instances.  In this case, we generate answers from DBpedia
 * ontology relations. */

public class DBpediaRelationPrimarySearch extends JCasMultiplier_ImplBase {
	final Logger logger = LoggerFactory.getLogger(DBpediaRelationPrimarySearch.class);

	final DBpediaOntology dbo = new DBpediaOntology();
	final DBpediaProperties dbp = new DBpediaProperties();

	protected JCas questionView;
	protected Iterator<DBpediaOntology.PropertyValue> relIter;
	protected int i;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		questionView = jcas;

		List<DBpediaOntology.PropertyValue> properties = new ArrayList<DBpediaOntology.PropertyValue>();

		for (ClueConcept concept : JCasUtil.select(questionView, ClueConcept.class)) {
			/* Query the DBpedia ontology dataset - cleaned up
			 * but quite sparse infobox based data. */
			properties.addAll(dbo.query(concept.getLabel(), logger));
			/* Query the DBpedia raw infobox dataset - uncleaned
			 * but depnse infobox based data. */
			/* We disable this again for the time being because
			 * this dataset really is awful:
			 *
			 * (i) It actually is not complete; e.g. the <Sun>
			 * resource is missing most of the infobox stuff.
			 * Also, many values like dates are stripped to just
			 * day-of-month number or such.
			 *
			 * (ii) It contains too much junk that is not relevant
			 * as it is not presented to the user.  For example
			 * image alts, alignments and links.  Various
			 * properties like "date" for city are actually not
			 * pertaining to the entity but just some part of the
			 * infobox that describes e.g. the leader, and such
			 * segmentation is lost.
			 *
			 * A useful raw infobox dataset needs to carry labels
			 * *as they are shown to the user*, and needs to
			 * actually render the infobox template internally to
			 * determine any grouping and representation of data! */
			// properties.addAll(dbp.query(concept.getLabel(), logger));
		}

		/* Actually consider only properties that have some plausible
		 * relation to the question. */
		properties = filterProperties(properties, questionView);

		relIter = properties.iterator();
		i = 0;
	}


	@Override
	public boolean hasNext() throws AnalysisEngineProcessException {
		return relIter.hasNext() || i == 0;
	}

	@Override
	public AbstractCas next() throws AnalysisEngineProcessException {
		DBpediaOntology.PropertyValue property = relIter.hasNext() ? relIter.next() : null;

		JCas jcas = getEmptyJCas();
		try {
			jcas.createView("Question");
			JCas canQuestionView = jcas.getView("Question");
			copyQuestion(questionView, canQuestionView);

			jcas.createView("Answer");
			JCas canAnswerView = jcas.getView("Answer");
			if (property != null) {
				propertyToAnswer(canAnswerView, property, !relIter.hasNext(), questionView);
			} else {
				dummyAnswer(canAnswerView);
			}
		} catch (Exception e) {
			jcas.release();
			throw new AnalysisEngineProcessException(e);
		}
		i++;
		return jcas;
	}

	protected List<DBpediaOntology.PropertyValue> filterProperties(List<DBpediaOntology.PropertyValue> src, JCas questionView) {
		List<DBpediaOntology.PropertyValue> dst = new ArrayList<>();
		for (DBpediaOntology.PropertyValue p : src) {
			/* Keep this property if its name matches some
			 * question clue. */
			String pName = p.getProperty().toLowerCase();
			for (Clue clue : JCasUtil.select(questionView, Clue.class)) {
				String cText = clue.getCoveredText().toLowerCase();
				String cLabel = clue.getLabel().toLowerCase();
				// XXX: word boundaries?
				if (pName.contains(cText) || pName.contains(cLabel)
				    || cText.contains(pName) || cLabel.contains(pName)) {
					/* We have a match! */
					dst.add(p);
					break;
				}
			}
			// no-match logging is not necessary as DBpediaOntology
			// logs found stuff
		}
		return dst;
	}

	protected void copyQuestion(JCas src, JCas dest) throws Exception {
		CasCopier copier = new CasCopier(src.getCas(), dest.getCas());
		copier.copyCasView(src.getCas(), dest.getCas(), true);
	}

	protected void propertyToAnswer(JCas jcas, DBpediaOntology.PropertyValue property,
			boolean isLast, JCas questionView) throws Exception {
		logger.info(" FOUND: {} {}", property.getProperty(), property.getValue());

		jcas.setDocumentText(property.getValue());
		jcas.setDocumentLanguage("en"); // XXX

		String title = property.getObject() + " " + property.getProperty();

		ResultInfo ri = new ResultInfo(jcas);
		ri.setDocumentTitle(title);
		ri.setSource("DBpedia");
		ri.setRelevance(1.0);
		ri.setIsLast(isLast);
		ri.setOrigin("cz.brmlab.yodaqa.pipeline.dbpedia.DBpediaRelationPrimarySearch");
		/* XXX: We ignore ansfeatures as we generate just
		 * a single answer here. */
		ri.addToIndexes();

		AnswerFV fv = new AnswerFV();
		fv.setFeature(AF_Occurences.class, 1.0);
		fv.setFeature(AF_ResultLogScore.class, Math.log(1 + ri.getRelevance()));
		fv.setFeature(AF_OriginDBpRelation.class, 1.0);

		/* Mark by concept-clue-origin AFs. */
		// XXX: Carry the clue reference in property object.
		for (ClueConcept concept : JCasUtil.select(questionView, ClueConcept.class)) {
			if (!concept.getLabel().toLowerCase().equals(property.getObject().toLowerCase()))
				continue;
			// We don't set this since all our clues have concept origin
			//afv.setFeature(AF_OriginConcept.class, 1.0);
			if (concept.getBySubject())
				fv.setFeature(AF_OriginConceptBySubject.class, 1.0);
			if (concept.getByLAT())
				fv.setFeature(AF_OriginConceptByLAT.class, 1.0);
			if (concept.getByNE())
				fv.setFeature(AF_OriginConceptByNE.class, 1.0);
		}

		/* Match clues in relation name (esp. LAT or Focus clue
		 * will be nice). */
		boolean clueMatched = false;
		for (Clue clue : JCasUtil.select(questionView, Clue.class)) {
			if (!property.getProperty().matches(PassByClue.getClueRegex(clue)))
				continue;
			clueMatched = true;
			clueAnswerFeatures(fv, clue);
		}
		if (!clueMatched)
			fv.setFeature(AF_OriginDBpRNoClue.class, -1.0);

		/* Generate also a LAT for the answer right away. */
		addTypeLAT(jcas, fv, property.getProperty());
		if (property.getProperty().contains(" ")) {
			/* If the property name is multi-word, well,
			 * we may not handle multi-word LATs very well
			 * so also generate a single-word LAT with the
			 * first word ("KNOWN for", "AREA total", ...). */
			addTypeLAT(jcas, fv, property.getProperty().replaceFirst(" .*$", ""));
		}

		AnswerInfo ai = new AnswerInfo(jcas);
		ai.setFeatures(fv.toFSArray(jcas));
		ai.setIsLast(isLast);
		ai.addToIndexes();
	}

	protected void dummyAnswer(JCas jcas) throws Exception {
		/* We will just generate a single dummy CAS
		 * to avoid flow breakage. */
		jcas.setDocumentText("");
		jcas.setDocumentLanguage("en"); // XXX

		ResultInfo ri = new ResultInfo(jcas);
		ri.setDocumentTitle("");
		ri.setOrigin("cz.brmlab.yodaqa.pipeline.dbpedia.DBpediaRelationPrimarySearch");
		ri.setIsLast(true);
		ri.addToIndexes();

		AnswerInfo ai = new AnswerInfo(jcas);
		ai.setIsLast(true);
		ai.addToIndexes();
	}

	protected LAT titleContainsLAT(String title, JCas questionView) {
		LAT bestQlat = null;
		for (LAT qlat : JCasUtil.select(questionView, LAT.class)) {
			String text = qlat.getText().toLowerCase();
			String textalt = qlat.getCoveredText().toLowerCase();

			if (!title.toLowerCase().contains(text) && !title.toLowerCase().contains(textalt))
				continue;

			/* We have a match! But keep just the
			 * most specific LAT match within the
			 * passage. */
			if (bestQlat != null && bestQlat.getSpecificity() > qlat.getSpecificity())
				continue;
			bestQlat = qlat;
		}
		return bestQlat;
	}

	protected void addTypeLAT(JCas jcas, AnswerFV fv, String type) throws AnalysisEngineProcessException {
		fv.setFeature(AF_LATDBpRelation.class, 1.0);

		String ntype = type.toLowerCase();

		/* We have a synthetic noun(-ish), synthetize
		 * a POS tag for it. */
		int len = jcas.getDocumentText().length();
		POS pos = new NN(jcas);
		pos.setBegin(0);
		pos.setEnd(len);
		pos.setPosValue("NNS");
		pos.addToIndexes();

		addLAT(new DBpRelationLAT(jcas), 0, len, null, ntype, pos, 0, 0.0);
	}

	protected void addLAT(LAT lat, int begin, int end, Annotation base, String text, POS pos, long synset, double spec) {
		lat.setBegin(begin);
		lat.setEnd(end);
		lat.setBase(base);
		lat.setPos(pos);
		lat.setText(text);
		lat.setSpecificity(spec);
		lat.setSynset(synset);
		lat.addToIndexes();
	}

	protected void clueAnswerFeatures(AnswerFV afv, Clue clue) {
		     if (clue instanceof ClueToken     ) afv.setFeature(AF_OriginDBpRClueToken.class, 1.0);
		else if (clue instanceof CluePhrase    ) afv.setFeature(AF_OriginDBpRCluePhrase.class, 1.0);
		else if (clue instanceof ClueSV        ) afv.setFeature(AF_OriginDBpRClueSV.class, 1.0);
		else if (clue instanceof ClueNE        ) afv.setFeature(AF_OriginDBpRClueNE.class, 1.0);
		else if (clue instanceof ClueLAT       ) afv.setFeature(AF_OriginDBpRClueLAT.class, 1.0);
		else if (clue instanceof ClueSubject   ) {
			afv.setFeature(AF_OriginDBpRClueSubject.class, 1.0);
			     if (clue instanceof ClueSubjectNE) afv.setFeature(AF_OriginDBpRClueSubjectNE.class, 1.0);
			else if (clue instanceof ClueSubjectToken) afv.setFeature(AF_OriginDBpRClueSubjectToken.class, 1.0);
			else if (clue instanceof ClueSubjectPhrase) afv.setFeature(AF_OriginDBpRClueSubjectPhrase.class, 1.0);
			else assert(false);
		} else if (clue instanceof ClueConcept ) {
			afv.setFeature(AF_OriginDBpRClueConcept.class, 1.0);
			ClueConcept concept = (ClueConcept) clue;
			if (concept.getBySubject())
				afv.setFeature(AF_OriginDBpRClueSubject.class, 1.0);
			if (concept.getByLAT())
				afv.setFeature(AF_OriginDBpRClueLAT.class, 1.0);
			if (concept.getByNE())
				afv.setFeature(AF_OriginDBpRClueNE.class, 1.0);
		}
		else assert(false);
	}
}
