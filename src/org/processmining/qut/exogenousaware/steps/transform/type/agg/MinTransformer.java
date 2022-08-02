package org.processmining.qut.exogenousaware.steps.transform.type.agg;

import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;
import org.processmining.qut.exogenousaware.steps.transform.type.Transformer;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class MinTransformer implements Transformer {

	public TransformedAttribute transform(SubSeries subtimeseries) {
		if (subtimeseries.size() < 1) {
			return null;
		}
		double min = subtimeseries.getYSeries()
				.stream()
				.reduce(Double::min)
				.get();
		min = Double.parseDouble(String.format("%.2f", min));
		return TransformedAttribute.builder()
				.value(min)
				.transform(getName())
				.key(subtimeseries.buildPrefix(true))
				.source(subtimeseries)
				.build();
	}

	public String getName() {
		return "min";
	}

}
