package org.processmining.qut.exogenousaware.steps.slicing.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.qut.exogenousaware.data.ExogenousDataset;
import org.processmining.qut.exogenousaware.steps.slicing.Slicer;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class SlicingConfiguration {

	@NonNull @Getter private List<Slicer> generalSlicers;
	@NonNull @Getter private Map<String, List<Slicer>> targetedSlicers;
	
	public Map<String,Map<String,List<SubSeries>>> slice(XTrace endogenous, List<XTrace> exogenous,ExogenousDataset edataset) {
//		create container for all event slices
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
			String exoKey = exoTrace.getAttributes().get("exogenous:name").toString();
//			apply all general slicers
			for(Slicer slicer: this.generalSlicers) {
//				get slicing points for endogenous event
				Map<XEvent, SubSeries> slicePoints = slicer.slice(endogenous, exoTrace, edataset);
				handleSlicing(slicePoints, eventSlices, exoKey);
			}
//			apply targeted slicers if needed
			for(Entry<String, List<Slicer>> target: this.targetedSlicers.entrySet()) {
				if (edataset.toString().equals(target.getKey())) {
					for(Slicer slicer: target.getValue()) {
//						get slicing points for endogenous event
						Map<XEvent, SubSeries> slicePoints = slicer.slice(endogenous, exoTrace, edataset);
						handleSlicing(slicePoints, eventSlices, exoKey);
					}
				}
			}
		}
//		return all event slices found
		return eventSlices;
	}
	
	private void handleSlicing(Map<XEvent, SubSeries> slicePoints, Map<String,Map<String,List<SubSeries>>> eventSlices, String name) {
		// update each event with matching sub-series 
		for(XEvent endoEvent: slicePoints.keySet()) {
			Map<String, List<SubSeries>> endoMap = eventSlices.get(endoEvent.getID().toString());
			if (endoMap.containsKey(name)) {
				endoMap.get(name).add(slicePoints.get(endoEvent));
			} else {
				List<SubSeries> lister = new ArrayList<SubSeries>();
				lister.add(slicePoints.get(endoEvent));
				endoMap.put(name,lister);
			}
		}
	}
}
