package org.processmining.qut.exogenousaware.steps.transform.type.velocity;

import java.util.Date;
import java.util.List;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.deckfour.xes.model.impl.XEventImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.qut.exogenousaware.data.ExogenousDatasetType;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries.SubSeriesBuilder;
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
	
	static private double scale = 1000 * 60;
	
	public TransformedAttribute transform(SubSeries subtimeseries) {
		if (subtimeseries.size() < 2) {
			return null;
		}
	
		if (subtimeseries.getDatatype() == ExogenousDatasetType.NUMERICAL) {
			List<Double> yvals = subtimeseries.getYSeries();
			List<Long> xvals = subtimeseries.getXSeries(false);
			XTrace velocityTrace = new XTraceImpl( new XAttributeMapImpl());
			
			if (yvals.size() > 1) {
				int curr = 0;
				int next = 1;
				
				while(next < yvals.size()) {
					XEvent event = new XEventImpl();
					event.getAttributes().put( 
							"time:timestamp", new XAttributeTimestampImpl("time:timestamp", new Date(xvals.get(next)))
					);
					
					if (xvals.get(next) > xvals.get(curr) & Double.isFinite(xvals.get(next)) & Double.isFinite(xvals.get(curr)) ) {
						Double v_top = (yvals.get(next) - yvals.get(curr));
						Double v_bottom = ((xvals.get(next) - xvals.get(curr))/scale);
						Double velocity = 
								v_top
								/ 
								v_bottom;
//						System.out.println(String.format("velocity :: %3.2f :: %3.2f / %3.2f", velocity, v_top, v_bottom));
						event.getAttributes().put( 
								"exogenous:value", new XAttributeContinuousImpl("exogenous:value", velocity)
						); 
						velocityTrace.add(event);
						
					}
					
					curr++;					
					next++;
				}
				
			}
			
			
			SubSeriesBuilder velocityBuilder = SubSeries.builder()	
					.slicingName("velocity:"+subtimeseries.getSlicingName())
					.abvSlicingName(subtimeseries.getAbvSlicingName())
					.source(subtimeseries.getSource())
					.dataset("v:"+subtimeseries.getDataset())
					.datatype(ExogenousDatasetType.NUMERICAL)
					.comesFrom(subtimeseries.getComesFrom())
					.endogenous(subtimeseries.getEndogenous());
			
			for(XEvent ev: velocityTrace) {
				velocityBuilder.subEvent((XEvent) ev.clone());
			}
			
			SubSeries velocity = velocityBuilder.build();

			return chainer.transform(velocity);
			
		} else {
			return null;
		}
	}

	public String getName() {
		return "velocity:" + chainer.getName();
	}

}
