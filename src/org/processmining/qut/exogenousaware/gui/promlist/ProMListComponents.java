package org.processmining.qut.exogenousaware.gui.promlist;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceList.TraceBuilder;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceView.Event;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceView.Trace;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;

public class ProMListComponents {
	
	private ProMListComponents() {};
	
	public static class ExoTraceBuilder implements TraceBuilder<XTrace> {

		private boolean highlight = false;
		private int eventid = -1;
		private int traceCount = 0;
		private Map<XTrace, Integer> traceMap = new HashMap();
		public List<Integer> selection = new ArrayList();
		
		public ExoTraceBuilder() {
		}
		
		
		@Override
		public Trace<? extends Event> build(XTrace element) {
			// TODO Auto-generated method stub
			Boolean hasExo = false;
			
			// check if event in the trace has some transformed attribute, then we have exogenous data.
			for( XEvent ev : element) {
				for( XAttribute attr: ev.getAttributes().values()) {
					if (attr instanceof TransformedAttribute) {
						hasExo = true;
						break;
					}
				}
				if (hasExo) {
					break;
				}
			}
			if (!traceMap.containsKey(element)) {
				traceMap.put(element, traceCount);
				this.traceCount++;
			}
			int traceNo = traceMap.get(element);
			boolean selected = selection.contains(traceNo);
			ExoTrace newTrace;
			if (this.highlight) {
				newTrace = new ExoTrace(element, 
						hasExo,
						this.eventid,
						traceNo,
						selected
				);
			} else {
				newTrace = new ExoTrace(element, 
						hasExo,
						-1,
						traceNo,
						selected
				);
			}
			return newTrace;
			
		}
		
		public void setHighlight(int eventid) {
			this.highlight = true;
			this.eventid = eventid;
		}
		
		public void dehighlight() {
			this.highlight = false;
			this.eventid = -1;
		}
		
	}

	public static class ExoTrace implements Trace<Event> {
		
		public XTrace source;
		public Boolean hasExo;
		public int highlightevent = -1;
		public int traceNo = -1;
		public boolean selected = false;
		
		public ExoTrace(XTrace source, Boolean hasExo,int traceNo, boolean selected) {
			this(source,hasExo,-1,traceNo, selected);
		}
		
		public ExoTrace(XTrace source, Boolean hasExo, int highlightevent, int traceNo, boolean selected) {
			this.source = source;
			this.hasExo = hasExo;
			this.highlightevent = highlightevent;
			this.traceNo = traceNo;
			this.selected = selected;
		}
		
		@Override
		public Iterator<Event> iterator() {
			return new ExoIterator(
					this.source.iterator(),  
					this.highlightevent
					);
		}

		@Override
		public String getName() {
			return this.source.getAttributes().get("concept:name").toString();
		}

		@Override
		public Color getNameColor() {
			return this.hasExo ? new Color(65,150,98) : Color.red;
		}

		@Override
		public String getInfo() {
			if (this.selected) {
				return "Selected";
			} else {
				return "";
			}
		}

		@Override
		public Color getInfoColor() {
			// TODO Auto-generated method stub
			return Color.black;
		}
		
	}

	public static class ExoIterator implements Iterator<Event> {

		Iterator<XEvent> source;
		private int highlightevent = -1;
		private int counter = -1;
		
		public ExoIterator(Iterator<XEvent> source, int highlightevent) {
			this.source = source;
			this.highlightevent = highlightevent;
		}
		
		@Override
		public boolean hasNext() {
			// TODO Auto-generated method stub
			return this.source.hasNext();
		}

		@Override
		public Event next() {
			// TODO Auto-generated method stub
			this.counter++;
			XEvent ev = this.source.next();
			return new ExoEvent(
					ev,
					counter,
					ev.getAttributes().keySet().stream().anyMatch(s -> s.contains("exogenous:dataset")),
					this.counter == this.highlightevent
					);
		}
		
	}

	public static class ExoEvent implements Event {
		
		private XEvent source;
		private boolean hasExo;
		private boolean highlight;
		public int eventID;
		
		public ExoEvent(XEvent source, int eventID, boolean hasExo) {
			this(source, eventID, hasExo, false);
		}
		
		public ExoEvent(XEvent source, int eventID, boolean hasExo, boolean highlight) {
			this.source = source;
			this.hasExo = hasExo;
			this.highlight = highlight;
			this.eventID = eventID;
		}
		
		public void setHighlight(boolean highlight) {
			this.highlight = highlight;
		}
		
		@Override
		public Color getWedgeColor() {
			if (this.highlight) {
				return Color.orange;
			}
			else if (this.hasExo) {
				return new Color(65,150,98);
			} else {
				return Color.red;
			}
		}

		@Override
		public Color getBorderColor() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getLabel() {
			return this.source.getAttributes().get("concept:name").toString();
		}

		@Override
		public Color getLabelColor() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getTopLabel() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Color getTopLabelColor() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getBottomLabel() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Color getBottomLabelColor() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getBottomLabel2() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Color getBottomLabel2Color() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
