package org.processmining.qut.exogenousaware.ds.timeseries.norm;

import org.processmining.qut.exogenousaware.ds.timeseries.data.TimeSeries;

public interface TimeSeriesNormaliser<T extends TimeSeries> {

	public T normalise(T series);
	
}
