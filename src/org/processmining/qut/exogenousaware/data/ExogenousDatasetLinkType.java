package org.processmining.qut.exogenousaware.data;

import org.deckfour.xes.model.XLog;

public enum ExogenousDatasetLinkType {
	TRACE_ATTRIBUTE_MATCH("match"),
	EVENT_ATTRIBUTE_MATCH("ematch");
	
	private String id;
	
	private ExogenousDatasetLinkType(String id) {
		this.id = id;
	}
	
	public boolean checkLog(XLog log) {
		return false;
	}
	
}
