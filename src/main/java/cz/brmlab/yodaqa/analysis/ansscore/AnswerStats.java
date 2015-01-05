package cz.brmlab.yodaqa.analysis.ansscore;

import java.util.LinkedList;
import java.util.List;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;

/**
 * Aggregate statistics on feature vectors of all hitlist'd answers.
 * Basically, for each feature we track the mean and SD, which is used
 * for producing rescaled versions of the features. */


public class AnswerStats {
	public double mean[], sd[];

	public AnswerStats(JCas jcas) {
		mean = new double[AnswerFV.labels.length];
		sd = new double[AnswerFV.labels.length];

		loadAnswers(jcas);
	}

	public void loadAnswers(JCas jcas) {
		List<double[]> values = new LinkedList<double[]>();
		for (Answer a : JCasUtil.select(jcas, Answer.class)) {
			AnswerFV fv = new AnswerFV(a);
			values.add(fv.getValues());
		}

		/* Compute mean. */
		for (double val[] : values)
			for (int i = 0; i < mean.length; i++)
				mean[i] += val[i];
		for (int i = 0; i < mean.length; i++) {
			// System.err.println("i " + i + " sum " + mean[i]);
			mean[i] /= values.size();
		}

		/* Compute SD. */
		for (double val[] : values)
			for (int i = 0; i < sd.length; i++)
				sd[i] += Math.pow(val[i] - mean[i], 2);
		for (int i = 0; i < sd.length; i++) {
			sd[i] = Math.sqrt(sd[i] / values.size());
			// System.err.println("i " + i + " mean " + mean[i] + " sd " + sd[i]);
		}
	}
}
