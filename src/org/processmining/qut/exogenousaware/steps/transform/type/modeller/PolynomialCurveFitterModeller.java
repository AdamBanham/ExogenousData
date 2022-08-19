package org.processmining.qut.exogenousaware.steps.transform.type.modeller;

import java.util.List;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;
import org.processmining.qut.exogenousaware.steps.transform.type.Transformer;

import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
public class PolynomialCurveFitterModeller implements Transformer {

	public TransformedAttribute transform(SubSeries subtimeseries) {
		PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2);
		WeightedObservedPoints obs = new WeightedObservedPoints();
		
		List<Long> x = subtimeseries.getXSeries(true);
		List<Double> y = subtimeseries.getYSeries();
		
		for(int i=0 ; i < y.size(); i++) {
			obs.add(i, y.get(i));
		}
		
		if (obs.toList().size() < 1) {
			return null;
		}
		
		double[] coeff = fitter.fit(obs.toList());
		
		return TransformedAttribute.builder() 
			.key(subtimeseries.buildPrefix(true))
		    .value(Double.parseDouble(String.format("%.2f", coeff[1])))
		    .transform(getName())
		    .source(subtimeseries)	
			.build();
	}

	public String getName() {
		return "poly:coeff:second";
	}

}
