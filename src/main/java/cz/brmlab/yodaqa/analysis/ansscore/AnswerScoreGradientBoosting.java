package cz.brmlab.yodaqa.analysis.ansscore;

import com.google.gson.Gson;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by honza on 5.7.15.
 */
public class AnswerScoreGradientBoosting extends JCasAnnotator_ImplBase {
    private class Tree {
        public List<Integer> children_right;
        public List<Integer> children_left;
        public List<Integer> features;
        public List<Double> thresholds;
        public List<Double> values;
        public Tree() {
            children_right = new ArrayList<Integer>();
            children_left = new ArrayList<Integer>();
            features = new ArrayList<Integer>();
            thresholds = new ArrayList<Double>();
            values = new ArrayList<Double>();
        }
    }

    private class Model {
        public double prior;
        public double learning_rate;
        public List<Tree> forest;
    }


    final Logger logger = LoggerFactory.getLogger(AnswerScoreGradientBoosting.class);

    /**
     * Pipeline phase in which we are scoring.  We may be scoring
     * multiple times and will use different models.
     */
    public static final String PARAM_SCORING_PHASE = "SCORING_PHASE";
    @ConfigurationParameter(name = PARAM_SCORING_PHASE, mandatory = true)
    protected String scoringPhase;

    protected String modelName;

    public Model model;

    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        modelName = "AnswerScoreGradientBoosting.model";

		/* Load and parse the model. */
        try {
            loadModel(AnswerScoreGradientBoosting.class.getResourceAsStream(modelName));
        } catch (Exception e) {
            throw new ResourceInitializationException(e);
        }
        logger.debug("model " + modelName);
    }

    protected void loadModel(InputStream model_stream) throws Exception {
        Gson gson = new Gson();
        BufferedReader br = new BufferedReader(new InputStreamReader(model_stream));
        model = gson.fromJson(br, Model.class);
    }

    protected class AnswerScore {
        public Answer a;
        public double score;

        public AnswerScore(Answer a_, double score_) {
            a = a_;
            score = score_;
        }
    }

    private double classifyWithOneTree(double[] fvec, Tree tree, int node) {
        int fidx = tree.features.get(node);
        if (fidx < 0) return tree.values.get(node);
        double threshold = tree.thresholds.get(node);
        if (fvec[fidx] <= threshold) return classifyWithOneTree(fvec, tree, tree.children_left.get(node));
        else return classifyWithOneTree(fvec, tree, tree.children_right.get(node));
    }

    public void process(JCas jcas) throws AnalysisEngineProcessException {
        logger.debug("scoring with model {}", modelName);

        AnswerStats astats = new AnswerStats(jcas);
        List<AnswerScore> answers = new LinkedList<AnswerScore>();

        for (Answer a : JCasUtil.select(jcas, Answer.class)) {
            AnswerFV fv = new AnswerFV(a, astats);

            double fvec[] = fv.getFV();
            double res = model.prior;
            for(Tree t: model.forest) {
                res += model.learning_rate*classifyWithOneTree(fvec, t, 0);
            }
            res = (1.0/(1 + Math.exp(-res)));
            answers.add(new AnswerScore(a, res));
        }

		/* Reindex the touched answer info(s). */
        for (AnswerScore as : answers) {
            as.a.removeFromIndexes();
            as.a.setConfidence(as.score);
            as.a.addToIndexes();
        }
    }
}
