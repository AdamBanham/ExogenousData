package org.processmining.qut.exogenousaware.steps;

import java.util.ArrayList;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class Linking {

	/**
	 * 
	 * @param endogenous trace in endogenous universe to be investigated
	 * @param exoLog the exogenous data set under investigation
	 * @return Linked exogenous traces for a endogenous trace
	 */
	static public ArrayList<XTrace> findLinkedExogenousSignals(XTrace endogenous, XLog exoLog){
		ArrayList<XTrace> linked = new ArrayList<XTrace>();
//		#1 search exogenous dataset for attribute with key set of "exogenous:link:concept"
		ArrayList<XTrace> subset = new ArrayList<XTrace>();
		for(XTrace xtrace: exoLog) {
			if (xtrace.getAttributes().keySet().contains("exogenous:link:concept")) {
				subset.add(xtrace);
			}
		}
//		#2 check for subset what xtraces are linked to endogenous trace
		String endoId = endogenous.getAttributes().get("concept:name").toString();
		for(XTrace xtrace: subset) {
			String exoId = xtrace.getAttributes().get("exogenous:link:concept").toString();
			if (endoId.compareTo(exoId) == 0) {
				linked.add(xtrace);
			}
		}
//		return all linked traces
		return linked;
	}
	
	static public ArrayList<XEvent> lookupLinkedExogenousSubseries (XLog exoLog, ArrayList<String> exoEventStream) {
		ArrayList<XEvent> exoLookupEvents = new ArrayList<XEvent>();
		
		return exoLookupEvents;
	}
	
	
}
