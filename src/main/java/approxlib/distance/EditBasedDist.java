// Copyright (c) 2010, Nikolaus Augsten. All rights reserved.
// This software is released under the 2-clause BSD license.

package approxlib.distance;
import approxlib.tree.LblTree;

public abstract class EditBasedDist extends TreeDist {

	private double ins;
	private double del;
	private double update;
	protected double dist;

	public EditBasedDist(boolean normalized) {
		this(1, 1, 1, normalized);
	}
	
	public EditBasedDist(double ins, double del, double update, boolean normalized) {
		super(normalized);
		this.ins = ins;
		this.del = del;
		this.update = update;
	}
	
	public double getLastComputedDist() {
		return this.dist;
	}
	
	@Override
	public double treeDist(LblTree t1, LblTree t2) {
		if (this.isNormalized()) {
			this.dist =  nonNormalizedTreeDist(t1, t2) / (t1.getNodeCount() + t2.getNodeCount());
			return this.dist;
		} else {
			this.dist = nonNormalizedTreeDist(t1, t2);
			return this.dist;
		}
	}

	public abstract double nonNormalizedTreeDist(LblTree t1, LblTree t2);

	public double getDel() {
		return del;
	}

	public void setDel(double del) {
		this.del = del;
	}

	public double getIns() {
		return ins;
	}

	public void setIns(double ins) {
		this.ins = ins;
	}

	public double getUpdate() {
		return update;
	}

	public void setUpdate(double update) {
		this.update = update;
	}




}
