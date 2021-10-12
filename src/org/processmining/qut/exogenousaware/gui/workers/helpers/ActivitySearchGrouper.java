package org.processmining.qut.exogenousaware.gui.workers.helpers;

import org.deckfour.xes.model.XTrace;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;

import lombok.Builder;
import lombok.NonNull;

/**
 * This Grouper class looks for a named activity being present in an endogenous trace and assigns a boolean identifier as a group identifier. 
 * 
 * 
 * @author Adam Banham
 *
 */
@Builder
public class ActivitySearchGrouper implements ExogenousObserverGrouper {

	@NonNull private String activityName;
	
	public int findGroup(XTrace trace, SubSeries sliced) {
		int group = 0;
		boolean found = trace
				.stream()
				.map(ev -> ev.getAttributes().get("concept:name").toString())
				.map(ev -> ev.contains(this.activityName))
				.reduce(false, (c,n) -> (c || n));
		group = found ? 1 : 0;
		return group;
	}
	
	public int findGroup(XTrace trace, SubSeries sliced,int endEvent) {
		int group = 0;
		boolean found = trace
				.stream()
				.limit(endEvent+1)
				.map(ev -> ev.getAttributes().get("concept:name").toString())
				.map(ev -> ev.contains(this.activityName))
				.reduce(false, (c,n) -> (c || n));
		group = found ? 1 : 0;
		return group;
	}
	
	public String getGroupName(int group) {
		return group == 1 ? "Found "+this.activityName : "Did not occur";
	}
	

}
