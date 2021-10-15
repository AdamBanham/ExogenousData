package org.processmining.qut.exogenousaware.ds.timeseries.stats;

public interface Measure {
	
	/**
	 * This calcuates the given measure.
	 * @return
	 */
	public abstract double calculate();

}
