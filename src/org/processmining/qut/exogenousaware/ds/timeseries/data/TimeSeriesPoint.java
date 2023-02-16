package org.processmining.qut.exogenousaware.ds.timeseries.data;

public interface TimeSeriesPoint<V, T> {

	public T getTime();
	
	public V getValue();
	
}
