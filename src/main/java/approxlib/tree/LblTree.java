// Copyright (c) 2010, Nikolaus Augsten. All rights reserved.
// This software is released under the 2-clause BSD license.

package approxlib.tree;

import approxlib.hash.FixedLengthHash;
import approxlib.hash.HashValue;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import approxlib.util.FormatUtilities;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;


/**
 * A node of a tree. Each tree has an ID.
 * The label can be empty, but can not contain trailing spaces (nor consist only of spaces).
 * 
 * Two nodes are equal, if there labels are equal, and n1 < n2 if label(n1) < label(n2).
 */
public class LblTree extends DefaultMutableTreeNode implements Comparable {
	
	public static final String TAB_STRING = "    ";
	public static final String ROOT_STRING =   "*---+";
	public static final String BRANCH_STRING = "+---+";
	
	public static final String OPEN_BRACKET = "{";
	public static final String CLOSE_BRACKET = "}";
	public static final String ID_SEPARATOR = ":";
	
	public static final int HIDE_NOTHING = 0;
	public static final int HIDE_ROOT_LABEL = 1;
	public static final int RENAME_LABELS_TO_LEVEL = 2;
	public static final int HIDE_ALL_LABELS = 3;
	public static final int RANDOM_ROOT_LABEL = 4;
	
	int treeID = Node.NO_TREE_ID;
	String label = null;
	Object tmpData = null;
	int nodeID = Node.NO_NODE;
	String sentence;
	
	// treeID and nodeID above are pretty confusing. But they are
	// heavily used in other classes, so I decided not to touch them
	// but define my own (and yet another) ID. 
	int idxInWordOrder = -1;
	
	int idxInPostOrder = -1;
	
	public int getIdxInWordOrder() { return idxInWordOrder;}
	
	public String getSentence() { return sentence;}

	public void setSentence(String sentence) { this.sentence = sentence;}

	/**
	 * Use only this constructor!
	 */
	public LblTree(String label, int treeID) {
		super();
		this.treeID = treeID;
		int idx = label.indexOf(":");
		if (idx != -1) {
			this.idxInWordOrder = Integer.parseInt(label.substring(0,idx));
			this.label = label.substring(idx+1);
		} else
			this.label = label;
	}
		
	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String showNode() {
		return label;
	}
	
	public int getTreeID() {
		if (isRoot()) {
			return treeID;
		} else {
			return ((LblTree)getRoot()).getTreeID();
		}
	}
	
	public void setTreeID(int treeID) {
		if (isRoot()) {
			this.treeID = treeID;
		} else {
			((LblTree)getRoot()).setTreeID(treeID);
		}
	}
	
	/**
	 * tmpData: Hook for any data that a method must attach to a tree.
	 * Methods can assume, that this date is null and should return it
	 * to be null!
	 */
	public void setTmpData(Object tmpData) {
		this.tmpData = tmpData;
	}
	
	public Object getTmpData() {
		return tmpData;
	}
	
	public void setIdxInPostOrder(int idx) { this.idxInPostOrder = idx; }
	public int getIdxInPostOrder() { return this.idxInPostOrder; }
	
	/**
	 * Clear tmpData in subtree rooted in this node.
	 */
	public void clearTmpData() {
		for (Enumeration e = breadthFirstEnumeration(); e.hasMoreElements();) {
			((LblTree)e.nextElement()).setTmpData(null);
		}
	}
	
	
	public String toLatex() {
		String res = "";
		String label = FormatUtilities.escapeLatex(this.showNode());
		if (isLeaf() && !isRoot()) {
			res += "\\Tr{" + label + "}";
		} else {
			int levelsep = 20 + (int)(0.3 * this.getNodeCount());
			res += "\\pstree[linewidth=0.2pt,levelsep=" + levelsep + "pt,treefit=tight,treesep=4pt,nodesep=2pt]{\\Tr{" + label + "}}{";
			for (Enumeration e = children(); e.hasMoreElements();) {
				res += ((LblTree)e.nextElement()).toLatex() + "\n";
			}	   
			res += "}";
		}
		return res;
	}
	
	public void prettyPrint() {
		prettyPrint(false);
	}
	
	public void prettyPrint(boolean printTmpData) {
		for (int i = 0; i < getLevel(); i++) {
			System.out.print(TAB_STRING);
		}
		if (!isRoot()) {
			System.out.print(BRANCH_STRING);
		} else {
			if (getTreeID() != Node.NO_TREE_ID) {
				System.out.println("treeID: " + getTreeID());
			}
			System.out.print(ROOT_STRING);
		}
		System.out.print(" '" + this.showNode() + "' ");
		if (printTmpData) {
			System.out.println(getTmpData());
		} else {
			System.out.println();
		}
		for (Enumeration e = children(); e.hasMoreElements();) {
			((LblTree)e.nextElement()).prettyPrint(printTmpData);
		}
		
	}
	
	public int getNodeCount() {
		int sum = 1;
		for (Enumeration e = children(); e.hasMoreElements();) {
			sum += ((LblTree)e.nextElement()).getNodeCount();
		}
		return sum;
	}
	
	public double getMediumFanout() {
		int nodes = getNodeCount();
		int parents = nodes - getLeafCount();
		int children = nodes - 1;
		if (parents == 0) {
			return 0.0;
		} else {
			return ((double)children / (double)parents);
		}
	}
	
	/**
	 * @return all leafs from left to right.
	 */
	public LblTree[] getLeafs() {
		int leafCount = getLeafCount();
		LblTree[] leafs = new LblTree[leafCount];
		int i = 0;
		for (Enumeration e = depthFirstEnumeration(); e.hasMoreElements();) {
			LblTree n = (LblTree)e.nextElement();
			if (n.isLeaf()) {
				leafs[i] = n;
				i++;
			}
		}
		return leafs;
	}
	
	/**
	 * @param enums enumeration to use (0=breadthFirst,
	 * 1=depthFirst=postorder, 2=preOrder)
	 * @return array with all internal nodes
	 */
	public LblTree[] getInternalNodes(int enums) {
		int leafCount = getLeafCount();
		int nodes = getNodeCount();
		LblTree[] internal = new LblTree[nodes - leafCount];
		int i = 0;
		Enumeration e;
		switch (enums) {
		case 1: e = depthFirstEnumeration(); break; 
		case 2: e = preorderEnumeration(); break; 
		case 0: 
		default:
			e = breadthFirstEnumeration(); break;
		}	
		while (e.hasMoreElements()) {
			LblTree n = (LblTree)e.nextElement();
			if (!n.isLeaf()) {
				internal[i] = n;
				i++;
			}
		}
		return internal;
	}
	
	/**
	 * Constructs an LblTree from a string representation of tree. The
	 * treeID in the String representation is optional; if no treeID is given,
	 * the treeID of the returned tree will be NO_ID.
	 *
	 * @param s string representation of a tree. Format: "treeID:{root{...}}".
	 * @return tree represented by s
	 */
	public static LblTree fromString(String s) {
		int treeID = FormatUtilities.getTreeID(s);
		return fromString(s, treeID);
	}
	
	private static LblTree fromString(String s, int TreeID) {
		s = s.substring(s.indexOf(OPEN_BRACKET), s.lastIndexOf(CLOSE_BRACKET) + 1);
		LblTree node = new LblTree(FormatUtilities.getRoot(s), TreeID);
		Vector c = FormatUtilities.getChildren(s);
		for (int i = 0; i < c.size(); i++) {
			node.add(fromString((String)c.elementAt(i), TreeID));
		}
		return node;
	}
	/**
	 * String representation of a tree. Reverse operation of {@link #fromString(String)}.
	 * treeID is NO_ID, it is skipped in the string representation.
	 *
	 * @return string representation of this tree
	 *
	 */
	@Override
	public String toString() {	
		String res = OPEN_BRACKET + showNode();
		if ((getTreeID() >= 0) && (isRoot())) {
			res = getTreeID() + ID_SEPARATOR + res;
		}
		for (Enumeration e = children(); e.hasMoreElements();) {
			res += ((LblTree)e.nextElement()).toString();
		}
		res += CLOSE_BRACKET;
		return res;
	}   
	
	
	/**
	 * 
	 */
	public static LblTree deepCopy(LblTree t) {
		LblTree nt = new LblTree(t.getLabel(), t.getTreeID());
		nt.setTmpData(t.getTmpData());
		for (Enumeration e = t.children(); e.hasMoreElements(); ) {
			nt.add(deepCopy((LblTree)e.nextElement()));
		}
		return nt;
	}
	

	/**
	 * Returns the number of the node in the sibling set.
	 * 
	 * {@link DefaultMutableTreeNode.getIndex(DefaultMutableTreeNode)} fails
	 * if nodes have the same label, as it uses the {@link #equals(Object)} method
	 * to find a node. This method compares the object addresses.
	 *
	 */
	@Override
	public int getIndex(TreeNode n) {
		for (int i = 0; i < this.getChildCount(); i++) {
			if (this.getChildAt(i) == n) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Compares the labels.
	 */
	public int compareTo(Object o) {		
		return getLabel().compareTo(((LblTree)o).getLabel());
	}
	

	/**
	 * Compares the labels.
	 */
	@Override
	public boolean equals(Object o) {
		return this.compareTo(o) == 0;
	}

	/* (non-Javadoc)
	 * @see tree.EditTree#deleteNode(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	public boolean deleteNode(Object nodeOfTree) {
		try {
			LblTree n = (LblTree)nodeOfTree;
			if (n.isRoot()) {
				return false;				
			} else {
			    LblTree parent = (LblTree)n.getParent();
			    int nChildID = parent.getIndex(n);
			    n.removeFromParent();
			    int i = 0;
			    LblTree[] childArr = new LblTree[n.getChildCount()];
			    for (Enumeration<LblTree> e = n.children(); e.hasMoreElements();) {
			    	childArr[i] = e.nextElement();
			    	i++;
			    }
			    for (i = 0; i < childArr.length; i++) {
			    	parent.insert(childArr[i], nChildID + i); 
			    }
			    return true;
			}			
		} catch (ClassCastException e) {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see tree.EditTree#insertNode(java.lang.Object, java.lang.Object, int, int)
	 */
	public boolean insertNode(Object newNode, int k, int n) {
		LblTree nd = (LblTree)newNode;
		if ((this.getChildCount() >= k + n - 1) &&      	// do all children v_k ... v_(k+n-1) exist?
				(nd.getRoot() != this.getRoot())) {      // n and p can not be of the same tree
			for (int j = 0; j < n; j++) {
				nd.add((LblTree)this.getChildAt(k - 1));
			}
			this.insert(nd, k - 1);
			return true;
		} else {
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see tree.EditTree#insertPath(java.lang.Object, java.lang.Object)
	 */
	public boolean insertPath(Object path) {
		LblTree p = (LblTree)path;
		int cmp = this.compareTo(p);

		if (cmp == 0) {    // follow down the path
			if (!p.isLeaf()) {
				int i = 0;
				// search a match among children
				// linear search - slow!!!
				while ((i < this.getChildCount()) &&  
						(!((LblTree)this.getChildAt(i)).insertPath(p.getFirstChild()))) {
					i++;
				}
				if (i == this.getChildCount()) {
					this.add((LblTree)p.getFirstChild());
				}
			}
			return true;
		} else if (cmp > 0) {
			if (!isRoot()) {
				LblTree parent = ((LblTree)this.getParent()); 
				int i = parent.getIndex(this);     // slow, as getIndex() performs linear search in children array
				parent.insert(p, i);
				return true;
			} else {
				return false;
			}
		}
		return false; 		// cmp < 0
	}

	/* (non-Javadoc)
	 * @see tree.EditTree#deletePath(java.lang.Object, java.lang.Object)
	 * 
	 * Problem: returns true, if tree should be empty!
	 * 
	 */
	public int deletePath(Object path) {
		LblTree n = (LblTree)path;
		int cnt = 0;
		while (n.isLeaf() && !n.isRoot()) {
			LblTree l = n;
			n = (LblTree)n.getParent();
			l.removeFromParent();
			cnt++;
		}
		if (n.isLeaf() && n.isRoot()) {
			return -1;
		} else {
			return cnt;
		}
	}
	
	
	/* (non-Javadoc)
	 * @see tree.EditTree#updateNode(java.lang.Object, java.lang.String)
	 */
	public boolean relabelNode(String label) {
		if (!getLabel().equals(label)) {
			setLabel(label);	
			return true;
		} else {
			return false;
		}
	}
	
	public HashValue getNodeHash(FixedLengthHash hf) {
		return hf.getHashValue(this.getLabel());
	}
	
	/**
	 * Taylormade to addresses: 
	 * * all empyt leafs at the 1st (house number) and 3th level (apartment numbers) are removed
	 * * on the other levels, empty leaves are removed if they are the only child of their parent
	 */
	public void cleanNullValues() {
		LblTree[] ch = new LblTree[this.getChildCount()];
		int i = 0;
		for (Enumeration e = this.children(); e.hasMoreElements();) {
			ch[i] = (LblTree)e.nextElement();
			i++;
		}
		for (i = 0; i < ch.length; i++) {
			ch[i].cleanNullValues();
		}
		if (getLabel().equals("") && (isLeaf())) {
				if (getLevel() != 2) {
					removeFromParent();
				} else if (getParent().getChildCount() == 1) {
					removeFromParent();
				} 
		}
	}

	public int getNodeID() {
		return nodeID;
	}

	public void setNodeID(int nodeID) {
		this.nodeID = nodeID;
	}
	
	/**
	 * returns the integer IDs of traveling this node in post order
	 * @return
	 */
	public ArrayList<Integer> postOrderChildren() {
		ArrayList<Integer> treePostOrder = new ArrayList<Integer>();
		for (Enumeration e = this.postorderEnumeration(); e.hasMoreElements();) {
			LblTree n = (LblTree)e.nextElement();
			if (n.getTmpData() == null)
				treePostOrder.add(n.getIdxInPostOrder());
			else
				treePostOrder.add((Integer)n.getTmpData());
		}
		return treePostOrder;
	}
	
}
