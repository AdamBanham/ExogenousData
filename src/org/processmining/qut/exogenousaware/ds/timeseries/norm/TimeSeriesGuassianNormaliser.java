package org.processmining.qut.exogenousaware.ds.timeseries.norm;

import java.util.ArrayList;
import java.util.List;

import org.processmining.qut.exogenousaware.ds.timeseries.data.RealTimePoint;
import org.processmining.qut.exogenousaware.ds.timeseries.data.RealTimeSeries;

public class TimeSeriesGuassianNormaliser implements TimeSeriesNormaliser<RealTimeSeries> {

	public RealTimeSeries normalise(RealTimeSeries series) {
		if (series.getSize() < 2) {
			return null;
		}
		
		List<Double> ret = new ArrayList<Double>(series.getSize());
		// perform weighted mean, calc using time different between now and last
		List<Double> times = series.getTimes();
		List<Double> values= series.getValues();
		double len = times.get(times.size() - 1) - times.get(0);
		double tmp = 0.0;
		for(int i=1; i < values.size(); i++) {
			double dur = (times.get(i) - times.get(i -1)) / len;
			double cval = values.get(i);
			tmp += cval * dur;
		}
		final double mean = tmp;
		// find standard deviation
		double std = values.stream()
				.map( v -> v - mean)
				.map( v -> Math.pow(v, 2))
				.reduce((c,n) -> c + n)
				.get();
		std = std / values.size();
		std = Math.sqrt(std);
		// modify values and return new series
		for(int i=0; i < values.size(); i++) {
			double val = values.get(i);
			val = (val - mean) / std;
			ret.add(val);
		}
		List<RealTimePoint> retPoints = new ArrayList();
		for(int i=0; i < ret.size(); i++) {
			RealTimePoint point = new RealTimePoint(times.get(i), ret.get(i));
			retPoints.add(point);
		}
		return new RealTimeSeries(
				series.getName(),
				series.getColor(),
				retPoints);
	}
}
