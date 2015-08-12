package cz.brmlab.yodaqa.pipeline.solrdoc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import cz.brmlab.yodaqa.flow.dashboard.AnswerIDGenerator;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSource;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceEnwiki;
import cz.brmlab.yodaqa.flow.dashboard.Question;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.flow.dashboard.SourceIDGenerator;
import cz.brmlab.yodaqa.flow.dashboard.snippet.AnsweringDocTitle;
import cz.brmlab.yodaqa.flow.dashboard.snippet.SnippetIDGenerator;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.component.JCasMultiplier_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.flow.asb.MultiThreadASB;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerResource;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
import cz.brmlab.yodaqa.provider.solr.Solr;
import cz.brmlab.yodaqa.provider.solr.SolrNamedSource;
import cz.brmlab.yodaqa.provider.solr.SolrQuerySettings;
import cz.brmlab.yodaqa.provider.solr.SolrTerm;

/**
 * From the QuestionCAS, generate a bunch of CandidateAnswerCAS
 * instances.  In this case, we submit an in-text Solr query
 * for "document search", looking for the most relevant documents
 * to the set of clues and creating CandidateAnswers from the
 * document titles. */

public class SolrDocPrimarySearch extends JCasMultiplier_ImplBase {
	final Logger logger = LoggerFactory.getLogger(SolrDocPrimarySearch.class);

	/** Number of results to grab and analyze. */
	public static final String PARAM_HITLIST_SIZE = "hitlist-size";
	@ConfigurationParameter(name = PARAM_HITLIST_SIZE, mandatory = false, defaultValue = "20")
	protected int hitListSize;

	/** Number and baseline distance of gradually desensitivized
	 * proximity searches. Total of proximity-num optional search
	 * terms are included, covering proximity-base-dist * #of terms
	 * neighborhood. For each proximity term, the coverage is
	 * successively multiplied by proximity-base-factor; initial weight
	 * is sum of individual weights and is successively halved. */
	public static final String PARAM_PROXIMITY_NUM = "proximity-num";
	@ConfigurationParameter(name = PARAM_PROXIMITY_NUM, mandatory = false, defaultValue = "3")
	protected int proximityNum;
	public static final String PARAM_PROXIMITY_BASE_DIST = "proximity-base-dist";
	@ConfigurationParameter(name = PARAM_PROXIMITY_BASE_DIST, mandatory = false, defaultValue = "2")
	protected int proximityBaseDist;
	public static final String PARAM_PROXIMITY_BASE_FACTOR = "proximity-base-factor";
	@ConfigurationParameter(name = PARAM_PROXIMITY_BASE_FACTOR, mandatory = false, defaultValue = "3")
	protected int proximityBaseFactor;

	protected SolrQuerySettings settings = null;
	protected String srcName;
	protected Solr solr;

	protected JCas questionView;
	protected Iterator<SolrDocument> docIter;
	protected int i;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		/* Eew... well, for now, we just expect that only a single
		 * Solr source has been registered and grab that one,
		 * whatever its name (allows easy enwiki/guten switching). */
		this.srcName = (String) SolrNamedSource.nameSet().toArray()[0];
		this.solr = SolrNamedSource.get(srcName);

		this.settings = new SolrQuerySettings(proximityNum, proximityBaseDist, proximityBaseFactor,
				new String[]{""}, true);
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		questionView = jcas;

		SolrDocumentList documents;
		try {
			Collection<Clue> clues = JCasUtil.select(questionView, Clue.class);
			Collection<SolrTerm> terms = SolrTerm.cluesToTerms(clues);
			documents = solr.runQuery(terms, hitListSize, settings, logger);
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
		docIter = documents.iterator();
		i = 0;
	}


	@Override
	public boolean hasNext() throws AnalysisEngineProcessException {
		return docIter.hasNext() || i == 0;
	}

	@Override
	public AbstractCas next() throws AnalysisEngineProcessException {
		SolrDocument doc = docIter.hasNext() ? docIter.next() : null;
		i++;

		JCas jcas = getEmptyJCas();
		try {
			jcas.createView("Question");
			JCas canQuestionView = jcas.getView("Question");
			copyQuestion(questionView, canQuestionView);

			jcas.createView("Answer");
			JCas canAnswerView = jcas.getView("Answer");
			if (doc != null) {
				documentToAnswer(canAnswerView, doc, !docIter.hasNext() ? i : 0, questionView);
			} else {
				dummyAnswer(canAnswerView, i);
			}
		} catch (Exception e) {
			jcas.release();
			throw new AnalysisEngineProcessException(e);
		}
		return jcas;
	}

	protected void copyQuestion(JCas src, JCas dest) throws Exception {
		CasCopier copier = new CasCopier(src.getCas(), dest.getCas());
		copier.copyCasView(src.getCas(), dest.getCas(), true);
	}

	protected void documentToAnswer(JCas jcas, SolrDocument doc,
			int isLast, JCas questionView) throws Exception {
		Integer id = (Integer) doc.getFieldValue("id");
		String title = (String) doc.getFieldValue("titleText");
		logger.info(" FOUND: " + id + " " + (title != null ? title : ""));

		/* Chop any trailing (...) substring (e.g. Foo (disambiguation)
		 * to just Foo).  N.B. it might be tempting to use this as
		 * a LAT, but it might not be a type; e.g.
		 * Socialist Party(France), ...  so at most this should be
		 * a weak LAT signal. */
		jcas.setDocumentText(title.replaceAll("\\s+\\([^)]*\\)\\s*$", ""));
		jcas.setDocumentLanguage("en"); // XXX

		AnswerSourceEnwiki ac = new AnswerSourceEnwiki(AnswerSourceEnwiki.ORIGIN_DOCUMENT, (title != null ? title : ""), id);
		int sourceID = QuestionDashboard.getInstance().get(questionView).storeAnswerSource(ac);
		AnsweringDocTitle adt = new AnsweringDocTitle(SnippetIDGenerator.getInstance().generateID(), sourceID);
		QuestionDashboard.getInstance().get(questionView).addSnippet(adt);

		ResultInfo ri = new ResultInfo(jcas);
		ri.setDocumentId(id.toString());
		ri.setDocumentTitle(title);
		ri.setSource(srcName);
		ri.setRelevance(((Float) doc.getFieldValue("score")).floatValue());
		ri.setIsLast(isLast);
		ri.setSourceID(sourceID);
		ri.setOrigin("cz.brmlab.yodaqa.pipeline.solrdoc.SolrDocPrimarySearch");
		/* XXX: We ignore ansfeatures as we generate just
		 * a single answer here. */
		ri.addToIndexes();

		AnswerFV fv = new AnswerFV();
		fv.setFeature(AF.Occurences, 1.0);
		fv.setFeature(AF.ResultRR, 1 / ((float) i));
		fv.setFeature(AF.ResultLogScore, Math.log(1 + ri.getRelevance()));
		fv.setFeature(AF.OriginDocTitle, 1.0);

		AnswerResource ar = new AnswerResource(jcas);
		ar.setIri("http://en.wikipedia.org/wiki/" + title.replace(" ", "_"));
		ar.addToIndexes();
		ArrayList<AnswerResource> ars = new ArrayList<>();
		ars.add(ar);

		AnswerInfo ai = new AnswerInfo(jcas);
		ai.setFeatures(fv.toFSArray(jcas));
		ai.setResources(FSCollectionFactory.createFSArray(jcas, ars));
		ai.setIsLast(1);
		ai.setSnippetIDs(new IntegerArray(jcas, 1));
		ai.setSnippetIDs(0, adt.getSnippetID());
		ai.setAnswerID(AnswerIDGenerator.getInstance().generateID());
 		ai.addToIndexes();
	}

	protected void dummyAnswer(JCas jcas, int isLast) throws Exception {
		/* We will just generate a single dummy CAS
		 * to avoid flow breakage. */
		jcas.setDocumentText("");
		jcas.setDocumentLanguage("en"); // XXX

		ResultInfo ri = new ResultInfo(jcas);
		ri.setDocumentTitle("");
		ri.setOrigin("cz.brmlab.yodaqa.pipeline.solrdoc.SolrDocPrimarySearch");
		ri.setIsLast(isLast);
		ri.addToIndexes();

		AnswerInfo ai = new AnswerInfo(jcas);
		ai.setIsLast(1);
		ai.addToIndexes();

	}

	@Override
	public int getCasInstancesRequired() {
		return MultiThreadASB.maxJobs * 2;
	}
}
