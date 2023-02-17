package org.processmining.qut.exogenousaware.data.graphs.followrelations;

import java.util.Objects;

public class FollowPair<E> {
	
	protected E left;
	protected E right;
	protected double weight;
	
	protected static String OUT_FORMAT = "(%s -> %s)^%.1f";
	
	public FollowPair(E left, E right, double weight) {
		this.left = left;
		this.right = right;
		this.weight = weight;
	}
	
	public E getLeft() {
		return left;
	}
	
	public E getRight() {
		return right;
	}
	
	public double getWeight() {
		return weight;
	}
	
	public FollowPair<E> combine(FollowPair<E> other){
		if (this.getLeft().equals(other.getLeft())) {
			if (this.getRight().equals(other.getRight())) {
				return new FollowPair<E>(
						this.getLeft(),
						this.getRight(),
						this.getWeight()+other.getWeight()
				);
			}
		}
		return null;
	}
	
	@Override
	public boolean equals(Object o) {
	    // self check
	    if (this == o)
	        return true;
	    // null check
	    if (o == null)
	        return false;
	    // type check and cast
	    if (getClass() != o.getClass())
	        return false;
	    FollowPair<E> pair = (FollowPair<E>) o;
	    // field comparison
	    return Objects.equals(this.getLeft(), pair.getLeft())
	            && Objects.equals(this.getRight(), pair.getRight());
	}
	
	
	
	@Override
	public String toString() {
		return String.format(OUT_FORMAT, this.left, this.right, this.weight);
	}
	
	
}
