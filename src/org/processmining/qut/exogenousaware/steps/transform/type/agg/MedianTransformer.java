package org.processmining.qut.exogenousaware.steps.transform.type.agg;

import org.apache.commons.math3.stat.StatUtils;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;
import org.processmining.qut.exogenousaware.steps.transform.type.Transformer;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class MedianTransformer implements Transformer {

	public TransformedAttribute transform(SubSeries subtimeseries) {
		if (subtimeseries.size() < 1) {
			return null;
		}
		double[] vals = new double[subtimeseries.size()];
		int i= 0;
		for(double v : subtimeseries.getYSeries()) {
			vals[i] = v;
			i++;
		}
		double median = StatUtils.percentile(vals, 50);
		median = Double.parseDouble(String.format("%.2f", median));
		return TransformedAttribute.builder()
				.source(subtimeseries)
				.transform(getName())
				.key(subtimeseries.buildPrefix(true))
				.value(median)
				.build();
	}

	public String getName() {
		return "median";
	}

}
