package cz.brmlab.yodaqa.analysis.passage.biotagger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cleartk.ml.Feature;

import approxlib.distance.Edit;
import approxlib.distance.Edit.TYPE;
import approxlib.distance.EditDist;
import approxlib.tree.LblTree;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/** Cleartk token feature generator based on an edit tree script.
 * It precomputes various indices for a specific edit script, then
 * produces results per token (tree node) index.
 *
 * XXX: Maybe there is a more proper way to do this within Cleartk
 * feature extractors framework, but I can't be bothered to find out
 * as I also feel kind of mad at Cleartk right now. */

public class EditFeatureGenerator {
	protected EditDist editDist;
	/* Pre-computed index mapping token sequence ids to tree post-order
	 * ids (for lookup in parse tree), to question words in case of
	 * alignment (otherwise no entry) and edit types (or no entry in
	 * case of alignment). */
	protected Map<Integer, Integer> postOrderMapping = new HashMap<>();
	protected Map<Integer, Integer> questionPOMapping = new HashMap<>();
	protected Map<Integer, TYPE> editTypeMapping = new HashMap<>();
	protected int maxWOIdx = 0;

	public EditFeatureGenerator(EditDist editDist) {
		this.editDist = editDist;
		preindex(editDist);
	}

	protected void preindex(EditDist editDist) {
		/* Process edits. */
		ArrayList<Edit> edits = editDist.getEditList();
		for (Edit edit : edits) {
			/* An edit can be either a deletion or a rename.
			 * And we ignore renames, they can be handled
			 * when recording alignment. */
			TYPE editType = edit.getType();
			if (!(editType == TYPE.DEL || editType == TYPE.DEL_SUBTREE || editType == TYPE.DEL_LEAF))
				continue;

			/* Get the token identifiers. */
			int postOrderIdx = edit.getArgs()[0];
			int wordOrderIdx = editDist.id2idxInWordOrder1(postOrderIdx);

			postOrderMapping.put(wordOrderIdx, postOrderIdx);
			editTypeMapping.put(wordOrderIdx, editType);
			if (wordOrderIdx > maxWOIdx)
				maxWOIdx = wordOrderIdx;
		}

		/* Process non-edits or just renames (alignment). */
		HashMap<Integer, Integer> alignment = editDist.getAlign1to2();
		for (Integer postOrderIdx : alignment.keySet()) {
			int questionPOIdx = alignment.get(postOrderIdx);
			int wordOrderIdx = editDist.id2idxInWordOrder1(postOrderIdx);
			int questionWOIdx = editDist.id2idxInWordOrder2(questionPOIdx);
			assert(wordOrderIdx >= 0 && questionWOIdx >= 0);

			postOrderMapping.put(wordOrderIdx, postOrderIdx);
			questionPOMapping.put(wordOrderIdx, questionPOIdx);
			if (wordOrderIdx > maxWOIdx)
				maxWOIdx = wordOrderIdx;
		}
	}

	/** Get word order index of the nearest aligned token to the token
	 * with a given index. */
	protected Integer getNearestAligned(int fromIdx) {
		// XXX: Preindex this
		Integer bestAligned = null, best_d = 99999;
		for (int i = 0; i < maxWOIdx; i++) {
			if (!questionPOMapping.containsKey(i))
				continue;
			/* Aligned. */
			if (bestAligned == null || Math.abs(fromIdx - i) < best_d) {
				best_d = Math.abs(fromIdx - i);
				bestAligned = i;
			}
		}
		return bestAligned;
	}

	public Collection<Feature> extract(List<Feature> featuresSoFar, int wordOrderIdx, Token t, LblTree aTree, LblTree qTree) {
		List<Feature> features = new ArrayList<Feature>();

		if (editTypeMapping.containsKey(wordOrderIdx)) {
			/* Edit */
			String editType = editTypeMapping.get(wordOrderIdx).toString();
			features.add(new Feature("edit", editType));
			features.add(new Feature("edit(" + editType + ")_Pos", t.getPos().getPosValue()));
			for (Feature f : featuresSoFar) {
				if (f.getName().equals("Dep")) {
					features.add(new Feature("edit(" + editType + ")_Dep", f.getValue()));
				} else if (f.getName().equals("NE")) {
					features.add(new Feature("edit(" + editType + ")_NE", f.getValue()));
				}
			}
		}

		/* XXX: This is not an else branch as we apparently
		 * sometimes generate both edit and align.  FIXME
		 * investigate why */
		if (questionPOMapping.containsKey(wordOrderIdx)) {
			/* Rename or full alignment. */
			// XXX use editDist.get*() consistently?
			int questionPO = questionPOMapping.get(wordOrderIdx);
			features.add(new Feature("edit", "align"));
			features.add(new Feature("edit(align)_0Pos", t.getPos().getPosValue()));
			String passageDep = "";
			for (Feature f : featuresSoFar) {
				if (f.getName().equals("Dep")) {
					features.add(new Feature("edit(align)_0Dep", f.getValue()));
					passageDep = (String) f.getValue();
				} else if (f.getName().equals("NE")) {
					features.add(new Feature("edit(align)_0NE", f.getValue()));
				}
			}
			features.add(new Feature("edit(align)_1Pos", editDist.getPos2()[questionPO]));
			features.add(new Feature("edit(align)_1Dep", editDist.getRel2()[questionPO]));
			features.add(new Feature("edit(align)_Pos", t.getPos().getPosValue() + "_" + editDist.getPos2()[questionPO]));
			features.add(new Feature("edit(align)_Dep", passageDep + "_" + editDist.getRel2()[questionPO]));
			// TODO: NE renames
		}

		/* (N.B. it is also possible we don't generate any edit feature;
		 * we don't know about some tokens.  For example, prepositions
		 * are not part of dependency structure in StanfordParser's NN
		 * based output, they instead specialize dependency rels. */

		/* Also make a feature about the nearest alignment, unless
		 * we are aligned. */
		if (!questionPOMapping.containsKey(wordOrderIdx)) {
			Integer nearestAlignedWOIdx = getNearestAligned(wordOrderIdx);
			if (nearestAlignedWOIdx != null) {  // (we cannot just questionPOMapping.isEmpty() as it always contains -1 key
				//System.err.println(wordOrderIdx + " " + nearestAlignedWOIdx + " " + maxWOIdx + " " + questionPOMapping);
				int alignedPOIdx = postOrderMapping.get(nearestAlignedWOIdx);
				// TODO: Make this a numeric, continuous feature
				int dist = nearestAlignedWOIdx - wordOrderIdx;
				features.add(new Feature("nearest_aligned", Integer.toString(dist)));
				features.add(new Feature("nearest_aligned_Pos", editDist.getPos1()[alignedPOIdx]));
				features.add(new Feature("nearest_aligned_Dep", editDist.getRel1()[alignedPOIdx]));
				// TODO NE
			}
		}

		return features;
	}
}

