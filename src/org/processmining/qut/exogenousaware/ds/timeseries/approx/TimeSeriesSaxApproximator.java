package org.processmining.qut.exogenousaware.ds.timeseries.approx;

import java.util.ArrayList;
import java.util.List;

import org.processmining.qut.exogenousaware.ds.timeseries.data.DiscreteTimePoint;
import org.processmining.qut.exogenousaware.ds.timeseries.data.DiscreteTimeSeries;
import org.processmining.qut.exogenousaware.ds.timeseries.data.RealTimePoint;
import org.processmining.qut.exogenousaware.ds.timeseries.data.RealTimeSeries;

public class TimeSeriesSaxApproximator implements TimeSeriesApproximator<RealTimeSeries, DiscreteTimeSeries> {

//	SAX Gaussian equiprobable regions (10)
	public static List<Double> SAX_BOUNDARIES = new ArrayList<Double>() {{
		add(-1.28);
		add(-0.84);
		add(-0.52);
		add(-0.25);
		add(0.0);
		add(0.25);
		add(0.52);
		add(0.84);
		add(1.28);
	}};
	public static List<String> SAX_LETTERS = new ArrayList<String>() {{
		add("a");
		add("b");
		add("c");
		add("d");
		add("e");
		add("f");
		add("g");
		add("h");
		add("i");
		add("j");
	}};
	
	
	public DiscreteTimeSeries approximate(RealTimeSeries series) {
		List<DiscreteTimePoint> points = new ArrayList();
		for(RealTimePoint point: series.getPoints()) {
			points.add(new DiscreteTimePoint(
					findSaxLetter(point.getValue()),
					point.getTime()
					)
			);
		}
		return new DiscreteTimeSeries(
				series.getName()+":SAX",
				series.getColor(),
				points
		);
	}
	
	public String findSaxLetter(Double value) {
		int letter = 0;
		for(Double boundary: SAX_BOUNDARIES) {
			if (value <= boundary) {
				return SAX_LETTERS.get(letter);
			}
			letter += 1;
		}
		return SAX_LETTERS.get(letter);
	}


}
