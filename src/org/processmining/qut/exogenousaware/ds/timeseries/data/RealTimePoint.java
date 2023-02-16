package org.processmining.qut.exogenousaware.ds.timeseries.data;

public class RealTimePoint implements TimeSeriesPoint<Double, Double> {
	
	private double time;
	private double value;
	
	public RealTimePoint(double time, double value) {
		super();
		this.time = time;
		this.value = value;
	}

	public Double getTime() {
		return this.time;
	}

	public Double getValue() {
		return this.value;
	}

}
