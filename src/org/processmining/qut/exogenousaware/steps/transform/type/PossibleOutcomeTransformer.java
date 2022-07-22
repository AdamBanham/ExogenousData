package org.processmining.qut.exogenousaware.steps.transform.type;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.processmining.qut.exogenousaware.data.ExogenousDatasetType;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;

/**
 * This transformer takes a discrete slice and converts it to a binary attribute.<br>
 * This transformers finds the most recent leftside data point, and all rightside data points of an source event.<br>
 * Then finds outcome if the given outcome occurred at any of the found data points.<br>
 * <br>
 * This class uses the builder design pattern and takes one parameter, the outcome used in .equals comparisions for the 1 value.<br>
 * @author n7176546
 *
 */
@Builder
public class PossibleOutcomeTransformer implements Transformer {

	@NonNull Object Outcome; 
	
	
	@Default private String transformName = "POT";
	@Default private long dayMs = 1000 * 60 * 60 * 24; 
	
	public TransformedAttribute transform(SubSeries subtimeseries) {		
		double outcomeFound = -1.0;
		if (!subtimeseries.getDatatype().equals(ExogenousDatasetType.DISCRETE)) {
			return TransformedAttribute.builder()
				.key(subtimeseries.buildPrefix(true))
				.source(subtimeseries)
				.extension(null)
				.transform(this.transformName)
				.value(outcomeFound)
				.build();
		}
//		get leftmost datapoint value
		XEvent endo = subtimeseries.getEndogenous();
		long eventTime =  getEventTimeMillis(endo);
		List<XEvent> datapoints = subtimeseries.getSubEvents();
		Optional<XEvent> left = datapoints.stream()
				.filter(e -> getEventTimeMillis(e) < eventTime)
				.filter(e -> e.equals(this.Outcome))
				.reduce( (c,n) -> {
					if (getEventTimeMillis(c) < getEventTimeMillis(n)) {
						return n;
					}
					return c;
				});
//		get all rightside data points
		List<XEvent> right = datapoints.stream()
				.filter(e -> getEventTimeMillis(e) >= eventTime)
				.collect(Collectors.toList());
		
//		check for outcome
		Boolean outcomeSeen = false;
//		check that the left outcome is within 7 days of current 
		if (left.isPresent()) {
			long leftMs = getEventTimeMillis(left.get());
			if (Math.abs(leftMs - eventTime) < dayMs * 7) {
				outcomeSeen = true;
			}
		}
//		check right for an outcome
		for(XEvent ev: right) {
			Object value = getExogenousValue(ev);
			outcomeSeen = outcomeSeen || this.Outcome.equals(value);
		}
		outcomeFound = outcomeSeen ? 1.0 : -1.0;
//		create transformed attribute
		return TransformedAttribute.builder()
				.key(subtimeseries.buildPrefix(true))
				.source(subtimeseries)
				.extension(null)
				.transform(this.transformName)
				.value(outcomeFound)
				.build();
	}
	
	private long getEventTimeMillis(XEvent ev) throws NoSuchElementException {
		if (ev.getAttributes().containsKey("time:timestamp")) {
			return ( (XAttributeTimestamp) ev.getAttributes().get("time:timestamp")).getValueMillis();
		} else {
			throw new NoSuchElementException("Unable to find suitable attribute for event to describe time.");
		}
	};
	
	private Object getExogenousValue(XEvent ev) throws NoSuchElementException {
		if (ev.getAttributes().containsKey("exogenous:value")) {
			return ev.getAttributes().get("exogenous:value").toString();
		} else {
			throw new NoSuchElementException("Unable to find suitable attribute for event to describe time.");
		}
	}

	public String getName() {
		return this.transformName;
	}

}
