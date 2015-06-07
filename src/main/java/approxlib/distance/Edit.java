// Copyright (c) 2010, Nikolaus Augsten. All rights reserved.
// This software is released under the 2-clause BSD license.

/**
 * 
 */
package approxlib.distance;

/**
 * A class for edit operations, including ins/del/ren/ins_subtree/del_subtree
 * @author Xuchen Yao
 *
 */
public class Edit {
	public enum TYPE {
		NONE { public String toString() { return "";}},
		INS { public String toString() { return "ins";}},
		// for insertion, there are three cases:
		// 1. insert a whole subtree, a subtree must contain more than one node
		// 2. insert a leaf, which has no children. leaf is a modifier to its parent,
		//    so it's relative not 'as important' as inserting a 'parent' node.
		// 3. insert a node which doesn't fall into case 1/2 (neither is a leaf nor incurs a subtree)
		// for deletion it's the same
		INS_LEAF { public String toString() { return "insLeaf";}},
		DEL { public String toString() {return "del";}},
		DEL_LEAF { public String toString() {return "delLeaf";}},
		// renaming only happens when lemmas match, so we define it as
		// either renaming only POS, only Rel, or both
		REN_REL { public String toString() {return "renRel";}}, 
		REN_POS { public String toString() {return "renPos";}},
		REN_POS_REL { public String toString() {return "renPosRel";}},
		INS_SUBTREE { public String toString() {return "insSubtree";}},
		DEL_SUBTREE { public String toString() {return "delSubtree";}}
		};
	
	int[] args;
	TYPE type;
	
	public Edit(TYPE type, int... args) {
		this.type = type;
		this.args = args;
	}
	
	public TYPE getType() { return this.type;}
	
	public int[] getArgs() { return this.args;}
	
	public void setArgs(int... args) { this.args = args;}
	
	public String toString() {
		if (this.type == TYPE.NONE) return "";
		StringBuilder sb = new StringBuilder();
		sb.append(this.type.toString());
		sb.append("(");
		sb.append(args[0]);
		for (int i=1;i<args.length;i++) {
			sb.append(","+args[i]);
		}
		sb.append(")");
		
		return sb.toString();
	}

}
