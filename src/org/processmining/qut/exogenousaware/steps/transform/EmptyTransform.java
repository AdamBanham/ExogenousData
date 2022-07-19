package org.processmining.qut.exogenousaware.steps.transform;

import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;

public class EmptyTransform implements Transformer {

	public TransformedAttribute transform(SubSeries subtimeseries) {
		return null;
	}

	public String getName() {
		return "??";
	}

}
