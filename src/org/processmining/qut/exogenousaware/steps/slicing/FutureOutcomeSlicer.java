package org.processmining.qut.exogenousaware.steps.slicing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.qut.exogenousaware.data.ExogenousDataset;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;

/**
 * This slicer will create a slice between an endogenous trace and exogenous time series,
 * such that each event has all the data points before an event and some period after the event.
 * <br>
 * <br>
 * This class uses the builder design pattern and timePeriod parameter (ms) for how far to look ahead of the event.<br>
 * New instances are created using the following:<br>
 * PastOutcomeSlicer.builder().timePeriod(100000).build()
 * 
 * @author Adam P. Banham
 *
 */
@Builder
public class FutureOutcomeSlicer implements Slicer {

	@NonNull long timePeriod;
	
	@Default private String identifier = "futureoutcomeslicer";
	@Default private String shortName = "FOS";
	
	public void createUserChoices() {
		// TODO Auto-generated method stub

	}

	public String getName() {
		String name = this.identifier;
		if (this.timePeriod < 1000) {
			name = name + ":" + this.timePeriod + "ms";
		}	
		else if (this.timePeriod < 1000 * 60) {
			name = name + ":" + (this.timePeriod/1000) + "s";
		}			
		else if (this.timePeriod < 1000 * 60 * 60) {
			name = name + ":" + (this.timePeriod/(1000*60)) + "m";
		}			
		else if (this.timePeriod < 1000 * 60 * 60 * 24) {
			name = name + ":" + (this.timePeriod/(1000*60*60)) + "h";
		}
		else {
			name = name + ":" + (this.timePeriod/(1000*60*60*24)) + "d";
		}
		return name;
	}

	public String getShortenName() {
		String name = this.shortName;
		if (this.timePeriod < 1000) {
			name = name + ":" + this.timePeriod + "ms";
		}	
		else if (this.timePeriod < 1000 * 60) {
			name = name + ":" + (this.timePeriod/1000) + "s";
		}			
		else if (this.timePeriod < 1000 * 60 * 60) {
			name = name + ":" + (this.timePeriod/(1000*60)) + "m";
		}			
		else if (this.timePeriod < 1000 * 60 * 60 * 24) {
			name = name + ":" + (this.timePeriod/(1000*60*60)) + "h";
		}
		else {
			name = name + ":" + (this.timePeriod/(1000*60*60*24)) + "d";
		}
		return name;
	}

	public Map<XEvent, SubSeries> slice(XTrace endogenous, XTrace exogenous, ExogenousDataset dataset) {
		Map<XEvent,SubSeries> points = new HashMap<XEvent,SubSeries>();
		String datasetName = exogenous.getAttributes().get("exogenous:name").toString();
		// for each endogenous event find all datapoints to the left
		for(XEvent ev: endogenous) {
			long time = getEventTimeMillis(ev) + this.timePeriod;
			List<XEvent> leftDatapoints = exogenous.stream()
					.filter(e -> getEventTimeMillis(e) < time)
					.collect(Collectors.toList());
			// create subseries for event
			SubSeries subtimeseries = SubSeries.builder()
					.slicingName(this.identifier)
					.abvSlicingName(this.shortName)
					.dataset(datasetName)
					.comesFrom(dataset)
					.source(exogenous)
					.datatype(dataset.getDataType())
					.endogenous(ev)
					.endoSource(endogenous)
					.subEvents(leftDatapoints)
					.build();
			// add to map 
			points.put(ev, subtimeseries);
		}
		return points;
	}

}
