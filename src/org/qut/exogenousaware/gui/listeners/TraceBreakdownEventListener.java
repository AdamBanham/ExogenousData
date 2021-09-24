package org.qut.exogenousaware.gui.listeners;

import java.awt.event.MouseEvent;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceList.ClickListener;
import org.qut.exogenousaware.gui.ExogenousTraceView;

import lombok.Builder;
import lombok.NonNull;

@Builder
public class TraceBreakdownEventListener implements ClickListener<XTrace>{
	
	@NonNull private ExogenousTraceView source;

	@Override
	public void traceMouseDoubleClicked(XTrace trace, int traceIndex, int eventIndex, MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void traceMouseClicked(XTrace trace, int traceIndex, int eventIndex, MouseEvent e) {
		if(eventIndex >= 0) {
			XEvent ev = trace.get(eventIndex);
	//		create breakdown of event in rightBottomBottoms
			this.source.updateTraceBreakdownEvent(ev);
	//		higlight this event slice in the overview chart
			this.source.highlightEventSlice(trace, eventIndex);
		}
	}

}
