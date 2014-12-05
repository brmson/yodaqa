package cz.brmlab.yodaqa.analysis.answer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
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

	private static final String MODEL_RES = "AnswerScoreLogistic.model";

	public double weights[];
	public double intercept;

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		weights = new double[AnswerFV.labels.length * 3];

		/* Load and parse the model. */
		InputStream model_stream = AnswerScoreLogistic.class.getResourceAsStream(MODEL_RES);
		BufferedReader model = new BufferedReader(new InputStreamReader(model_stream));
		String line;
		int i = 0;
		while (true) {
			try {
				line = model.readLine();
			} catch (IOException e) {
				throw new ResourceInitializationException(e);
			}
			if (line == null)
				break;
			if (line.equals("") || line.matches("^\\s*//.*"))
				continue;
			line = line.replaceAll("\\s*/\\*.*?\\*/\\s*", "");
			if (i < weights.length) {
				while (!line.equals("")) {
					String line1 = line.replaceAll(",.*$", "");
					weights[i] = Double.parseDouble(line1);
					// logger.debug("{} {}, line {}", i, weights[i], line);
					line = line.replaceAll("^.*?,\\s*", "");
					i++;
				}
			} else if (i == weights.length) {
				intercept = Double.parseDouble(line);
				// logger.debug("i {}", intercept);
				i++;
			} else {
				throw new ResourceInitializationException(); // "Too many data in the model " + MODEL_RES
			}
		}
		if (i <= weights.length)
			throw new ResourceInitializationException(); // "Too little data in the model " + MODEL_RES
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
