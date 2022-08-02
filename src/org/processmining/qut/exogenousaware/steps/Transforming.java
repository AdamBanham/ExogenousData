package org.processmining.qut.exogenousaware.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.deckfour.xes.model.XEvent;
import org.processmining.qut.exogenousaware.data.ExogenousDatasetType;
import org.processmining.qut.exogenousaware.data.storage.ExogenousAttribute;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;
import org.processmining.qut.exogenousaware.steps.transform.type.PossibleOutcomeTransformer;
import org.processmining.qut.exogenousaware.steps.transform.type.linear.SlopeTransformer;

public class Transforming {
		

	final public static List<ExogenousAttribute> applyNaiveTransforms(List<XEvent> exoLinks, String key) {
		List<ExogenousAttribute> transforms = new ArrayList<ExogenousAttribute>();
		try {
			transforms.add(minTransform(exoLinks,key));
		} catch (NoSuchElementException e) {
			System.out.println("[Exogenous Transforming] Unable to perform minimum transform :"+e.getMessage());
		}
		try {
			transforms.add(maxTransform(exoLinks, key));
		} catch (NoSuchElementException e) {
			System.out.println("[Exogenous Transforming] Unable to perform maximun transform:"+e.getMessage());
		}
		try {
			transforms.add(meanTransform(exoLinks, key));
		} catch (NoSuchElementException e) {
			System.out.println("[Exogenous Transforming] Unable to perform mean transform:"+e.getMessage());
		}
		
		return transforms;
	}
	
	public static List<TransformedAttribute> applySlopeTransform(SubSeries subtimeseries,String dataset){
		List<TransformedAttribute> attrs = new ArrayList<TransformedAttribute>();
		if (subtimeseries.getDatatype().equals(ExogenousDatasetType.NUMERICAL)) {
			if (subtimeseries.getSubEvents().size() > 1) {
				attrs.add(SlopeTransformer.builder().build().transform(subtimeseries));
			}
		} else if (subtimeseries.getDatatype().equals(ExogenousDatasetType.DISCRETE)) {
			attrs.add(PossibleOutcomeTransformer.builder().Outcome("SEPSIS INFECTION").build().transform(subtimeseries));
		}
		return attrs;
	}
	
	
	final public static ExogenousAttribute minTransform(List<XEvent> exoLinks, String key ) throws NoSuchElementException {
		
		double transformValue = exoLinks.stream()
				.map(ev -> ev.getAttributes().get("exogenous:value").toString())
				.map(val -> Double.parseDouble(val))
				.reduce((curr,next) -> curr > next ? next : curr)
				.get();
		
		return ExogenousAttribute.builder() 
				.key(key)
				.transform("min")
				.value(
						transformValue
				)
				.build();
	}
	
	final public static ExogenousAttribute maxTransform(List<XEvent> exoLinks, String key ) throws NoSuchElementException {
			
			double transformValue = exoLinks.stream()
					.map(ev -> ev.getAttributes().get("exogenous:value").toString())
					.map(val -> Double.parseDouble(val))
					.reduce((curr,next) -> curr < next ? next : curr)
					.get();
			
			return ExogenousAttribute.builder() 
					.key(key)
					.transform("max")
					.value(
							transformValue
					)
					.build();
	}
	
	
	final public static ExogenousAttribute meanTransform(List<XEvent> exoLinks, String key ) throws NoSuchElementException {
		
		double transformValue = exoLinks.stream()
				.map(ev -> ev.getAttributes().get("exogenous:value").toString())
				.map(val -> Double.parseDouble(val))
				.reduce((curr,next) -> curr + next )
				.get();
		
		return ExogenousAttribute.builder() 
				.key(key)
				.transform("mean")
				.value(
						transformValue/exoLinks.size()
				)
				.build();
	}

}

