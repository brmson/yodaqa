// Copyright (c) 2010, Nikolaus Augsten. All rights reserved.
// This software is released under the 2-clause BSD license.

package approxlib.distance;

import java.util.*;

import approxlib.tree.LblTree;

public class EditDist extends EditBasedDist {
	
	public static HashSet<String> stopWordPosTags = new HashSet<String>() {{
		add("dt"); add("."); add("pos"); add("in"); add("to"); add("``"); add("''");
	}};
	
	// in some sentences, stopwords get renamed first and the dynamic programming algorithm
	// will later decide not to rename a content word but to delete/insert that.
	// for instance, in the following pair:
	// what is the name of durst 's group ?
	// '' i hope he 's ok , '' said limp bizkit singer fred durst .
	// 's got aligned because it has identical lemma/pos/dep in two sentences
	// then later durst cant be renamed because 's is alread aligned
	// we don't want this to happen: thus instead of assigning 0 cost for stopword alignment,
	// we assign a cost of 3.
	boolean higherCostForStopWordsAlign = true;
	
	// call initKeyroots to init the following arrays!
	int[] kr1;  // LR_keyroots(t1)={k | there is no k'>k such that l(k)=l(k')}
	int[] kr2;  // LR_keyroots(t2)
	int[] l1;   // l(t1) = post-order number of left-most leaf 
             	//   descendant of i-th node in post-order in t1
	int[] l2;   // l(t2)
	
	int[] pId1; // stores the parent ID for each node, thus pId[1] is the parent ID of node 1
	int[] pId2;
	
	int size; // total node counts in both trees
	HashMap<Integer, LblTree> id2node1;
	HashMap<Integer, LblTree> id2node2;
	
	HashMap<Integer, LblTree> idxInWordOrder2node1;
	HashMap<Integer, LblTree> idxInWordOrder2node2;
	HashMap<Integer, Integer> align1to2 = new HashMap<Integer, Integer>(); 	// the final alignment from tree1 to tree2
	HashMap<Integer, Integer> align2to1 = new HashMap<Integer, Integer>();	// the final alignment from tree2 to tree1
	
	HashMap<Integer, Integer> alignInWordOrder1to2 = new HashMap<Integer, Integer>(); 	// the final alignment from tree1 to tree2
	HashMap<Integer, Integer> alignInWordOrder2to1 = new HashMap<Integer, Integer>();	// the final alignment from tree2 to tree1
	String[] lbl1; // label of i-th node in postorder of t1
	String[] lbl2; // label of i-th node in postorder of t2
	
	/**
	 * whether this EditDist computes dependence trees or not
	 * dependency tree node labels are separted by / for lemma/pos/rel
	 * for instance, mary/nnp/nsub
	 */
	Boolean isDep = null; 
	
	
	String[] lemma1; // lemma of i-th node in postorder of t1
	String[] lemma2; // lemma of i-th node in postorder of t2

	String[] word1; // word of i-th node in postorder of t1
	String[] word2; // word of i-th node in postorder of t2

	
	String[] pos1; // pos of i-th node in postorder of t1
	String[] pos2; // pos of i-th node in postorder of t2
	
	String[] rel1; // relation of i-th node in postorder of t1
	String[] rel2; // relation of i-th node in postorder of t2
	
	double[][] treedist; // intermediate treedist results
	double[][] forestdist; // intermediate forest dist results
	int[][][] backpointer;
	Edit[][] edits;
	ArrayList<Edit> editList;
	ArrayList<Edit> compactEditList; // we merge consecutive ins/del to insSubTree and delSubTree
	String editScript = null;
	String compactEditScript = null;
	String humaneEditScript = null;
	
	String sentence1;
	String sentence2;
	
	LblTree t1, t2;
	
	/**
	 * Given an <code>id</code> for a node in tree 1 in post-order, 
	 * return the index of this node in word order
	 * @param id an integer for post-order id in tree 1
	 * @return an integer of index in word order
	 */
	public int id2idxInWordOrder1(int id) {
		return id2node1.get(id).getIdxInWordOrder();
	}

	/**
	 * Given an <code>id</code> for a node in tree 2 in post-order, 
	 * return the index of this node in word order
	 * @param id an integer for post-order id in tree 2
	 * @return an integer of index in word order
	 */
	public int id2idxInWordOrder2(int id) {
		return id2node2.get(id).getIdxInWordOrder();
	}

	
	public String getSentence1() {	return sentence1;}

	public void setSentence1(String sentence1) { this.sentence1 = sentence1;}

	public String getSentence2() { return sentence2;}

	public void setSentence2(String sentence2) { this.sentence2 = sentence2;}
	
	public LblTree getTree1() { return t1;}
	public LblTree getTree2() { return t2;}

	public EditDist(boolean normalized) {
		this(1, 1, 1, normalized);
	}
	
	public EditDist(double ins, double del, double update, boolean normalized) {
		super(ins, del, update, normalized);
	}
	
	// inits kr (keyroots), l (left-most leaves), lbl (labels) with t (tree)
	private void init(int[] kr, int[] l, String[] lbl, String[] word, String[] lemma, String[] pos, String[] rel, int[] pId, 
			HashMap<Integer, LblTree> id2node, HashMap<Integer, LblTree> idxInWordOrder2node, LblTree t) {
		lbl[0] = "";
		int i = 1;
		String[] fields;

		for (Enumeration e = t.postorderEnumeration(); e.hasMoreElements();) {
		 	LblTree n = (LblTree)e.nextElement();
			// add postorder number to node
			n.setIdxInPostOrder(i);
			id2node.put(i, n);
			idxInWordOrder2node.put(n.getIdxInWordOrder(), n);
			// label
			lbl[i] = n.getLabel();
			if (isDep == null) {
				fields = lbl1[i].split("\\/");
				if (fields.length >= 3)
					// originally I used == 3, but then an exception came up: 9/11. OK, >= 3...
					isDep = true;
				else
					isDep = false;
			}
			if (isDep) {
				fields = lbl[i].split("\\/");
				if (fields.length == 1) {
					// root node
					lemma[i] = fields[0];
					pos[i] = "";
					rel[i] = "";
				} else if (fields.length == 3) {
					lemma[i] = fields[0];
					pos[i] = fields[1];
					rel[i] = fields[2];
				} else if (fields.length == 4) {
					word[i] = fields[0];
					lemma[i] = fields[1];
					pos[i] = fields[2];
					rel[i] = fields[3];
				}
			}
			// left-most leaf
			l[i] = ((LblTree)n.getFirstLeaf()).getIdxInPostOrder();	    
			i++;
		}
		for (Enumeration e = t.postorderEnumeration(); e.hasMoreElements();) {
		 	LblTree n = (LblTree)e.nextElement();
		 	if (n.getParent() == null) {
		 		pId[n.getIdxInPostOrder()] = 0;
		 	} else {
		 		pId[n.getIdxInPostOrder()] = ((LblTree)n.getParent()).getIdxInPostOrder(); 
		 	}
		}
		boolean[] visited = new boolean[l.length];
		Arrays.fill(visited, false);
		int k = kr.length - 1;
		for (i = l.length - 1; i >= 0; i--) {
			if (!visited[l[i]]) {
				kr[k] = i;
				visited[l[i]] = true;
				k--;
			}
		}
	}
	
	@Override
	public double nonNormalizedTreeDist(LblTree t1, LblTree t2) {

		// System.out.print(t1.getTreeID() + "|" + t2.getTreeID() + "|");
		this.t1 = t1;
		this.t2 = t2;
		int nc1 = t1.getNodeCount() + 1;
		kr1 = new int[t1.getLeafCount() + 1];
		l1 = new int[nc1];
		lbl1 = new String[nc1];
		lemma1 = new String[nc1];
		word1 = new String[nc1];
		pos1 = new String[nc1];
		rel1 = new String[nc1];
		pId1 = new int[nc1];
		
		int nc2 = t2.getNodeCount() + 1;
		kr2 = new int[t2.getLeafCount() + 1];
		l2 = new int[nc2];
		lbl2 = new String[nc2];
		lemma2 = new String[nc2];
		word2 = new String[nc2];
		pos2 = new String[nc2];
		rel2 = new String[nc2];
		pId2 = new int[nc2];
		
		sentence1 = t1.getSentence();
		sentence2 = t2.getSentence();
		
		size = nc1+nc2-2;
		
		id2node1 = new HashMap<Integer, LblTree>();
		this.idxInWordOrder2node1 = new HashMap<Integer, LblTree>();
		id2node2 = new HashMap<Integer, LblTree>();
		this.idxInWordOrder2node2 = new HashMap<Integer, LblTree>();
		
		init(kr1, l1, lbl1, word1, lemma1, pos1, rel1, pId1, id2node1, this.idxInWordOrder2node1, t1);
		init(kr2, l2, lbl2, word2, lemma2, pos2, rel2, pId2, id2node2, this.idxInWordOrder2node2, t2);

		backpointer = new int[nc1][nc2][2];
		for (int i=0; i<nc1; i++)
			for (int j=0; j<nc2; j++)
				Arrays.fill(backpointer[i][j], -1);
		treedist = new double[nc1][nc2];
		forestdist = new double[nc1][nc2];
		edits = new Edit[nc1][nc2];
		                                   
		for (int i = 1; i < kr1.length; i++) {
			for (int j = 1; j < kr2.length; j++) {
				treeEditDist(kr1[i], kr2[j]);
			}
		}
		return treedist[nc1 - 1][nc2 - 1];
	}
	
	public double getDel(int i, int j) {
		if (this.isDep)
			if (i != 0 && j != 0 && lemma1[i].equals(lemma2[j]))
				// we want a good renaming cost, so set deletion cost to be high
				return Double.MAX_VALUE;
			else
				// cost of 3 times for mismatching lemma/pos/rel
				return super.getDel()*3.0;
		else
			return super.getDel();
	}
	
	public double getIns(int i, int j) {
		if (this.isDep)
			if (i != 0 && j != 0 && lemma1[i].equals(lemma2[j]))
				// we want a good renaming cost, so set insertion cost to be high
				return Double.MAX_VALUE;
			else
				// cost of 3 times for mismatching lemma/pos/rel			
				return super.getIns()*3;
		else
			return super.getIns();
	}
	
	public double getUpdate(int i, int j) {
		double u = super.getUpdate();
		if (lbl1[i].equals(lbl2[j]) && ! higherCostForStopWordsAlign) {
			return 0.0;
		}
		if (this.isDep)
			if (lemma1[i].equals(lemma2[j])) {
				if (higherCostForStopWordsAlign && stopWordPosTags.contains(pos1[i]) &&
						stopWordPosTags.contains(pos2[j])) {
					// why 2.5 here: it should be lower than a cost of insertion/deletion, which is 3
					// it should be higher than the cost of rename, which is either 1 or 2. thus 2.5
					return 2.5*u;
				} else
					return (pos1[i].equals(pos2[j])?0:u) + (rel1[i].equals(rel2[j])?0:u);
			} else
				// we don't allow renaming in the case of lemma mismatch
				return Double.MAX_VALUE;
		else
			return super.getUpdate();
	}

	private void treeEditDist(int i, int j) {
		forestdist[l1[i] - 1][l2[j] - 1] = 0;
		double cDel, cIns, cUpd, min;
		for (int i1 = l1[i]; i1 <= i; i1++) {
			forestdist[i1][l2[j] - 1] = forestdist[i1 - 1][l2[j] - 1] + this.getDel(i1, 0);
			backpointer[i1][l2[j] - 1][0] = i1 - 1;
			backpointer[i1][l2[j] - 1][1] = l2[j] - 1;
			if (this.id2node1.get(i1).isLeaf())
				edits[i1][l2[j] - 1] = new Edit(Edit.TYPE.DEL_LEAF, i1);
			else
				edits[i1][l2[j] - 1] = new Edit(Edit.TYPE.DEL, i1);
			for (int j1 = l2[j]; j1 <= j; j1++) {
				forestdist[l1[i] - 1][j1] = forestdist[l1[i] - 1][j1 - 1] + this.getIns(0, j1);
				backpointer[l1[i] - 1][j1][0] = l1[i] - 1;
				backpointer[l1[i] - 1][j1][1] = j1 - 1;
				if (this.id2node2.get(j1).isLeaf())
					edits[l1[i] - 1][j1] = new Edit(Edit.TYPE.INS_LEAF, j1, pId1[i1], 0, 0);
				else
					edits[l1[i] - 1][j1] = new Edit(Edit.TYPE.INS, j1, pId1[i1], 0, 0);

				double wDel, wIns;
				wDel = this.getDel(i1, j1); 
				wIns = this.getIns(i1, j1);
				min = Double.MAX_VALUE;
				
				if ((l1[i1] == l1[i]) && (l2[j1] == l2[j])) {
					double u = 0;
//					if (!lbl1[i1].equals(lbl2[j1])) {
//						u = this.getUpdate(i1, j1);
//					}
					u = this.getUpdate(i1, j1);
					cDel = forestdist[i1 - 1][j1] + wDel;
					cIns = forestdist[i1][j1 - 1] + wIns;
					cUpd = forestdist[i1 - 1][j1 - 1] + u;
					// for now the order matters: initially we didn't really want insertion because it's hard to get
					// right with, so we placed it last in case some other edit costs the same minimal
					// however, after testing, this order was difficult to get the following case right:
					// for "{d{a}{c{b}}}", "{d{a}{b}}", this order gives del(2)
					// while the correct edit should be del(3).
					// the order wasn't wrong (it's faithful to the backpointer and edit matrix, it just "looked" wrong)
					// but it was hard to get it right when dealing with forests
					// thus we adjusted the order to put renaming (update) last. This works in most cases.
					if (cDel < min) {
						min = cDel;
						backpointer[i1][j1][0] = i1-1;
						backpointer[i1][j1][1] = j1;
						if (this.id2node1.get(i1).isLeaf())
							edits[i1][j1] = new Edit(Edit.TYPE.DEL_LEAF, i1);
						else
							edits[i1][j1] = new Edit(Edit.TYPE.DEL, i1);
					}
					if (cIns < min) {
						min = cIns;
						backpointer[i1][j1][0] = i1;
						backpointer[i1][j1][1] = j1-1;
						// ins(v, p, k, m)
//						insert new node v as a child of p at position k
//						substitute children ck , ck+1 , . . . , cm of p with v
//						insert ck , ck+1 , . . . , cm as children of the new node v (preserving order)
						// insert a node with the label of j1 from tree 2 to tree 1
						int v = j1;
						// p is the parent of i1
						int p = pId1[i1];
						int k = getChildOffsetFromParent(l1[i], p, pId1, id2node1);
						int m = getChildOffsetFromParent(i1, p, pId1, id2node1);
						if (this.id2node2.get(v).isLeaf())
							edits[i1][j1] = new Edit(Edit.TYPE.INS_LEAF, v, p, k, m);
						else
							edits[i1][j1] = new Edit(Edit.TYPE.INS, v, p, k, m);

					}
					if (cUpd < min) {
						min = cUpd;
						backpointer[i1][j1][0] = i1-1;
						backpointer[i1][j1][1] = j1-1;
						if (u != 0.0) {
							if (!pos1[i1].equals(pos2[j1]) && !rel1[i1].equals(rel2[j1]))
								edits[i1][j1] = new Edit(Edit.TYPE.REN_POS_REL, i1, j1);
							else if (!pos1[i1].equals(pos2[j1]))
								edits[i1][j1] = new Edit(Edit.TYPE.REN_POS, i1, j1);
							else
								edits[i1][j1] = new Edit(Edit.TYPE.REN_REL, i1, j1);
						} else {
							// identical nodes, renaming cost is 0
							edits[i1][j1] = new Edit(Edit.TYPE.NONE, null);
						}
						this.align1to2.put(i1, j1);
						this.align2to1.put(j1, i1);
					}
					forestdist[i1][j1] = min;

//					forestdist[i1][j1] = 
//						Math.min(Math.min(forestdist[i1 - 1][j1] + wDel,
//								forestdist[i1][j1 - 1] + wIns),
//								forestdist[i1 - 1][j1 - 1] + u);
					treedist[i1][j1] = forestdist[i1][j1];
				} else {
					cDel = forestdist[i1 - 1][j1] + wDel;
					cIns = forestdist[i1][j1 - 1] + wIns;
					cUpd = forestdist[l1[i1] - 1][l2[j1] -1] + treedist[i1][j1];
					// for now the order matters: we don't really want insertion because it's hard to get
					// right with, so we place it last in case some other edit costs the same minimal
					
					if (cDel < min) {
						min = cDel;
						backpointer[i1][j1][0] = i1-1;
						backpointer[i1][j1][1] = j1;
						if (this.id2node1.get(i1).isLeaf())
							edits[i1][j1] = new Edit(Edit.TYPE.DEL_LEAF, i1);
						else
							edits[i1][j1] = new Edit(Edit.TYPE.DEL, i1);
					}
					if (cIns < min) {
						min = cIns;
						backpointer[i1][j1][0] = i1;
						backpointer[i1][j1][1] = j1-1;
						
						// ins(v, p, k, m)
//						insert new node v as a child of p at position k
//						substitute children ck , ck+1 , . . . , cm of p with v
//						insert ck , ck+1 , . . . , cm as children of the new node v (preserving order)
						// insert a node with the label of j1 from tree 2 to tree 1
						int v = j1;
						// p is the parent of i1
						int p = pId1[i1];
						int p2 = pId2[j1];
						if (lbl1[p].equals(lbl2[p2])) {
							// we peek at whether these two forests share the same parent
							// if they do, then the rules for children k..m apply
							int k = getChildOffsetFromParent(l1[i], p, pId1, id2node1);
							int m = getChildOffsetFromParent(i1, p, pId1, id2node1);
							// TODO: since this is forest, k, m aren't necessarily the children of v
							// should add more checking here
							if (this.id2node2.get(v).isLeaf())
								edits[i1][j1] = new Edit(Edit.TYPE.INS_LEAF, v, p, k, m);
							else
								edits[i1][j1] = new Edit(Edit.TYPE.INS, v, p, k, m);
						} else
							// if they do not ,then we assign 0 to it
							if (this.id2node2.get(v).isLeaf())
								edits[i1][j1] = new Edit(Edit.TYPE.INS_LEAF, v, 0, 0, 0);
							else
								edits[i1][j1] = new Edit(Edit.TYPE.INS, v, 0, 0, 0);
					}
					
					if (cUpd < min) {
						min = cUpd;
						backpointer[i1][j1][0] = i1-1;
						backpointer[i1][j1][1] = j1-1;
						if (cUpd - forestdist[i1-1][j1-1] != 0.0 && !lbl1[i1].equals(lbl2[j1])) {
							if (!pos1[i1].equals(pos2[j1]) && !rel1[i1].equals(rel2[j1]))
								edits[i1][j1] = new Edit(Edit.TYPE.REN_POS_REL, i1, j1);
							else if (!pos1[i1].equals(pos2[j1]))
								edits[i1][j1] = new Edit(Edit.TYPE.REN_POS, i1, j1);
							else
								edits[i1][j1] = new Edit(Edit.TYPE.REN_REL, i1, j1);
						} else {
							// identical nodes, renaming cost is 0
							edits[i1][j1] = new Edit(Edit.TYPE.NONE, null);
						}
						this.align1to2.put(i1, j1);
						this.align2to1.put(j1, i1);
					}

					forestdist[i1][j1] = min;

					
//					forestdist[i1][j1] = 
//						Math.min(Math.min(forestdist[i1 - 1][j1] + wDel,
//								forestdist[i1][j1 - 1] + wIns),
//								forestdist[l1[i1] - 1][l2[j1] -1] + 
//								treedist[i1][j1]);
				}
			}
		}		
	}
	
	/**
	 * Given a node i, finds out the index of the node x that satisfies:
	 * A. this node is an ancestor of i
	 * B. this node is the direct child of j
	 * For instance, given node b2 (i=2), and parent node f7 (j=7)
	 * node x would be c5, which is the first child of f7, so return 1.
*---+ 'f7' 
    +---+ 'c5' 
        +---+ 'd3' 
            +---+ 'a1' 
            +---+ 'b2' 
        +---+ 'g4' 
    +---+ 'e6'
	 * @param i
	 * @param j
	 * @param pId the parent array, pId[i] = j means j is the direct parent of i.
	 * @return the child offset (starting from 1), if j isn't the ancestor of i, then return 0
	 */
	private int getChildOffsetFromParent(int i, int j, int[] pId, HashMap<Integer, LblTree> id2node) {
		if (j == 0) return 0;
		int p = i;
		int counter = 0;
		int ret = -1;
		int top_loop = pId.length;
		while (pId[p] != j) {
			p = pId[p];
			if (p == 0) {
				// j isn't the ancestor of i
				// this happens especially when one tree is a forest
				// e.g., tree(forest) 1 is: a b
				// tree 2 is {d a b}
				// apparently inserting d to tree 1 is enough
				// but here b isn't the ancestor of a at all.
				return 0;
			}
			counter += 1;
			if (counter > top_loop)			
				System.err.println("Recursive loop in EditDist.java! Check your code!");
		}
		
		// now p should be the direct child of j
		// we need to find out which child
		LblTree parent = id2node.get(j);
		LblTree child = id2node.get(p);
		for (int k=0; k<parent.getChildCount(); k++) {
			if (parent.getChildAt(k) == child)
				ret = k+1;
		}
		if (ret == -1) {
			System.err.println("Error: getChildIdxFromParent() in EditDist.java returns -1. Check your code!");
		}
		return ret;
	}
	
	/**
	 * When computing TED bottom-up, the insert operation might have some information missing,
	 * mainly in the form ins(?,p,k,m) with p=k=m=0. Since it's bottom up, we don't know the parent p yet.
	 * This function walks through the edits in a reverse way (top down) and fix at least parent p.
	 */
	private void fixEditScriptTopDown() {

		for (int ii=this.editList.size()-1; ii>=0; ii--) {
			Edit e = this.editList.get(ii);
			int i,j,k,l;

			switch (e.getType()) {
			case INS:
			case INS_LEAF:
				i = e.getArgs()[0];
				j = e.getArgs()[1];
				k = e.getArgs()[2];
				l = e.getArgs()[3];
				if (j==0) {
					LblTree parentJ = (LblTree) this.id2node2.get(i).getParent();
					if (parentJ != null) {
						int parent_of_j = parentJ.getIdxInPostOrder();
						if (this.align2to1.containsKey(parent_of_j)) {
							int alignInTree1 = this.align2to1.get(parent_of_j);
							this.editList.set(ii, new Edit(Edit.TYPE.INS, i,alignInTree1,k,l));
						} else {
							// the parent of j was also inserted to tree1
							// in this case, all children of j SHOULD (?) as well
							// be inserted to tree1. we should consider renaming the
							// operation to insert_sub_tree.
						}
					}
				}
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Print the sequence of edits to transform from tree 1 to tree 2
	 * @return the edit script
	 */
	public String printEditScript() {
		editList = new ArrayList<Edit>();
		if (editScript != null) return editScript;
		if (this.dist == 0.0) {
			editScript = "";
			humaneEditScript = "";
			this.compactEditList = new ArrayList<Edit> ();
			return editScript;
		}

		int i=backpointer.length-1, j=backpointer[0].length-1, new_i;
		while (! (i == -1 || j == -1)) {
			if (edits[i][j] != null && edits[i][j].getType() != Edit.TYPE.NONE)
				editList.add(edits[i][j]);
			new_i = backpointer[i][j][0];
			j = backpointer[i][j][1];
			i = new_i;
		}
		this.fixEditScriptTopDown();
	
		mergeEditScript();
		
		StringBuilder editString = new StringBuilder();
		for (int k=editList.size()-1; k>=0; k--)
			editString.append(editList.get(k)+";");
		editScript = editString.toString().substring(0, editString.length()-1);
		
		editString = new StringBuilder();
		for (int k=compactEditList.size()-1; k>=0; k--)
			editString.append(compactEditList.get(k)+";");
		compactEditScript = editString.toString().substring(0, editString.length()-1);
		return editScript;
	}
	
	protected void mergeEditScript() {
		this.compactEditList = new ArrayList<Edit> ();
		ArrayList<Integer> postOrderChildren = null;
		int window;

		int totalSize = this.editList.size();
		for (int ii=0; ii<totalSize; ii++) {
			Edit e = this.editList.get(ii);
			int i,j,k,l;

			switch (e.getType()) {
			case DEL:
			case DEL_LEAF:
				i = e.getArgs()[0];

				postOrderChildren = this.id2node1.get(i).postOrderChildren();
				// now the last element of postOrderChildren should be i since it's i's post order traversal
				window = postOrderChildren.size();
				if (window > 1 && window <= totalSize-ii) {
					boolean match = false;
					for (int s=1; s<window; s++) {
						// look backward window elements and see whether they all match
						if (this.editList.get(ii+s).getType() == Edit.TYPE.DEL ||
								this.editList.get(ii+s).getType() == Edit.TYPE.DEL_LEAF) {
							if (this.editList.get(ii+s).getArgs()[0] == postOrderChildren.get(window-s-1)) {
								match = true;
								continue;
							}
							match = false; break;
						}
						match = false; break;							
					}
					if (match) {
						this.compactEditList.add(new Edit(Edit.TYPE.DEL_SUBTREE, i));
						// jump over this window
						ii += window-1;
					} else {
						this.compactEditList.add(e);
					}
				} else
					this.compactEditList.add(e);
				
				break;
//			case 'r':
//				m = pRen.matcher(e);
//				m.find();
//				i = Integer.parseInt(m.group(1));
//				j = Integer.parseInt(m.group(2));
//				break;
			case INS:
			case INS_LEAF:
				i = e.getArgs()[0];
				j = e.getArgs()[1];
				k = e.getArgs()[2];
				l = e.getArgs()[3];

				postOrderChildren = this.id2node2.get(i).postOrderChildren();
				// now the last element of postOrderChildren should be i since it's i's post order traversal
				window = postOrderChildren.size();
				if (window > 1 && window <= totalSize-ii) {
					boolean match = false;
					for (int s=1; s<window; s++) {
						// look backward window elements and see whether they all match
						if (this.editList.get(ii+s).getType() == Edit.TYPE.INS ||
								this.editList.get(ii+s).getType() == Edit.TYPE.INS_LEAF) {
							if (this.editList.get(ii+s).getArgs()[0] == postOrderChildren.get(window-s-1)) {
								match = true;
								continue;
							}
							match = false; break;
						}
						match = false; break;							
					}
					if (match) {
						this.compactEditList.add(new Edit(Edit.TYPE.INS_SUBTREE, i,j,k,l));
						// jump over this window
						ii += window-1;
					} else {
						this.compactEditList.add(e);
					}
				} else
					this.compactEditList.add(e);
				
				break;
			default:
				this.compactEditList.add(e);
				break;
			}
		}
		
	}
	
	/**
	 * Consecutive operations of ins/del are searched and merged into
	 * insSubTree/delSubTree if possible.
	 * @return
	 */
	public String printCompactEditScript() {
		if (compactEditScript == null)
			printEditScript();
		return compactEditScript;
	}
	
	public void NaiveFeatureExtractor(int[] feats) {
		String editScript = printHumaneEditScript();
		int cInt = editScript.split("ins\\(").length-1;
		int cDel = editScript.split("del\\(").length-1;
		int cRen = editScript.split("ren\\(").length-1;
		feats[0] = cInt;
		feats[1] = cDel;
		feats[2] = cRen;
	}
	
	public String printHumaneEditScript() {
		if (humaneEditScript != null) return humaneEditScript;
		if (editScript == null)
			printEditScript();
		// yes, again, since printEditScript might set it in the case of dist=0.0 
		if (humaneEditScript != null) return humaneEditScript;

		StringBuilder sb = new StringBuilder();
		int i,j,k,l;
		for (int ii=this.editList.size()-1; ii>=0; ii--) {
			Edit e = this.editList.get(ii);
			switch (e.getType()) {
			case DEL:
			case DEL_LEAF:
			case DEL_SUBTREE:
				i = e.getArgs()[0];
				sb.append(String.format("%s(%s);", e.getType().toString(), lbl1[i]));
				break;
			case REN_POS:
			case REN_REL:
			case REN_POS_REL:
				i = e.getArgs()[0];
				j = e.getArgs()[1];
				sb.append(String.format("%s(%s,%s);", e.getType().toString(), lbl1[i], lbl2[j]));
				break;
			case INS:
			case INS_LEAF:
			case INS_SUBTREE:
				i = e.getArgs()[0];
				j = e.getArgs()[1];
				k = e.getArgs()[2];
				l = e.getArgs()[3];
				sb.append(String.format("%s(%s,%s,%d,%d);", e.getType().toString(), lbl2[i], lbl1[j], k, l));
				break;
			default:
				System.err.println("Unable to parse edit: "+e);
				System.err.println("Check your code!");
				break;
			}
		}
		
		humaneEditScript = sb.toString().substring(0, sb.length()-1);
		return humaneEditScript;
	}
	
	public static String prettyPrint2D(double[][] m) {
		StringBuilder sb = new StringBuilder();
		int rows = m.length;
		int columns = m[0].length;
		
		sb.append("   |");
		for (int i=0; i<columns; i++) {
			sb.append("   "+i+"   ");
		}
		sb.append("\n____");
		for (int i=0; i<columns; i++) {
			sb.append("_______");
		}
		sb.append("\n");
		for (int i=0; i<rows; i++) {
			sb.append(" "+i+" |");
			for (int j=0; j<columns; j++) {
				sb.append(String.format("  %.1f  ", m[i][j]));
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	public static String prettyPrint3D(int[][][] m) {
		StringBuilder sb = new StringBuilder();
		int rows = m.length;
		int columns = m[0].length;
		int size = 1 + m[0][0].length*2;
		for (int i=0; i<size; i++)
			sb.append(" ");
		String spaces = sb.toString();
		sb = new StringBuilder();
		for (int i=0; i<size; i++)
			sb.append("_");
		String underscores = sb.toString();
		sb = new StringBuilder();
		
		sb.append("   |");
		for (int i=0; i<columns; i++) {
			sb.append(spaces+i+spaces);
		}
		sb.append("\n____");
		for (int i=0; i<columns; i++) {
			sb.append(underscores+underscores+"_");
		}
		sb.append("\n");
		for (int i=0; i<rows; i++) {
			sb.append(" "+i+" |");
			for (int j=0; j<columns; j++) {
				sb.append("  "+Arrays.toString(m[i][j])+"   ");
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	public static String prettyPrint2D(Edit[][] m) {
		StringBuilder sb = new StringBuilder();
		int rows = m.length;
		int columns = m[0].length;
		// ins(1,2,3,4) --> size is 12
		int size = 12+4;
		for (int i=0; i<size/2; i++)
			sb.append(" ");
		String spaces = sb.toString();
		sb = new StringBuilder();
		for (int i=0; i<size/2; i++)
			sb.append("_");
		String underscores = sb.toString();
		sb = new StringBuilder();
		
		sb.append("   |");
		for (int i=0; i<columns; i++) {
			sb.append(spaces+i+spaces);
		}
		sb.append("\n____");
		for (int i=0; i<columns; i++) {
			sb.append(underscores+underscores+"_");
		}
		sb.append("\n");
		for (int i=0; i<rows; i++) {
			sb.append(" "+i+" |");
			for (int j=0; j<columns; j++) {
				sb.append(String.format("%16s", m[i][j]==null?"":m[i][j].toString()));
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	public void printForestDist() {
		System.out.println("ForestDist:\n");
		System.out.println(prettyPrint2D(this.forestdist));
	}
	
	public void printTreeDist() {
		System.out.println("TreeDist:\n");
		System.out.println(prettyPrint2D(this.treedist));
	}
	
	public void printBackPointer() {
		System.out.println("backpointer:\n");
		System.out.println(prettyPrint3D(this.backpointer));
	}
	
	public void printEditMatrix() {
		System.out.println("Edit Matrix:\n");
		System.out.println(prettyPrint2D(this.edits));
	}
	
	public ArrayList<Edit> getCompactEditList() {
		return this.compactEditList;
	}
	
	public ArrayList<Edit> getEditList() {
		return this.editList;
	}
	
	public int getSize() { return this.size;}
	
	public HashMap<Integer, LblTree> getId2node1() {return id2node1;}
	public HashMap<Integer, LblTree> getId2node2() {return id2node2;}
	
	public String[] getPos1() {return pos1;}
	public String[] getPos2() {return pos2;}
	
	public String[] getRel1() {return rel1;}
	public String[] getRel2() {return rel2;}
	
	public String[] getLemma1() {return lemma1;}
	public String[] getLemma2() {return lemma2;}
	
	public HashMap<Integer, Integer> getAlign1to2() {return align1to2;}
	public HashMap<Integer, Integer> getAlign2to1() {return align2to1;}
	
	public HashMap<Integer, Integer> getAlignInWordOrder1to2() {
		if (alignInWordOrder1to2.size() > 0)
			return alignInWordOrder1to2;
		else {
			for (int i:align1to2.keySet()) {
				int j = align1to2.get(i);
				i = this.id2node1.get(i).getIdxInWordOrder();
				j = this.id2node2.get(j).getIdxInWordOrder();
				alignInWordOrder1to2.put(i, j);
			}
			return alignInWordOrder1to2;
		}
	}
	
	public HashMap<Integer, Integer> getAlignInWordOrder2to1() {
		if (alignInWordOrder2to1.size() > 0)
			return alignInWordOrder2to1;
		else {
			for (int i:align2to1.keySet()) {
				int j = align2to1.get(i);
				i = this.id2node2.get(i).getIdxInWordOrder();
				j = this.id2node1.get(j).getIdxInWordOrder();
				alignInWordOrder2to1.put(i, j);
			}
			return alignInWordOrder2to1;
		}
	}
	
	public int getId2WordOrder1 (int i) {
		return this.id2node1.get(i).getIdxInWordOrder();
	}
	
	public int getId2WordOrder2 (int i) {
		return this.id2node2.get(i).getIdxInWordOrder();
	}
	
	public HashMap<Integer, LblTree> getIdxInWordOrder2node1 () { return idxInWordOrder2node1; }
	public HashMap<Integer, LblTree> getIdxInWordOrder2node2 () { return idxInWordOrder2node2; }
}
