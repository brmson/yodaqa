package cz.brmlab.yodaqa.pipeline.esdoc;

import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.flow.dashboard.AnswerIDGenerator;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSource;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceAguAbstract;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.flow.dashboard.snippet.AnsweringDocTitle;
import cz.brmlab.yodaqa.flow.dashboard.snippet.SnippetIDGenerator;
import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.Question.CluePhrase;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerInfo;
import cz.brmlab.yodaqa.model.CandidateAnswer.AnswerResource;
import cz.brmlab.yodaqa.model.SearchResult.ResultInfo;
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
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;

import java.util.*;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

public class EsDocPrimarySearch extends JCasMultiplier_ImplBase {

    private Logger logger;

    protected JCas questionView;

    //protected Node esNode;

    protected Client esClient;
    protected Iterator<SearchHit> results = Collections.emptyIterator();

    @ConfigurationParameter(name = "es.cluster.name", mandatory = false, defaultValue = "elasticsearch_szednik")
    protected String esClusterName;

    @ConfigurationParameter(name = "es.index", mandatory = false, defaultValue = "zen")
    protected String esIndex;

    @ConfigurationParameter(name = "es.type", mandatory = false, defaultValue = "abstract")
    protected String esType;

    @ConfigurationParameter(name = "hitlist-size", mandatory = false, defaultValue = "20")
    protected int hitListSize;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        logger = context.getLogger();

        try {
            logger.log(Level.INFO, "connecting to elasticsearch : " + esClusterName);

            Settings settings = ImmutableSettings.settingsBuilder()
                    .put("cluster.name", esClusterName).build();

            esClient = new TransportClient(settings)
                    .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

            //esNode = nodeBuilder().clusterName(esClusterName).client(true).node();
            //esClient = esNode.client();

            logger.log(Level.INFO, "connected to elasticsearch");

        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if(esClient != null) {
            esClient.close();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

        Collection<Clue> clues = JCasUtil.select(aJCas, Clue.class);
        String[] terms = cluesToTerms(clues);

        logger.log(Level.INFO, "querying elasticsearch for: "+Arrays.toString(terms));

        SearchResponse response = esClient
                .prepareSearch(esIndex)
                .setTypes(esType)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.termsQuery("multi", terms))
                .setFrom(0).setSize(hitListSize).setExplain(true)
                .execute()
                .actionGet();

        logger.log(Level.INFO, "elasticsearch response status: "+response.status().name());

        results = response.getHits().iterator();
    }

    @Override
    public boolean hasNext() throws AnalysisEngineProcessException {
        return results.hasNext();
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {

        logger.log(Level.INFO, "");

        if(!results.hasNext()) { return null; }

        SearchHit hit = results.next();

        JCas jcas = getEmptyJCas();

        try {
            jcas.createView("Question");
            JCas canQuestionView = jcas.getView("Question");
            copyQuestion(questionView, canQuestionView);

            jcas.createView("Answer");
            JCas canAnswerView = jcas.getView("Answer");

            if (hit != null) {
                documentToAnswer(canAnswerView, hit, questionView);
            } else {
                emptyAnswer(canAnswerView);
            }
        } catch (Exception e) {
            jcas.release();
            throw new AnalysisEngineProcessException(e);
        }
        return jcas;

    }

    static protected ResultInfo emptyResultInfo(JCas jcas) {
        ResultInfo ri = new ResultInfo(jcas);
        ri.setDocumentTitle("");
        ri.setOrigin("cz.brmlab.yodaqa.pipeline.esdoc.EsDocPrimarySearch");
        return ri;
    }

    static protected AnswerInfo emptyAnswerInfo(JCas jcas) {
        AnswerInfo ai = new AnswerInfo(jcas);
//        ai.setIsLast(1);
        return ai;
    }

    protected void emptyAnswer(JCas jcas) {
        jcas.setDocumentText("");
        jcas.setDocumentLanguage("en");
        emptyAnswerInfo(jcas);
        emptyResultInfo(jcas);
    }

    protected void documentToAnswer(JCas jcas, SearchHit doc, JCas questionView) throws AnalysisEngineProcessException {

        String id = doc.getId();
        String title = doc.field("title").getValue().toString();
        title = (title != null) ? title.trim() : "";

        String uri = doc.field("uri").getValue().toString();

        float score = doc.getScore();

        logger.log(Level.FINER, "FOUND: "+ id + " " + title);

        jcas.setDocumentText(title.replaceAll("\\s+\\([^)]*\\)\\s*$", ""));
        jcas.setDocumentLanguage("en");

        AnswerSource ac = new AnswerSourceAguAbstract(AnswerSourceAguAbstract.ORIGIN_DOCUMENT, title, id);
        int sourceID = QuestionDashboard.getInstance().get(questionView).storeAnswerSource(ac);

        AnsweringDocTitle adt = new AnsweringDocTitle(SnippetIDGenerator.getInstance().generateID(), sourceID);
        QuestionDashboard.getInstance().get(questionView).addSnippet(adt);

        ResultInfo ri = new ResultInfo(jcas);
        ri.setDocumentId(id);
        ri.setDocumentTitle(title);
        ri.setRelevance(score);
        ri.setSource(esClusterName);
        ri.setSourceID(sourceID);
        ri.setOrigin("cz.brmlab.yodaqa.pipeline.esdoc.EsDocPrimarySearch");
//        ri.setIsLast(isLast);
        ri.addToIndexes();

        AnswerFV fv = new AnswerFV();
        fv.setFeature(AF.Occurences, 1.0);
//        fv.setFeature(AF.ResultRR, 1 / ((float) index));
        fv.setFeature(AF.ResultLogScore, Math.log(1 + ri.getRelevance()));
        fv.setFeature(AF.OriginDocTitle, 1.0);

        AnswerResource ar = new AnswerResource(jcas);
        ar.setIri(uri);
        ar.addToIndexes();
        ArrayList<AnswerResource> ars = new ArrayList<>();
        ars.add(ar);

        AnswerInfo ai = new AnswerInfo(jcas);
        ai.setFeatures(fv.toFSArray(jcas));
        ai.setResources(FSCollectionFactory.createFSArray(jcas, ars));
//        ai.setIsLast(1);
        ai.setSnippetIDs(new IntegerArray(jcas, 1));
        ai.setSnippetIDs(0, adt.getSnippetID());
        ai.setAnswerID(AnswerIDGenerator.getInstance().generateID());
        ai.addToIndexes();
    }

    protected static void copyQuestion(JCas src, JCas dest) throws Exception {
        CasCopier copier = new CasCopier(src.getCas(), dest.getCas());
        copier.copyCasView(src.getCas(), dest.getCas(), true);
    }

    protected static String[] cluesToTerms(Collection<Clue> clues) {
        List<String> terms = new ArrayList<String>(clues.size());
        for (Clue clue : clues) {
            if (clue instanceof CluePhrase)
                continue;
            terms.add(clue.getLabel());
        }
        return terms.toArray(new String[terms.size()]);
    }
}
