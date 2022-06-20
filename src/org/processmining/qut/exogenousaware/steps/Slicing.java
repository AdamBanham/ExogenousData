package org.processmining.qut.exogenousaware.steps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.qut.exogenousaware.data.ExogenousDataset;
import org.processmining.qut.exogenousaware.data.ExogenousDatasetType;
import org.processmining.qut.exogenousaware.steps.slicing.FutureOutcomeSlicer;
import org.processmining.qut.exogenousaware.steps.slicing.Slicer;
import org.processmining.qut.exogenousaware.steps.slicing.TimeAwareSlicer;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;

public class Slicing {
	
	
	static public XEvent getLastEvent(XEvent curr, XEvent next) {
		if ( ((XAttributeTimestamp)curr.getAttributes().get("time:timestamp")).getValueMillis() < ((XAttributeTimestamp)next.getAttributes().get("time:timestamp")).getValueMillis()) {
			 return next;
		 }
		 return curr;
	}
	
	static public XEvent getFirstEvent(XEvent curr, XEvent next) {
		double currTime = getEventTimeMillis(curr);
		double nextTime = getEventTimeMillis(next);
		if (nextTime == -1 && currTime != -1) {
			return curr;
		}
		else if (currTime == -1 && nextTime != -1) {
			return next;
		}
		else if (currTime != -1 && nextTime != -1) {
			if ( currTime > nextTime) {
				 return next;
			 }
			 return curr;
		}
		else {
			return curr;
		}
	}
	
	static public long getEventTimeMillis(XEvent ev) {
		if (ev.getAttributes().containsKey("time:timestamp")) {
			return ( (XAttributeTimestamp) ev.getAttributes().get("time:timestamp")).getValueMillis();
		}
		return -1;
	}

	/**
	 * 
	 * @param endogenous the endogenous trace under investigation
	 * @param exogenous the linked exogenous observations
	 * @return slicePoints a map containing for each event, a map of subseries cuts from the linked exogenous oberservations
	 * 
	 */
	static public Map<String,Map<String,List<SubSeries>>> naiveEventSlicing (XTrace endogenous, List<XTrace> exogenous, ExogenousDataset edataset) throws UnsupportedOperationException {
		Map<String,Map<String,List<SubSeries>>> eventSlices = new HashMap<String,Map<String,List<SubSeries>>>();
//		for each event add a key and a empty list of connected exogenous events
		for(XEvent event: endogenous){
			eventSlices.put(event.getID().toString(), new HashMap<String,List<SubSeries>>());
		}
//		for each exogenous trace connected to this endogenous trace, find slice points
		for(XTrace exoTrace: exogenous) {
			if (exoTrace.size() < 1) {
				continue;
			}
//			(1) numerical case
			if (edataset.getDataType().equals(ExogenousDatasetType.NUMERICAL)) {
	//			create a list of periods to use
				long hour = 1000 * 60 * 60;
				List<Long> periods = new ArrayList<Long>();
				periods.add(hour*2);
				periods.add(hour*4);
				periods.add(hour*6);
				periods.add(hour*12);
				for(long period: periods) {
		//			get slicing points for this piece of the exogenous dataset
		//			Map<String,ArrayList<XEvent>> slicePoints = naiveSlicingPoints(endogenous, exoTrace);
					Map<XEvent, SubSeries> slicePoints = TimeAwareSlicer
							.builder().timePeriod(period).build()
							.slice(endogenous, exoTrace, edataset);
		//			update each event with matching sub-series 
					for(XEvent endoEvent: slicePoints.keySet()) {
						Map<String, List<SubSeries>> endoMap = eventSlices.get(endoEvent.getID().toString());
						String endoKey = exoTrace.getAttributes().get("exogenous:name").toString();
						if (endoMap.containsKey(endoKey)) {
							endoMap.get(endoKey).add(slicePoints.get(endoEvent));
						} else {
							List<SubSeries> lister = new ArrayList<SubSeries>();
							lister.add(slicePoints.get(endoEvent));
							endoMap.put(endoKey,lister);
						}
					}
				}
			} 
//			(2) discrete case
			else if (edataset.getDataType().equals(ExogenousDatasetType.DISCRETE)) {
//				setup params
				long hour = 1000 * 60 * 60;
				long period = hour * 48;
//				create slicer
				Slicer slicer = FutureOutcomeSlicer.builder()
						.timePeriod(period)
						.build();
//				perform slice
				Map<XEvent, SubSeries> slicePoints = slicer.slice(endogenous, exoTrace, edataset);
//				store slices 
//				update each event with matching sub-series 
				for(XEvent endoEvent: slicePoints.keySet()) {
					Map<String, List<SubSeries>> endoMap = eventSlices.get(endoEvent.getID().toString());
					String endoKey = exoTrace.getAttributes().get("exogenous:name").toString();
					if (endoMap.containsKey(endoKey)) {
						endoMap.get(endoKey).add(slicePoints.get(endoEvent));
					} else {
						List<SubSeries> lister = new ArrayList<SubSeries>();
						lister.add(slicePoints.get(endoEvent));
						endoMap.put(endoKey,lister);
					}
				}
			}
		}
		return eventSlices;
	}
	
	
	
	static public Map<String,ArrayList<XEvent>> naiveSlicingPoints (XTrace endogenous, XTrace exogenous) throws UnsupportedOperationException {
		Map<String,ArrayList<XEvent>> points = new HashMap<String,ArrayList<XEvent>>();
//		#1 : find endogenous before exogenous signal starts
		ArrayList<String> starts = new ArrayList<String>();
//		find the earliest timestamp in exogenous signal
		XAttributeTimestamp startingExoTime = null;
		try {
		startingExoTime = (XAttributeTimestamp) 
				(
				(exogenous.stream()
				.reduce( 
					(curr,next) -> {return getFirstEvent(curr, next);}
				).get()
				)
				.getAttributes().get("time:timestamp")
				);
		} catch (NoSuchElementException e) {
			System.out.println("[ExogenousSlicing] Failed to get starting exo time.");
			System.out.println(e.getCause());
			System.out.println(e.getMessage());
		}
		if (startingExoTime == null) {
			throw new UnsupportedOperationException("Unable to handle case where no starting timestamp can be found");
		}
//		get all events that occured before first exogenous signal point
		for(XEvent ev: endogenous) {
			XAttributeTimestamp time = (XAttributeTimestamp) ev.getAttributes().get("time:timestamp");
			if (time.getValueMillis() < startingExoTime.getValueMillis()) {
				starts.add(ev.getID().toString());
			}
		}
//		#2 : find endogenous after exogenous signal finishs
		ArrayList<String> ends = new ArrayList<String>();
//		find the last timestamp in exogenous signal
		XAttributeTimestamp endingExoTime = (XAttributeTimestamp) exogenous.stream()
				.reduce( 
						(curr,next) -> {return getLastEvent(curr, next);}
				).get().getAttributes().get("time:timestamp");
//		get all events that occured before first exogenous signal point
		for(XEvent ev: endogenous) {
			XAttributeTimestamp time = (XAttributeTimestamp) ev.getAttributes().get("time:timestamp");
			if (time.getValueMillis() > endingExoTime.getValueMillis()) {
				ends.add(ev.getID().toString());
			}
		}		
//		#3 : for each other event, find sub-series between each
		List<XEvent> between = endogenous.stream()
				.filter(
					(event) -> {
						return !ends.contains(event.getID().toString()) && !starts.contains(event.getID().toString());
					}
				)
				.sorted(
						new Comparator<XEvent>() {
							
							@Override
							public int compare(XEvent o1, XEvent o2) {
								long o1time = ((XAttributeTimestamp) o1.getAttributes().get("time:timestamp")).getValueMillis();
								long o2time = ((XAttributeTimestamp) o2.getAttributes().get("time:timestamp")).getValueMillis();
								return Long.compare(o1time,o2time);
							}
						}
				)
				.collect(Collectors.toList());
//		get the last start and first end event
		Optional<XEvent> lastStart = endogenous.stream()
				.filter( (ev) -> {return starts.contains(ev.getID().toString());})
				.reduce( (curr,next) -> {return getLastEvent(curr, next);}
				); 
		Optional<XEvent> firstEnd = endogenous.stream()
				.filter( (ev) -> {return ends.contains(ev.getID().toString());})
				.reduce( (curr,next) -> {return getFirstEvent(curr, next);}
				);
//		setup pointers to prev and lastlast
		XEvent prev;
		XEvent lastlast;
//		handle edge cases
//		case (i) no start event and no slicing events for signal
		if (!lastStart.isPresent()) {
			if (between.size() > 0) {
				prev = between.remove(0);
			} else {
				throw new UnsupportedOperationException("Unable to handle case where no starting event can be found before stream and no events slice stream");
			}
		} else {
			prev = lastStart.get();
		}
//		case (ii) no end event and no slicing events for signal
		if (!firstEnd.isPresent()) {
			if (between.size() > 0) {
				lastlast = between.remove(between.size()-1);
			} else {
				throw new UnsupportedOperationException("Unable to handle case where no ending event can be found after stream and no events are left in slice section");
			}
		} else {
			lastlast = firstEnd.get();
		}
//		for each between event, find exogenous subseries between last and curr
//		store linked exogenous id's in 
//		handle edge case (iii) we have a prev and lastlast but no slicing events left
		if (between.size() > 0) {
			for (XEvent event: between) {
				long prevTime = getEventTimeMillis(prev);
				List<XEvent> betweeners = exogenous.stream()
						.filter( (ev) -> {
							return getEventTimeMillis(ev) >= prevTime;
						})
						.filter( (ev) -> {
							return getEventTimeMillis(ev) < getEventTimeMillis(event);
						})
						.collect(Collectors.toList());
				points.put(
						event.getID().toString(),
						new ArrayList<XEvent>(betweeners)
				);
				prev = event;
			}
		}
		long prevTime = getEventTimeMillis(prev);
		List<XEvent> betweeners = exogenous.stream()
				.filter( (ev) -> {
					return getEventTimeMillis(ev) >= prevTime;
				})
				.filter( (ev) -> {
					return getEventTimeMillis(ev) < getEventTimeMillis(lastlast);
				})
				.collect(Collectors.toList());
		
		points.put(
				lastlast.getID().toString(),
				new ArrayList<XEvent>(betweeners)
		);
		return points;
		
	}
	
	
}
