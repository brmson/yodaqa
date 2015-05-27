// Copyright (c) 2010, Nikolaus Augsten. All rights reserved.
// This software is released under the 2-clause BSD license.

package approxlib.distance;
import approxlib.tree.LblTree;

public abstract class TreeDist {

	private boolean normalized;
	
	/**
	 * @param normalized
	 */
	public TreeDist(boolean normalized) {
		this.setNormalized(normalized);
	}
	
	/** 
     * This method returns the distance between two trees as a 
     * scalar value. If the distance function is not symmetric (e.g. 
     * weighted edit distance) the treeDist returns the distance from 
     * tree t1 to tree t2.
     * 
     * If {@link #isNormalized()} is true, the distance is normalized. 
     * If there is no meaningful normalization for a distance, the result 
     * is independant of {@link #isNormalized()}. 
     *
     * @param t1 first tree
     * @param t2 second tree
     * @return dist(t1,t2) for a specific distance function dist(,)
     */
  	public abstract double treeDist(LblTree t1, LblTree t2);

	public boolean isNormalized() {
		return normalized;
	}

	public void setNormalized(boolean normalized) {
		this.normalized = normalized;
	}

	@Override
	public String toString() {
		String strNorm = (this.isNormalized() ? "(normalized)" : "(not normalized)");
		return this.getClass().getSimpleName() + strNorm;
	}

  	
}
