package org.processmining.qut.exogenousaware.ds.timeseries.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.processmining.qut.exogenousaware.ds.linear.BestFittingLine;

/**
 * Static class for time series analysis tools.
 * 
 * 
 * @author Adam Banham
 * */
public class TimeSeriesSampling {
	private TimeSeriesSampling() {};
	
	/**
	 * Finds a numerical sample for y from a given series of x,y data points.\n
	 * Using linear interpolation and two real points from the given series this function finds a y value at the middle of a given window.
	 * @param series
	 * @param sampleStart Start of window to consider
	 * @param sampleEnd End of window to consider
	 * @return the middle point of window, using linear interpolation between two real points
	 * @throws ArithmeticException
	 * @throws CloneNotSupportedException
	 */
	@SuppressWarnings("unchecked")
	public static double findSample(XYSeries series, Double sampleStart, Double sampleEnd) throws ArithmeticException,CloneNotSupportedException {
//		find a pair such that LHS is the lostmost of the sample point (mid) and RHS is the rightmost of the sample point
//		return a value for the mid point of the sample
		double sampleMiddle = sampleStart + ((sampleEnd - sampleStart) / 2.0);
		XYDataItem pairLeft = null;
		XYDataItem pairRight = null;
		List<XYDataItem> items = series.createCopy(0, series.getItemCount()-1).getItems();
//		find data points in sample window
		List<XYDataItem> windowedItems = items.stream()
				.filter(it -> it.getX().doubleValue() >= sampleStart)
				.filter(it -> it.getX().doubleValue() <= sampleEnd)
				.collect(Collectors.toList());
//		case (1) no window items
		if (windowedItems.size() > 0) {
//			case (1)(a) we have some datapoints in sample window
//			case (2) we only have data points to one side of the middle
			List<XYDataItem> leftmostItems = windowedItems.stream()
					.filter(it -> it.getX().doubleValue() < sampleMiddle)
					.collect(Collectors.toList());
			List<XYDataItem> rightmostItems = windowedItems.stream()
					.filter(it -> it.getX().doubleValue() > sampleMiddle)
					.collect(Collectors.toList());
			if (leftmostItems.size() < 1 || rightmostItems.size() < 1) {
//				case(2)(a) need to find either a leftmost or a rightmost to complete pair
				if (leftmostItems.size() < 1) {
//					case(2)(a)(i) find a possible left else throw
					List<XYDataItem> possibleLeft = items.stream()
							.filter(it -> it.getX().doubleValue() < sampleMiddle)
							.collect(Collectors.toList());
					if (possibleLeft.size() > 0) {
						leftmostItems.add(possibleLeft.get(possibleLeft.size()-1));
					} else {
						throw new ArithmeticException("Unable to find a leftmost element for this sample window.");
					}
				} 
				if (rightmostItems.size() < 1){
//					case(2)(a)(ii) find a possible right else throw
					List<XYDataItem> possibleRight = items.stream()
							.filter(it -> it.getX().doubleValue() > sampleMiddle)
							.collect(Collectors.toList());
					if (possibleRight.size() > 0) {
						rightmostItems.add(possibleRight.get(0));
					} else {
						throw new ArithmeticException("Unable to find a rightmost element for this sample window.");
					}
				}
				if (leftmostItems.size() > 0 && rightmostItems.size() > 0) {
					pairLeft = leftmostItems.get(leftmostItems.size()-1);
					pairRight = rightmostItems.get(0);
				} else {
					throw new ArithmeticException("Unable to find suitable pair witin sample window");
				}
			} else {
	//			case(3) we can make a pair
				pairLeft = leftmostItems.get(leftmostItems.size()-1);
				pairRight = rightmostItems.get(0);
			}
		} else {
//			case (1)(b) no data points within window, need to right leftmost and rightmost
			List<XYDataItem> possibleLeft = items.stream()
					.filter(it -> it.getX().doubleValue() < sampleMiddle)
					.collect(Collectors.toList());
			List<XYDataItem> possibleRight = items.stream()
					.filter(it -> it.getX().doubleValue() > sampleMiddle)
					.collect(Collectors.toList());
//			case (4) only items to one side of middle
			if (possibleLeft.size() < 1 || possibleRight.size() < 1) {
				throw new ArithmeticException("Unable to find suitable pair witin sample window");
			} else {
				pairLeft = possibleLeft.get(possibleLeft.size()-1);
				pairRight = possibleRight.get(0);
			}
		}
//		calculate the best fitting line and find y at middle of sample window
		BestFittingLine sampler = BestFittingLine.builder()
				.X(pairLeft.getXValue())
				.X(pairRight.getXValue())
				.Y(pairLeft.getYValue())
				.Y(pairRight.getYValue())
				.build()
				.findSlope()
				.findIntercept();
		return sampler.findY(sampleMiddle);
	}
	
	
	/**
	 * Resamples a series at a given frequency, adding a sample values into a bin map for future analysis.
	 * @param series to be resampled
	 * @param binMap a map to store new x,y samples from resampling
	 * @param resampleFrequency is how often the samples should be taken
	 */
	public static void resampleSeries(XYSeries series, Map<Double, List<Double>> binMap, double resampleFrequency) {
		double startx = series.getMinX();
		startx = startx + (startx % resampleFrequency);
		double endx = series.getMaxX();
		endx = endx + (endx % resampleFrequency);
//		sample between start and end using the window segment
		while (startx <= endx) {
			double sampleMiddle = startx + (resampleFrequency/2.0);
			sampleMiddle = sampleMiddle - (sampleMiddle % (resampleFrequency/2.0) );
			double sample = 0.0;
			boolean foundsample = false;
			try {
				sample = findSample(series, startx, startx+resampleFrequency);
				foundsample = true;
			} catch (Exception e) {
//				System.out.println("["+title+"] Unable to find sample in window :: "+startx+" to "+(startx+this.segmentInterval));
			}
			if(foundsample) {
					if (!binMap.containsKey(sampleMiddle)) {
						binMap.put(sampleMiddle, new ArrayList<Double>());
					}
					binMap.get(sampleMiddle).add(sample);
			}
			startx += resampleFrequency;
		}
	}
	
	/**
	 * Resamples a given series at a given frequency, and adds sample across a rolling window to smooth the given series
	 * @param series to be resampled
	 * @param binMap to store samples
	 * @param resampleFrequency is how often to resample
	 * @param resampleRollingWindow is how large the width of window is [-,+] sample point
	 */
	public static void resampleSeries(XYSeries series, Map<Double, List<Double>> binMap, double resampleFrequency, double resampleRollingWindow) {
		double startx = series.getMinX();
		startx = startx + (startx % resampleFrequency);
		double endx = series.getMaxX();
		endx = endx + (endx % resampleFrequency);
//		sample between start and end using the window segment
		while (startx <= endx) {
			double sampleMiddle = startx + (resampleFrequency/2.0);
			sampleMiddle = sampleMiddle - (sampleMiddle % (resampleFrequency/2.0) );
			double sample = 0.0;
			boolean foundsample = false;
			try {
				sample = findSample(series, startx, startx+resampleFrequency);
				foundsample = true;
			} catch (Exception e) {
//				System.out.println("["+title+"] Unable to find sample in window :: "+startx+" to "+(startx+this.segmentInterval));
			}
			if(foundsample) {
				for(double windowSample=sampleMiddle - resampleRollingWindow;
						   windowSample < (sampleMiddle+resampleRollingWindow);
						   windowSample += resampleFrequency) {
					double newwindowSample = windowSample - (windowSample % resampleFrequency);
					if (!binMap.containsKey(newwindowSample)) {
						binMap.put(newwindowSample, new ArrayList<Double>());
					}
					binMap.get(newwindowSample).add(sample);
				}
			}
			startx += resampleFrequency;
		}
	}
	
	
	/**
	 * Resamples a given series over a given timeline. 
	 * @param series to resample
	 * @param binMap to store 
	 * @param timeline to be used for sample points
	 */
	public static void resampleSeries(XYSeries series, Map<Double, List<Double>> binMap, List<Double> timeline) {
		try {
			//		setup interval space
					double min = series.getMinX();
					double max = series.getMaxX();
					double interval = timeline.get(1) - timeline.get(0);
					List<Double> intervalSpace = timeline.stream()
							.filter(i -> i >= min && i <= max)
							.collect(Collectors.toList());
			//		edge case (1) interval space is too small to sample from
					if (timeline.size() < 1) {
						return;
					}
			//		sample at each point in interval space
					for(double point: intervalSpace) {
						double sample;
						try {
							sample = findSample(series, point-interval, point+interval);
						} catch (ArithmeticException | CloneNotSupportedException e) {
							// skip interval point if sample cannot be computed
							// e.printStackTrace();
							continue;
						}
						if (!binMap.containsKey(point)) {
							binMap.put(point, new ArrayList<Double>());
						}
						binMap.get(point).add(sample);
					}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
	}
	
	/**
	 * Given a collection of XYSeries, finds a timeline which spans from least X to max X in 1% intervals.
	 * @param data Collection of series to be considered when creating timeline.
	 * @return the timeline for the given data.
	 */
	public static List<Double> findTimeline(XYSeriesCollection data) {
		return findTimeline(data,100);
	}
	
	public static List<Double> findTimeline(XYSeriesCollection data, int NumberOfIntervals) {
//		step one: find leftmost and rightmost across series
		double last = Double.MAX_VALUE;
		double first = Double.MIN_VALUE;
		for(XYSeries series: (List<XYSeries>) data.getSeries()) {
			double checklast = series.getMinX();
			double checkfirst = series.getMaxX();
			last = checklast < last ? checklast : last;
			first = checkfirst > first ? checkfirst : first;
		}
//		step two: create timeline such that each point at most (1/n)% away from its neighbours
		List<Double> timeline = new ArrayList<Double>();
		double timespan = Math.abs(Math.min(0, last)) + Math.abs(Math.max(0, first));
		double interval = timespan / NumberOfIntervals;
		double current = last + interval;
//		System.out.println("[TimeSeriesSampling] Computing timespan with the following :: last="+last+" first="+first+" interval="+interval+" timespan="+timespan);
//		build timeline
		timeline.add(last);
		while(current < first) {
			timeline.add(current);
			current += interval;
		}
		timeline.add(first);
		if (timeline.size() < NumberOfIntervals) {
			throw new UnsupportedOperationException("[TimeSeriesSampling] Unable to compute a timeline :: Reason :: timeline was shorter than 100 intervals :: "+timeline.size());
		}
		return timeline;
	}
}
