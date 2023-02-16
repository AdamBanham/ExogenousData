package org.processmining.qut.exogenousaware.ds.timeseries.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class DiscreteTimeSeries implements TimeSeries<String, Double, DiscreteTimePoint> {
	
	protected String name;
	protected Color color;
	protected int size;
	protected List<DiscreteTimePoint> sequence;
	
//	default values
	public static Color DEFAULT_COLOR = Color.black;
	public static String DEFAULT_NAME = "A sequence of discrete values";
	
	public DiscreteTimeSeries(DiscreteTimePoint ... points) {
		this(DEFAULT_NAME, DEFAULT_COLOR, points);
	}
	
	public DiscreteTimeSeries(String name, DiscreteTimePoint ... points) {
		this(name, DEFAULT_COLOR, points);
	}
	
	public DiscreteTimeSeries(String name, Color color, DiscreteTimePoint ... points) {
		this.name = name;
		this.color = color;
//		handle points
		this.sequence = new ArrayList();
		for(int i=0;i < points.length; i++) {
			this.sequence.add(points[i]);
		}
		this.size = this.sequence.size();
	}
	
	public DiscreteTimeSeries(List<DiscreteTimePoint> points) {
		this(DEFAULT_NAME, DEFAULT_COLOR, points);
	}
	
	public DiscreteTimeSeries(String name, List<DiscreteTimePoint> points) {
		this(name, DEFAULT_COLOR, points);
	}
	
	public DiscreteTimeSeries(String name, Color color, List<DiscreteTimePoint> points) {
		this.name = name;
		this.color = color;
		this.sequence = new ArrayList();
		this.sequence.addAll(points);
		this.size = this.sequence.size();
	}
	

	public List<DiscreteTimePoint> getPoints() {
		List<DiscreteTimePoint> ret = new ArrayList<DiscreteTimePoint>();
		for(DiscreteTimePoint point : sequence) {
			ret.add(new DiscreteTimePoint(point.getValue(), point.getTime()));
		}
		return ret;
	}

	public String getName() {
		return name;
	}

	public Color getColor() {
		return color;
	}

	public int getSize() {
		return size;
	}

	public List<String> getValues() {
		List<String> ret = new ArrayList();
		for(DiscreteTimePoint point: sequence) {
			ret.add(point.getValue());
		}
		return ret;
	}

	public List<Double> getTimes() {
		List<Double> ret = new ArrayList();
		for(DiscreteTimePoint point: sequence) {
			ret.add(point.getTime());
		}
		return ret;
	}
}
