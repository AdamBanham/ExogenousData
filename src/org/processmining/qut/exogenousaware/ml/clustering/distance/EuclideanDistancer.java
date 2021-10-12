package org.processmining.qut.exogenousaware.ml.clustering.distance;

import org.processmining.qut.exogenousaware.ml.data.FeatureVector;

import lombok.Builder;


/**
 * 
 * A distance function which computes the Euclidean distance between observations.<br>
 *
 * This class uses the builder pattern. Call EuclideanDistancer.builder() to create new instances.
 * 
 * @author Adam Banham
 *
 */
@Builder
public class EuclideanDistancer implements Distancer {

	public double distance(FeatureVector A, FeatureVector B) throws IllegalStateException {
		double distance = 0.0;
		checkFeatureVectors(A,B);
//		calculate the sum square differences between i-th members of A and B
		for(int i=0;i < A.getSize();i++) {
			distance += Math.pow((A.get(i) - B.get(i)),2);
		}
//		find square root of sum
		distance = Math.sqrt(distance);
		return distance;
	}

}
