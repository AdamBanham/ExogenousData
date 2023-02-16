package org.processmining.qut.exogenousaware.ds.timeseries.data;

import java.awt.Color;
import java.util.List;

public interface TimeSeries<V,T,P extends TimeSeriesPoint<V,T>> {

	public List<P> getPoints();
	
	public List<V> getValues();
	
	public List<T> getTimes();
	
	public String getName();
	
	public Color getColor();
	
	public int getSize();
	
}
