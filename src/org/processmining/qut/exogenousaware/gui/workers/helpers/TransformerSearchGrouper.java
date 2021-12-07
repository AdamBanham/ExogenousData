package org.processmining.qut.exogenousaware.gui.workers.helpers;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;

import lombok.Builder;
import lombok.NonNull;


@Builder
public class TransformerSearchGrouper implements ExogenousObserverGrouper {

	@NonNull private String transformedAttributeName;
	@NonNull private Object value;
	
	
	public int findGroup(XTrace trace, SubSeries sliced) {
		XEvent ev = trace.get(trace.size()-1);
		if (ev.getAttributes().containsKey(this.transformedAttributeName)) {
			Object evValue = ev.getAttributes().get(this.transformedAttributeName);
			return (evValue.toString().equals(value.toString())) ? 1 : 0;
		}
		return 0;
	}

	public int findGroup(XTrace trace, SubSeries sliced, int n) {
		XEvent ev = trace.get(n);
		if (ev.getAttributes().containsKey(this.transformedAttributeName)) {
			Object evValue = ev.getAttributes().get(this.transformedAttributeName);
			return (evValue.toString().equals(value.toString())) ? 1 : 0;
		}
		return 0;
	}

	public String getGroupName(int group) {
		if (group == 0) {
			return "No Infection";
		} else {
			return "Infected";
		}
	}

}
