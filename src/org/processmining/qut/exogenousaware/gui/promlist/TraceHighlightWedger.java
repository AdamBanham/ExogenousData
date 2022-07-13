package org.processmining.qut.exogenousaware.gui.promlist;

import java.awt.Color;

import org.processmining.framework.util.ui.widgets.traceview.ProMTraceList.DefaultWedgeBuilder;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceView.Event;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceView.Trace;
import org.processmining.qut.exogenousaware.gui.promlist.ProMListComponents.ExoEvent;
import org.processmining.qut.exogenousaware.gui.promlist.ProMListComponents.ExoTrace;

public class TraceHighlightWedger extends DefaultWedgeBuilder {
	
private int highlight = -1;
	
	public TraceHighlightWedger(int traceNumber) {
		this.highlight = traceNumber;
	}
	
	@Override
	public Color buildWedgeColor(Trace<? extends Event> trace, Event event) {
		ExoTrace newtracetype = (ExoTrace) trace;
		ExoEvent neweventype = (ExoEvent) event;
		neweventype.setHighlight(newtracetype.traceNo == this.highlight);
		return event.getWedgeColor();
		
	}
}
