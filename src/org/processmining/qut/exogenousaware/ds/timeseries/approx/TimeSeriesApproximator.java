package org.processmining.qut.exogenousaware.ds.timeseries.approx;

import org.processmining.qut.exogenousaware.ds.timeseries.data.TimeSeries;

public interface TimeSeriesApproximator<I extends TimeSeries, O extends TimeSeries> {
	
	public O approximate(I series);

}
