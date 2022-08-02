package org.processmining.qut.exogenousaware.steps.transform.type.agg;

import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;
import org.processmining.qut.exogenousaware.steps.transform.type.Transformer;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class MeanTransformer implements Transformer {

	public TransformedAttribute transform(SubSeries subtimeseries) {
		if (subtimeseries.size() < 1) {
			return null;
		}
		Double mean = subtimeseries.getYSeries()
				.stream()
				.reduce(Double::sum)
				.get();
		mean = mean / subtimeseries.size();
		mean = Double.parseDouble(String.format("%.2f", mean));
		return TransformedAttribute.builder()
				.source(subtimeseries)
				.key(subtimeseries.buildPrefix(true))
				.transform(getName())
				.value(mean)
				.build();
	}

	public String getName() {
		return "mean";
	}

}
