package cz.brmlab.yodaqa.analysis.ansscore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;

/**
 * Annotate the AnswerHitlistCAS Answer FSes with score based on the
 * present AnswerFeatures.  This particular implementation uses
 * the estimated probability of the answer being correct as determined
 * by logistic regression based classifier trained on the training set
 * using the data/ml/train-answer.py script. */


public class AnswerScoreLogistic extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(AnswerScoreLogistic.class);

	/**
	 * Pipeline phase in which we are scoring.  We may be scoring
	 * multiple times and will use different models.
	 */
	public static final String PARAM_SCORING_PHASE = "SCORING_PHASE";
	@ConfigurationParameter(name = PARAM_SCORING_PHASE, mandatory = true)
	protected String scoringPhase;

	protected String modelName;

	public double weights[];
	public double intercept;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		weights = new double[AnswerFV.labels.length * 3];

		modelName = "AnswerScoreLogistic" + scoringPhase + ".model";

		/* Load and parse the model. */
		try {
			loadModel(AnswerScoreLogistic.class.getResourceAsStream(modelName));
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
		logger.debug("model " + modelName + " i " + intercept);
	}

	protected void loadModel(InputStream model_stream) throws Exception {
		BufferedReader model = new BufferedReader(new InputStreamReader(model_stream));
		String line;
		while (true) {
			line = model.readLine();
			if (line == null)
				break;
			if (line.equals("") || line.matches("^\\s*//.*"))
				continue;

			// Load feature name from the line: ^\t/*      originConceptBySubject @,%,! */
			Matcher m = Pattern.compile("^\t/\\* *([^ ]*) ").matcher(line);
			line = line.replaceAll("\\s*/\\*.*?\\*/\\s*", "");

			if (!m.find()) {
				// The intercept!
				intercept = Double.parseDouble(line);
				// logger.debug("i {}", intercept);
				continue;
			}

			String label = m.group(1);
			int i = AnswerFV.featureIndex(label) * 3;

			if (i < 0) {
				logger.debug("WARNING: Ignoring unknown feature {}", label);
				continue;
			}

			while (!line.equals("")) {
				String line1 = line.replaceAll(",.*$", "");
				weights[i] = Double.parseDouble(line1);
				// logger.debug("{} {}, line {}", i, weights[i], line);
				line = line.replaceAll("^.*?,\\s*", "");
				i++;
			}
		}
	}

	protected class AnswerScore {
		public Answer a;
		public double score;

		public AnswerScore(Answer a_, double score_) {
			a = a_;
			score = score_;
		}
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		logger.debug("scoring with model {}", modelName);

		AnswerStats astats = new AnswerStats(jcas);
		List<AnswerScore> answers = new LinkedList<AnswerScore>();

		for (Answer a : JCasUtil.select(jcas, Answer.class)) {
			AnswerFV fv = new AnswerFV(a, astats);

			double t = intercept;
			double fvec[] = fv.getFV();
			for (int i = 0; i < fvec.length; i++) {
				t += fvec[i] * weights[i];
			}

			double prob = 1.0 / (1.0 + Math.exp(-t));
			answers.add(new AnswerScore(a, prob));
		}

		/* Reindex the touched answer info(s). */
		for (AnswerScore as : answers) {
			as.a.removeFromIndexes();
			as.a.setConfidence(as.score);
			as.a.addToIndexes();
		}
	}
}
