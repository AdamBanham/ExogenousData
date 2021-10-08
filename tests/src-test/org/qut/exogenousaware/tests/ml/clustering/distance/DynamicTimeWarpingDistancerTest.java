package org.qut.exogenousaware.tests.ml.clustering.distance;

import org.junit.jupiter.api.Test;
import org.qut.exogenousaware.ml.clustering.distance.DynamicTimeWarpingDistancer;
import org.qut.exogenousaware.ml.data.FeatureVector;
import org.qut.exogenousaware.ml.data.FeatureVectorImpl;

class DynamicTimeWarpingDistancerTest {

	
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
	void canCompute() {
		
		DynamicTimeWarpingDistancer distancer = DynamicTimeWarpingDistancer.builder().build();
		
		double distance = distancer.distance(A, B);
		
		System.out.println("Computed distance :: "+distance);
		
	}

}
