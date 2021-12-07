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

/**
 * This slicer will create a slice between an endogenous trace and exogenous time series,
 * such that each event has all the data points before each event within the exogenous time series.
 * <br>
 * <br>
 * This class uses the builder design pattern and has no parameters.<br>
 * New instances are created using the following:<br>
 * PastOutcomeSlicer.builder().build()
 * 
 * @author Adam P. Banham
 *
 */
@Builder
public class PastOutcomeSlicer implements Slicer {
	
	@Default private String shortName = "POS";
	@Default private String identifier = "pastoutcomeslicer";
	
	public void createUserChoices() {
		// TODO Auto-generated method stub
	}

	public String getName() {
		return this.identifier;
	}

	public String getShortenName() {
		return this.shortName;
	}

	public Map<XEvent, SubSeries> slice(XTrace endogenous, XTrace exogenous, ExogenousDataset dataset) {
		Map<XEvent,SubSeries> points = new HashMap<XEvent,SubSeries>();
		String datasetName = exogenous.getAttributes().get("exogenous:name").toString();
		// for each endogenous event find all datapoints to the left
		for(XEvent ev: endogenous) {
			long time = getEventTimeMillis(ev);
			List<XEvent> leftDatapoints = exogenous.stream()
					.filter(e -> getEventTimeMillis(e) < time)
					.collect(Collectors.toList());
			// create subseries for event
			SubSeries subtimeseries = SubSeries.builder()
					.slicingName(this.identifier)
					.abvSlicingName(this.shortName)
					.dataset(datasetName)
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
