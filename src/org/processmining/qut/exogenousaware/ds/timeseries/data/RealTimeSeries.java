package org.processmining.qut.exogenousaware.ds.timeseries.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.processmining.qut.exogenousaware.ds.timeseries.approx.TimeSeriesSaxApproximator;
import org.processmining.qut.exogenousaware.ds.timeseries.norm.TimeSeriesGuassianNormaliser;
import org.processmining.qut.exogenousaware.ds.timeseries.reduce.PiecewiseAggregateReduction;
import org.processmining.qut.exogenousaware.ds.timeseries.sample.EqualDistanceSampler;

public class RealTimeSeries implements TimeSeries<Double, Double, RealTimePoint> {
	
	private List<RealTimePoint> sequence;
	private int size;
	private Color color;
	private String name;
	private double min = -1;
	private double max =  1;
	
//	default values
	public static Color DEFAULT_COLOR = Color.black;
	public static String DEFAULT_NAME = "A sequence of real numbers";
	
	public RealTimeSeries(RealTimePoint ... points) {
		this(DEFAULT_NAME, DEFAULT_COLOR, points);
	}
	
	public RealTimeSeries(String name, RealTimePoint ... points) {
		this(name, DEFAULT_COLOR, points);
	}
	
	public RealTimeSeries(String name, Color color, RealTimePoint ... points) {
		sequence = new ArrayList<RealTimePoint>();
		for(int i=0; i < points.length; i++) {
			sequence.add(points[i]);
			if (i == 0) {
				this.min = points[i].getValue();
				this.max = points[i].getValue();
			} else {
				double val = points[i].getValue();
				min = val < min ? val : min;
				max = val > max ? val : max;
			}
		}
		this.size = sequence.size();
		this.color = color;
		this.name = name;
	}
	
	public RealTimeSeries(List<RealTimePoint> points) {
		this(DEFAULT_NAME, DEFAULT_COLOR, points);
	}
	
	public RealTimeSeries(String name, List<RealTimePoint> points) {
		this(name, DEFAULT_COLOR, points);
	}
	
	public RealTimeSeries(String name, Color color, List<RealTimePoint> points) {
		sequence = new ArrayList<RealTimePoint>();
		sequence.addAll(points);
		if (sequence.size() > 0) {
			max = sequence
					.stream()
					.reduce((c,n) -> {
						return c.getValue() < n.getValue() ? n : c;
					}
					)
					.get().getValue();
			min = sequence
					.stream()
					.reduce((c,n) -> {
						return c.getValue() > n.getValue() ? n : c;
					}
					)
					.get().getValue();
		}
		this.size = sequence.size();
		this.color = color;
		this.name = name;
	}

	public List<RealTimePoint> getPoints() {
		List<RealTimePoint> ret = new ArrayList<RealTimePoint>();
		for(RealTimePoint point : sequence) {
			ret.add(new RealTimePoint(point.getTime(), point.getValue()));
		}
		return ret;
	}

	public List<Double> getValues() {
		List<Double> ret = new ArrayList<Double>();
		for(RealTimePoint point : sequence) {
			ret.add(point.getValue());
		}
		return ret;
	}

	public List<Double> getTimes() {
		List<Double> ret = new ArrayList<Double>();
		for(RealTimePoint point : sequence) {
			ret.add(point.getTime());
		}
		return ret;
	}

	public int getSize() {
		return size;
	}

	public String getName() {
		return name;
	}

	public Color getColor() {
		return color;
	}
	
	@Override
	public String toString() {
		return this.sequence.toString();
	}
	
//	utility functions
	public double min() {
		return this.min;
	}
	
	public double max() {
		return this.max;
	}
	
	public double computeWeightedMean() {
		if (getSize() == 0) {
			return 0.0;
		}
		double len = sequence.get(getSize() - 1).getTime() - sequence.get(0).getTime();
		double mean = 0.0;
		for(int i=1; i < getSize(); i++) {
			double dur = (sequence.get(i).getTime() - sequence.get(i -1).getTime()) / len;
			double cval = sequence.get(i).getValue();
			mean += cval * dur;
		}
		return mean;
	}
	
	public double computeStandardDeviation() {
		double mean = computeWeightedMean();
		double std = 0.0;
		for(RealTimePoint point: sequence) {
			double tmp = point.getValue() - mean;
			std += Math.pow(tmp, 2);
		}
		return std / getSize();
	}
	
	public RealTimeSeries resampleWithEvenSpacing() {
		return resampleWithEvenSpacing(64);
	}
	
	public RealTimeSeries resampleWithEvenSpacing(int numberOfSamples) {
		return new EqualDistanceSampler(numberOfSamples)
				.sample(this);
		
	}
	
	/**
	 * Splits this series between fromIndex (inclusive) to toIndex (exclusive)
	 * @param fromIndex
	 * @param toIndex
	 * @return
	 */
	public RealTimeSeries splitSeriesByIndex(int fromIndex, int toIndex) {
		List<RealTimePoint> between = sequence.subList(fromIndex, toIndex);
		List<RealTimePoint> splitPoints = new ArrayList();
		for(RealTimePoint point: between) {
			splitPoints.add(new RealTimePoint(point.getTime(), point.getValue()));
		}
		return new RealTimeSeries(
				getName(),
				getColor(),
				splitPoints 
		);
	}
	
	/**
	 * Creates a symbolic aggregate approximation of the time series.
	 * @return a discrete representation of the time series.
	 */
	public DiscreteTimeSeries createSAXRepresentation() {
		TimeSeriesGuassianNormaliser normliser  = new TimeSeriesGuassianNormaliser();
		PiecewiseAggregateReduction reducer = new PiecewiseAggregateReduction(100);
		TimeSeriesSaxApproximator approximator = new TimeSeriesSaxApproximator();
		return approximator.approximate(normliser.normalise(this));
	}
	

}
