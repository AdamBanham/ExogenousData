package org.processmining.qut.exogenousaware.ml.data;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

@Builder
public class FeatureVectorImpl implements FeatureVector {

	@NonNull @Singular @Getter List<Double> values;
	@NonNull @Singular @Getter List<String> columns;
	@Getter int identifier;
	
	public int getSize() {
		return this.values.size();
	}

	public double get(int index) {
		return this.values.get(index);
	}
	
	

}
