package org.processmining.qut.exogenousaware.steps.transform.type.modeller;

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
		for(double y: subtimeseries.getYSeries()) {
			obs.add(1.00, y);
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
