package org.processmining.qut.exogenousaware.gui.listeners;

import java.awt.event.MouseEvent;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceList;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceList.ClickListener;
import org.processmining.qut.exogenousaware.gui.ExogenousTraceView;
import org.processmining.qut.exogenousaware.gui.promlist.ProMListComponents.exoTraceBuilder;
import org.processmining.qut.exogenousaware.gui.promlist.WedgeBuilderFactory;

import lombok.Builder;
import lombok.NonNull;

@Builder
public class TraceBreakdownEventListener implements ClickListener<XTrace>{
	
	@NonNull private ExogenousTraceView source;
	@NonNull private exoTraceBuilder builder;
	@NonNull private ProMTraceList<XTrace> controller;
	
	
	private int previousEvent = -1;

	@Override
	public void traceMouseDoubleClicked(XTrace trace, int traceIndex, int eventIndex, MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void traceMouseClicked(XTrace trace, int traceIndex, int eventIndex, MouseEvent e) {
		
		if(eventIndex >= 0) {
			XEvent ev = trace.get(eventIndex);
	//		create breakdown of event in rightBottomBottoms
			this.source.updateTraceBreakdownEvent(previousEvent != eventIndex ? ev : null, eventIndex);
	//		highlight this event slice in the overview chart
			this.source.highlightEventSlice(trace, eventIndex);
//			highlight wedge that was clicked
			this.controller.setWedgeBuilder(WedgeBuilderFactory.createEventHighlight(previousEvent != eventIndex ? eventIndex : -2));
			this.controller.updateUI();
			this.previousEvent = previousEvent == eventIndex ? -1 : eventIndex;
		}
	}

}
