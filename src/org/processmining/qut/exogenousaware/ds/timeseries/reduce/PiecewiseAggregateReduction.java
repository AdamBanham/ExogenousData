package org.processmining.qut.exogenousaware.ds.timeseries.reduce;

import java.util.ArrayList;
import java.util.List;

import org.processmining.qut.exogenousaware.ds.timeseries.data.RealTimePoint;
import org.processmining.qut.exogenousaware.ds.timeseries.data.RealTimeSeries;

public class PiecewiseAggregateReduction implements TimeSeriesReducer<RealTimeSeries> {
	
	private int numOfWindows;
	private int tempWindows;
	
	public PiecewiseAggregateReduction(int numOfWindows) {
		this.numOfWindows = numOfWindows;
		this.tempWindows = numOfWindows;
	}

	public RealTimeSeries reduce(RealTimeSeries series) {
		if (series.getSize() < numOfWindows) {
			System.out.println(String.format(
					"[PiecewiseAggregateReduction] PPA window reduced from %d to...",
					numOfWindows)
			);
			if (series.getSize() < 20) {
				numOfWindows = 1;
			} else {
				numOfWindows = (int)((series.getSize() * 0.05));
				numOfWindows = numOfWindows < 1 ? 1 : numOfWindows;
				numOfWindows = (series.getSize() / numOfWindows);
			}
			System.out.println("[TraceVisOverviewChart] ..."+numOfWindows);
		}
		List<Double> retValues = new ArrayList<Double>(numOfWindows);
		List<Double> retTimes = new ArrayList<Double>(numOfWindows);
		// perform aggregation
		// c^_i = w/n * sum(c_j)
		for(int i=1;i <= numOfWindows; i++) {
			int start = ((series.getSize()/ numOfWindows) * (i - 1)) + 1;
			int end = (series.getSize()/numOfWindows) * i;
			RealTimeSeries frame = series.splitSeriesByIndex(start, end+1);
			
			if (frame.getSize() == 1) {
				RealTimePoint point = frame.getPoints().get(0);
				retValues.add(point.getValue());
				retTimes.add(point.getValue());
			} else if (frame.getSize() < 1) {
				continue;
			} else {
				double mean = frame.computeWeightedMean();
				retValues.add(mean);
				retTimes.add(frame.getTimes().get(frame.getSize()-1));
			}
		}
		
		if (retValues.size() != numOfWindows) {
			System.out.println(String.format(
						"[TraceVisOverviewChart] PPA stream returned different size :: "
						+ "expected %d but saw %d.",
						numOfWindows, retValues.size())
			);
		}
		
//		build new time series
		RealTimeSeries ret;
		List<RealTimePoint> points = new ArrayList();
		for(int i=0; i < retValues.size(); i++) {
			points.add(new RealTimePoint(retTimes.get(i), retValues.get(i)));
		}
		ret = new RealTimeSeries(series.getName(), series.getColor(), points);
		
//		reset state
		this.numOfWindows = this.tempWindows;
		return ret;
	}

}
