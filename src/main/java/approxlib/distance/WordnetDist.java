package approxlib.distance;

import java.util.Collection;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.PointerTarget;
import net.sf.extjwnl.data.PointerType;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.dictionary.Dictionary;

import approxlib.tree.LblTree;

/** EditDist specialization that does not require edits to rewrite
 * lemmas that are related in the Wordnet lexicon. */
public class WordnetDist extends EditDist {
	/** A cache of per-token synset sets. */
	Collection<Synset>[] synsets1;
	Collection<Synset>[] synsets2;
	
	Dictionary wnDict = null;

	public WordnetDist(boolean normalized) {
		this(1, 1, 1, normalized);
	}
	
	public WordnetDist(double ins, double del, double update, boolean normalized) {
		super(ins, del, update, normalized);
	}

	@Override
	protected void initTreesData(LblTree t1, LblTree t2) {
		super.initTreesData(t1, t2);

		/* Setup wordnet dictionary. */
		try {
			if (wnDict == null)
				wnDict = Dictionary.getDefaultResourceInstance();
		} catch (JWNLException e) {
			e.printStackTrace();
			// XXX: No 'throws'.  Just throw a null pointer
			// exception later...
		}

		/* Setup the synset cache. This also resets any previous
		 * computations. */
		synsets1 = new Collection[nc1];
		synsets2 = new Collection[nc2];

		/* Fill the synset cache with preprocessed data. */
		for (int i = 0; i < nc1; i++)
			synsets1[i] = getTokenSynsets(pos1[i], lemma1[i]);
		for (int j = 0; j < nc2; j++)
			synsets2[j] = getTokenSynsets(pos2[j], lemma2[j]);
	}

	protected boolean existsSynsetLink(Collection<Synset> synset1, Collection<Synset> synset2, PointerType pt) {
		try {
			for (Synset si : synset1)
				for (PointerTarget t : si.getTargets(pt))
					if (synset2.contains((Synset) t))
						return true;
		} catch (JWNLException e) {
			// nothing better to do... XXX when would this actually happen?!
			e.printStackTrace();
		}
		return false;
	}

	protected Collection<Synset> getTokenSynsets(String pos, String lemma) {
		/* XXX: At this point, we allow just NN, VB even though
		 * we could easily support e.g. adjectives as well, for
		 * compatibility with Jacana as we are reproducing its
		 * results.  TODO extend this with more generic code. */
		// System.err.println("pos " + pos + " " + lemma);
		if (pos == null)
			return null; // a tree root?
		if (!(pos.startsWith("NN") || pos.startsWith("VB")))
			return null;

		/* Well, time to set up a Wordnet query. */
		POS wnpos = null;
		if (pos.matches("^NN.*")) {
			wnpos = POS.NOUN;
		} else if (pos.matches("^VB.*")) {
			wnpos = POS.VERB;
		}

		try {
			IndexWord iw = wnDict.getIndexWord(wnpos, lemma.toLowerCase());
			if (iw == null)
				return null;
			return iw.getSenses();
		} catch (JWNLException e) {
			// nothing better to do... XXX when would this actually happen?!
			e.printStackTrace();
			return null;
		}
	}

	@Override
	protected boolean compatibleLemmas(int i, int j) {
		/* [i] is answer
		 * [j] is question */

		/* Text match. */
		if (lemma1[i].equals(lemma2[j]))
			return true;

		/* Unknown/ignored words don't get past. */
		if (synsets1[i] == null || synsets2[j] == null)
			return false;

		/* POS tag equality. */
		// System.err.println("pos1 " + pos1[i] + " " + lemma1[i] + " pos2 " + pos2[j] + " " + lemma2[j]);
		if (!pos2[j].subSequence(0, 2).equals(pos1[i].subSequence(0, 2)))
			return false;

		/* Wordnet-based fuzzy matching follows. */
		/* N.B. the pos type checks seem necessary; e.g. almost all
		 * verbs are hyponymes of "be". */
		/* TODO unify with type coercion logic!  This might also
		 * mean sensible wide hypernyme sets. */

		Collection<Synset> synset1 = synsets1[i];
		Collection<Synset> synset2 = synsets2[j];

		/* Synonyme - intersect between synset1, synset2. */
		for (Synset si : synset1) {
			if (synset2.contains(si)) {
				System.err.println("SYN " + lemma1[i] + " " + lemma2[j]);
				return true;
			}
		}

		/* Hypernymes, hyponymes. (direct) */
		if (pos1[i].startsWith("NN")
		    && (existsSynsetLink(synset1, synset2, PointerType.HYPERNYM)
		        || existsSynsetLink(synset2, synset1, PointerType.HYPERNYM))) {
			System.err.println("HYP " + lemma1[i] + " " + lemma2[j]);
			return true;
		}

		/* VB: Entailing, causing.  (answer word entails question word) */
		// XXX: I think "causing" is suspect.  And other direction? --pasky
		if (pos1[i].startsWith("VB")
		    && (existsSynsetLink(synset1, synset2, PointerType.ENTAILMENT)
		        || existsSynsetLink(synset1, synset2, PointerType.CAUSE))) {
			System.err.println("E/C " + lemma1[i] + " " + lemma2[j]);
			return true;
		}

		/* NN: Member-of, part-of, have-member, have-part, have-substance. */
		// XXX: I think these are suspect.  What about have-substance?  And no instance-of? --pasky
		if (pos1[i].startsWith("NN")
		    && (existsSynsetLink(synset1, synset2, PointerType.MEMBER_HOLONYM)
		        || existsSynsetLink(synset2, synset1, PointerType.MEMBER_HOLONYM)
		        || existsSynsetLink(synset1, synset2, PointerType.PART_HOLONYM)
		        || existsSynsetLink(synset2, synset1, PointerType.PART_HOLONYM)
		        || existsSynsetLink(synset2, synset1, PointerType.SUBSTANCE_HOLONYM))) {
			System.err.println("MPS " + lemma1[i] + " " + lemma2[j]);
			return true;
		}

		/* No compatibility detected. */
		return false;
	}
}
