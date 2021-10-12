package org.processmining.tests.qut.exogenousaware.ml.clustering.distance;

import org.junit.Test;
import org.processmining.qut.exogenousaware.ml.clustering.distance.DynamicTimeWarpingDistancer;
import org.processmining.qut.exogenousaware.ml.data.FeatureVector;
import org.processmining.qut.exogenousaware.ml.data.FeatureVectorImpl;

public class DynamicTimeWarpingDistancerTest {

	
	FeatureVector A = FeatureVectorImpl.builder()
			.value(0.5)
			.value(1.0)
			.value(1.5)
	.build();
	
	FeatureVector B = FeatureVectorImpl.builder()
			.value(0.0)
			.value(1.0)
			.value(1.0)
			.value(1.0)
			.value(0.5)
	.build();
	
	
	
	@Test
	public void canCompute() {
		
		DynamicTimeWarpingDistancer distancer = DynamicTimeWarpingDistancer.builder().build();
		
		double distance = distancer.distance(A, B);
		
		System.out.println("Computed distance :: "+distance);
		
	}

}
