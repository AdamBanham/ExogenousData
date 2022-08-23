package org.processmining.qut.exogenousaware.measures;

import org.deckfour.xes.model.XLog;
import org.processmining.qut.exogenousaware.stats.models.ModelStatistics;

public interface PetriNetMeasure {
	
	
	public double measure(XLog log, Object model, ModelStatistics statistics, Object Alignment);
}
