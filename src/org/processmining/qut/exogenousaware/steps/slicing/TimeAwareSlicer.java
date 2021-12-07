package org.processmining.qut.exogenousaware.steps.slicing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.qut.exogenousaware.data.ExogenousDataset;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

/**
 * A Slicing function which looks for the most recent updates from each exogenous timeseries.<br>
 * 
 * This slicing function will create a sub-timeseries which accounts all measurements observed within some period of each endogenous event.<br>
 * E.G. For each event, all sub-timeseries produced will generate a sequence of measurements that occured within 2 hours of each endogenous event.<br>
 * <br>
 * 
 * This class uses the builder design pattern. Call TimeAwareSlicer.builder() to create new instances.
 * 
 * @author Adam Banham
 */
@Builder
@Data
public class TimeAwareSlicer implements Slicer{
	
	/**
	 * Time period to slice for in milliseconds
	 */
	long timePeriod;
	
	@Default String identifier = "timeawareslicer";
	
	@Override
	public Map<XEvent, SubSeries> slice(XTrace endogenous, XTrace exogenous, ExogenousDataset edataset) {
		Map<XEvent,SubSeries> points = new HashMap<XEvent,SubSeries>();
		String dataset = exogenous.getAttributes().get("exogenous:name").toString();
//		#1 : find endogenous before exogenous signal starts
		ArrayList<XEvent> starts = new ArrayList<XEvent>();
//		find the earliest timestamp in exogenous signal
		long startingExoTime;
		try {
			startingExoTime = getEventTimeMillis(
					exogenous.stream()
					.reduce( 
						(curr,next) -> {return getFirstEvent(curr, next);}
					)
					.get()
					);
			} catch (NoSuchElementException e) {
				System.out.println("[ExogenousSlicing] Failed to get starting exo time.");
				System.out.println(e.getCause());
				System.out.println(e.getMessage());
				throw new UnsupportedOperationException("Unable to handle case where no starting timestamp can be found");
			}
//		get all events that occurred before first exogenous signal point
		for(XEvent ev: endogenous) {
			if (getEventTimeMillis(ev) < startingExoTime) {
				starts.add(ev);
			}
		}
//		#2 : find endogenous after exogenous signal finishs
		ArrayList<XEvent> ends = new ArrayList<XEvent>();
//		find the last timestamp in exogenous signal
		long endingExoTime;
		try {
			endingExoTime = getEventTimeMillis(
					exogenous.stream()
					.reduce( 
						(curr,next) -> {return getLastEvent(curr, next);}
					)
					.get()
					);
		} catch (NoSuchElementException e) {
			System.out.println("[ExogenousSlicing] Failed to get ending exo time.");
			System.out.println(e.getCause());
			System.out.println(e.getMessage());
			throw new UnsupportedOperationException("Unable to handle case where no starting timestamp can be found");
		}
//		get all events that occurred after the end of the exogenous timeseries
		for(XEvent ev: endogenous) {
			if (getEventTimeMillis(ev) > endingExoTime) {
				ends.add(ev);
			}
		}
//		#3 : for each other event, find sub-series between each
		ArrayList<XEvent> between = new ArrayList<XEvent>();
		for(XEvent ev: endogenous) {
			if (!starts.contains(ev) && !ends.contains(ev)) {
				between.add(ev);
			}
		}
//		for each between, find a sub-timeseries of period length
		for(XEvent ev: between) {
			long end = getEventTimeMillis(ev);
			long start = end - this.timePeriod;
			List<XEvent> subseries = exogenous.stream()
					.filter( (xev) -> getEventTimeMillis(xev) >= start)
					.filter( (xev) -> getEventTimeMillis(xev) <= end)
					.collect(Collectors.toList());
			if (subseries.size() > 0) {
				SubSeries subtimeseries = SubSeries.builder()
						.slicingName(this.getName())
						.abvSlicingName(this.getShortenName())
						.dataset(dataset)
						.source(exogenous)
						.datatype(edataset.getDataType())
						.endogenous(ev)
						.subEvents(subseries)
						.endoSource(endogenous)
						.build();
				points.put(ev, subtimeseries);
			}
		}
		return points;
	}


	@Override
	public void createUserChoices() {
		// TODO Auto-generated method stub
	}


	@Override
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


	@Override
	public String getShortenName() {
		String name = "TAS";
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
	
	
	
	
	
}
