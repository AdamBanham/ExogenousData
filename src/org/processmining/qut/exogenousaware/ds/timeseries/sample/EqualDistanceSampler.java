package org.processmining.qut.exogenousaware.ds.timeseries.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.processmining.qut.exogenousaware.ds.linear.BestFittingLine;
import org.processmining.qut.exogenousaware.ds.timeseries.data.RealTimePoint;
import org.processmining.qut.exogenousaware.ds.timeseries.data.RealTimeSeries;

public class EqualDistanceSampler implements TimeSeriesSampler<RealTimeSeries> {

	private int sampleLength;
	
	public EqualDistanceSampler(int sampleLength) {
		this.sampleLength = sampleLength;
	}
	
	/**
	 * Resamples a given series and returns a series of equal distance measurements, 
	 * assuming the number of data points and resamples using linear interpolation.
	 */
	public RealTimeSeries sample(RealTimeSeries series) {
		List<RealTimePoint> points = new ArrayList();
		List<RealTimePoint> origPoints = series.getPoints();
//		construct timeline for resampling
		List<Double> timeline = buildEqualDistanceTimeline(origPoints);
		if (timeline.size() < 2) {
			return new RealTimeSeries();
		}
//		find leftmost and rightmost point from original
		RealTimePoint leftPoint = origPoints
				.stream()
				.reduce((c,n) -> c.getTime() > n.getTime() ? n : c)
				.get();
		RealTimePoint rightPoint = origPoints
				.stream()
				.reduce((c,n) -> c.getTime() < n.getTime() ? n : c)
				.get();
//		add leftmost
		points.add(new RealTimePoint(leftPoint.getTime(), leftPoint.getValue()));
//		resample inbetween on timeline
		for(double time : timeline.subList(1, timeline.size()-1)) {
			double sampleY = findSampleY(time, origPoints);
			if (Double.isNaN(sampleY)) {
				continue;
			}
			RealTimePoint samplePoint = new RealTimePoint(
					time, 
					sampleY
			);
			points.add(samplePoint);
		}
//		add rightmost
		points.add(new RealTimePoint(rightPoint.getTime(), rightPoint.getValue()));
//		System.out.println("completed sampling");
		return new RealTimeSeries(series.getName(), series.getColor(), points);
	}
	
	/**
	 * Builds an equal distance timeline across the x-axis for the given series.
	 * The leftmost and rightmost point will be from the series, the inbetween will
	 * be resampled from timespan between these two points.
	 * @param series
	 * @return A timeline across the x-axis
	 */
	public List<Double> buildEqualDistanceTimeline(List<RealTimePoint> series) {
		List<Double> times = series.stream()
				.map(p -> p.getTime())
				.collect(Collectors.toList());
//		step one: find leftmost and rightmost across series
		double right = times.stream().reduce((c,n) -> c < n ? n : c).get();
		double left = times.stream().reduce((c,n) -> c < n ? c : n).get();
//		System.out.println("Checking between :: "+ left + " to "+ right);
//		step two: create timeline such that each point at most (1/n)% away from its neighbours
		List<Double> timeline = new ArrayList<Double>();
		double timespan = Math.abs(Math.abs(right) - Math.abs(left));
		double interval = timespan / sampleLength;
		double current = left + interval;
//		System.out.println("building starting at :: "+current+" :: "+ interval);
//		build timeline
		timeline.add(left);
		while(current < right) {
			timeline.add(current);
			current += interval;
		}
		timeline.add(right);
//		System.out.println("Timeline built");
		if (timeline.size() < sampleLength) {
			throw new UnsupportedOperationException("[EqualDistanceSampler] Unable to compute a timeline :: Reason :: timeline was shorter than 100 intervals :: "+timeline.size());
		}
		return timeline;
	}
	
	/**
	 * Finds a sample of the y-axis value for the given x-axis point, using the 
	 * given series of real points.
	 * @param sampleX : The given x-axis point to sample for
	 * @param series : The real points of a series to use for sampling
	 * @return sample y-axis value
	 */
	public Double findSampleY(double sampleX, List<RealTimePoint> series) {
//		find the rightmost point of left
		RealTimePoint pairLeft = series.stream()
				.filter(p -> p.getTime() <= sampleX)
				.reduce((c, n) -> c.getTime() < n.getTime() ? n : c)
				.get();
//		find the leftmost point of right
		RealTimePoint pairRight = series.stream()
				.filter(p -> p.getTime() >= sampleX)
				.reduce((c, n) -> c.getTime() > n.getTime() ? n : c)
				.get();
//		calculate the best fitting line and find y at middle of sample window
		BestFittingLine sampler = BestFittingLine.builder()
				.X(pairLeft.getTime())
				.X(pairRight.getTime())
				.Y(pairLeft.getValue())
				.Y(pairRight.getValue())
				.build()
				.findSlope()
				.findIntercept();
		return sampler.findY(sampleX);
	}

}
