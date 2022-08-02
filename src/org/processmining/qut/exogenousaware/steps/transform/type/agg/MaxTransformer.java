package org.processmining.qut.exogenousaware.steps.transform.type.agg;

import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;
import org.processmining.qut.exogenousaware.steps.transform.type.Transformer;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class MaxTransformer implements Transformer {
	

	public TransformedAttribute transform(SubSeries subtimeseries) {
		if (subtimeseries.size() < 1) {
			return null;
		}
		Double max = subtimeseries.getYSeries()
				.stream()
				.reduce(Double::max)
				.get();	
		max = Double.parseDouble(String.format("%.2f", max));
		return TransformedAttribute.builder()
				.value(max)
				.key(subtimeseries.buildPrefix(true))
				.transform(getName())
				.source(subtimeseries)
				.build();
	}

	public String getName() {
		return "max";
	}

}
