package org.processmining.qut.exogenousaware.ds.timeseries.sample;

import org.processmining.qut.exogenousaware.ds.timeseries.data.RealTimeSeries;

public interface TimeSeriesSampler< T extends RealTimeSeries> {

	public T sample(T series);
}
