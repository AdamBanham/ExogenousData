package org.processmining.qut.exogenousaware.gui.promlist;

import java.awt.Color;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceList.TraceBuilder;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceView.Event;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceView.Trace;

public class ProMListComponents {
	
	private ProMListComponents() {};
	
	public static class exoTraceBuilder implements TraceBuilder<XTrace> {

		private Set<String> exoEvents;
		private boolean highlight = false;
		private int eventid = -1;
		
		public exoTraceBuilder(Set<String> exoEvents) {
			this.exoEvents = exoEvents;
		}
		
		
		@Override
		public Trace<? extends Event> build(XTrace element) {
			// TODO Auto-generated method stub
			Boolean hasExo = false;
			if (element.getAttributes().keySet().contains("exogenous:exist")) {
				hasExo = ((XAttributeLiteralImpl) element.getAttributes().get("exogenous:exist") ).getValue().contentEquals("True");
			}
			if (this.highlight) {
				return new exoTrace(element, 
						hasExo,
						exoEvents,
						this.eventid
				);
			} else {
				return new exoTrace(element, 
						hasExo,
						exoEvents
				);
			}
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

	public static class exoTrace implements Trace<Event> {
		
		private XTrace source;
		private Boolean hasExo;
		private Set<String> exoEvents;
		private int highlightevent = -1;
		
		public exoTrace(XTrace source, Boolean hasExo, Set<String> exoEvents) {
			this(source,hasExo,exoEvents,-1);
		}
		
		public exoTrace(XTrace source, Boolean hasExo, Set<String> exoEvents, int highlightevent) {
			this.source = source;
			this.hasExo = hasExo;
			this.exoEvents = exoEvents;
			this.highlightevent = highlightevent;
		}
		
		@Override
		public Iterator<Event> iterator() {
			
			List<String> evSet = this.source.stream().map(ev -> ev.getID().toString()).collect(Collectors.toList());
			return new exoIterator(
					this.source.iterator(), 
					this.hasExo ? this.exoEvents.stream().filter(s -> evSet.contains(s)).collect(Collectors.toSet()) : new HashSet<String>(), 
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
			// TODO Auto-generated method stub
			String info = "";
			for(String key: this.source.getAttributes().keySet()) {
				info += String.format("%s : %s \n",
						key, this.source.getAttributes().get(key)
				);
			}
			return info;
		}

		@Override
		public Color getInfoColor() {
			// TODO Auto-generated method stub
			return Color.black;
		}
		
	}

	public static class exoIterator implements Iterator<Event> {

		Iterator<XEvent> source;
		private Set<String> exoEvents;
		private int highlightevent = -1;
		private int counter = -1;
		
		public exoIterator(Iterator<XEvent> source, Set<String> exoEvents, int highlightevent) {
			this.source = source;
			this.exoEvents = exoEvents;
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
			return new exoEvent(
					ev,
					counter,
					ev.getAttributes().keySet().stream().anyMatch(s -> s.contains("exogenous:dataset")),
					this.counter == this.highlightevent
					);
		}
		
	}

	public static class exoEvent implements Event {
		
		private XEvent source;
		private boolean hasExo;
		private boolean highlight;
		public int eventID;
		
		public exoEvent(XEvent source, int eventID, boolean hasExo) {
			this(source, eventID, hasExo, false);
		}
		
		public exoEvent(XEvent source, int eventID, boolean hasExo, boolean highlight) {
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
