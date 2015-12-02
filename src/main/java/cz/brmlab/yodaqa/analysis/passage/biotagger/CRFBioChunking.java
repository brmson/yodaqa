package cz.brmlab.yodaqa.analysis.passage.biotagger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.chunking.BioChunking;

import com.github.jcrfsuite.util.Pair;

import cz.brmlab.yodaqa.model.SearchResult.AnswerBioMention;
import cz.brmlab.yodaqa.provider.crf.CRFTagging;

public class CRFBioChunking<SUB_CHUNK_TYPE extends Annotation, CHUNK_TYPE extends AnswerBioMention>
		extends BioChunking<SUB_CHUNK_TYPE, CHUNK_TYPE> {

	public CRFBioChunking(
			Class<? extends SUB_CHUNK_TYPE> subChunkClass,
			Class<? extends CHUNK_TYPE> chunkClass) {
		super(subChunkClass, chunkClass, null);
	}

	public CRFBioChunking(
			Class<? extends SUB_CHUNK_TYPE> subChunkClass,
			Class<? extends CHUNK_TYPE> chunkClass,
			String featureName) {
		super(subChunkClass, chunkClass, featureName);
	}

	/** Like super.createChunks(), but using CRFTagging instead.
	 * XXX: Maybe use the ClearTk .score() calling convention? */
	public List<CHUNK_TYPE> createChunks(
			JCas jCas,
			List<SUB_CHUNK_TYPE> subChunks,
			CRFTagging outcomes) throws AnalysisEngineProcessException {
		// validate parameters
		int nSubChunks = subChunks.size();
		int nOutcomes = outcomes.size();
		if (nSubChunks != nOutcomes) {
			String message = "expected the same number of sub-chunks (%d) as outcome s(%d)";
			throw new IllegalArgumentException(String.format(message, nSubChunks, nOutcomes));
		}

		// get the Feature object if we need to assign an attribute
		Feature feature;
		if (this.featureFullName == null) {
			feature = null;
		} else {
			feature = jCas.getTypeSystem().getFeatureByFullName(this.featureFullName);
		}

		// create chunk annotations as appropriate for the outcomes
		List<CHUNK_TYPE> chunks = new ArrayList<CHUNK_TYPE>();
		for (int i = 0; i < outcomes.size(); ++i) {
			Pair<String, Double> outcome = outcomes.get(i);

			// if we're at the beginning of a chunk, gather outcomes until we hit the end of the chunk
			// (a chunk ends when we hit 'O' or when the label change, e.g. I-PER I-ORG)
			if (outcome.first.charAt(0) != 'O') {
				double score = 0;

				// advance to the end of this chunk
				int begin = i;
				int end = i;
				while (true) {
					Pair<String, Double> curr = outcomes.get(end);
					score += curr.second;
					if (end + 1 >= outcomes.size())
						break;
					Pair<String, Double> next = outcomes.get(end + 1);
					if (this.isEndOfChunk(curr.first.charAt(0), curr.first.substring(1),
								next.first.charAt(0), next.first.substring(1))) {
						break;
					}
					++end;
				}

				score /= (end - begin + 1);

				// skip over all the outcomes we just consumed
				i = end;

				// convert the outcome indexes into CAS offsets
				begin = subChunks.get(begin).getBegin();
				end = subChunks.get(end).getEnd();

				// chop off trailing interpunction
				while (end > begin &&
					(jCas.getDocumentText().charAt(end-1) == '.'
					 || jCas.getDocumentText().charAt(end-1) == ',')) {
					// System.err.println(begin + "," + end + " --- " + jCas.getDocumentText().substring(begin, end));
					end--;
				}

				// construct the chunk annotation
				Constructor<? extends CHUNK_TYPE> constructor;
				try {
					constructor = this.chunkClass.getConstructor(JCas.class, int.class, int.class);
				} catch (NoSuchMethodException e) {
					throw new AnalysisEngineProcessException(e);
				}
				CHUNK_TYPE chunk;
				try {
					chunk = constructor.newInstance(jCas, begin, end);
				} catch (InstantiationException e) {
					throw new AnalysisEngineProcessException(e);
				} catch (IllegalAccessException e) {
					throw new AnalysisEngineProcessException(e);
				} catch (InvocationTargetException e) {
					throw new AnalysisEngineProcessException(e);
				}

				// set the annotation feature if necessary
				if (this.featureFullName != null) {
					chunk.setFeatureValueFromString(feature, outcome.first.substring(1));
				}

				// set the score feature
				chunk.setScore(score);

				// add the chunk to the CAS and to the result list
				chunk.addToIndexes();
				chunks.add(chunk);
			}
		}
		return chunks;
	}
};
