package org.processmining.qut.exogenousaware.ds.timeseries.data;

public class RealTimePoint implements TimeSeriesPoint<Double, Double> {
	
	private double time;
	private double value;
	
	public static String outFormat = "(%.1f@%.1f)";
	
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
	
	@Override
	public String toString() {
		return String.format(outFormat, value, time);
	}

}
