package org.processmining.qut.exogenousaware.steps.transform.type.velocity;

import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;
import org.processmining.qut.exogenousaware.steps.transform.type.Transformer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;


@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VelocityTransformer implements Transformer {

	@NonNull Transformer chainer;
	
	public TransformedAttribute transform(SubSeries subtimeseries) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return "velocity:" + chainer.getName();
	}

}
