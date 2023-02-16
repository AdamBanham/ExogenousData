package org.processmining.qut.exogenousaware.ds.timeseries.reduce;

import org.processmining.qut.exogenousaware.ds.timeseries.data.TimeSeries;

public interface TimeSeriesReducer<T extends TimeSeries> {

	public T reduce(T series);
	
}
