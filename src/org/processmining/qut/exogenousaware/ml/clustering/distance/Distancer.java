package org.processmining.qut.exogenousaware.ml.clustering.distance;

import org.processmining.qut.exogenousaware.ml.data.FeatureVector;

public interface Distancer {
	
	/**
	 * Computes the distance between two n-dimensional vectors
	 * @param A a n-dimensional vector
	 * @param B a n-dimensional vector
	 * @return The distance between A and B
	 */
	public double distance(FeatureVector A, FeatureVector B);
	
	/**
	 * Ensures that two vectors are compatible for distance measures. Will throw IllegalStateException if A and B are not compatible.
	 * @param A a n-dimensional vector
	 * @param B a n-dimensional vector
	 */
	public default void checkFeatureVectors(FeatureVector A, FeatureVector B) {
		if (A.getSize() != B.getSize()) {
			throw new IllegalStateException("Feature Vectors must be of the same size");
		}
	}

}
