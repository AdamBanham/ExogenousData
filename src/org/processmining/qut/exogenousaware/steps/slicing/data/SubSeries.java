package org.processmining.qut.exogenousaware.steps.slicing.data;

import java.util.ArrayList;
import java.util.List;

import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.qut.exogenousaware.data.ExogenousDataset;
import org.processmining.qut.exogenousaware.data.ExogenousDatasetType;
import org.processmining.qut.exogenousaware.ds.timeseries.data.RealTimePoint;
import org.processmining.qut.exogenousaware.ds.timeseries.data.RealTimeSeries;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;

/**
 * 
 * Stores useful information for annotation around a sub-timeseries, such as the slicing function name.<br>
 * 
 * This class uses the builder design pattern. Call SubSeries.builder() to create new instances.
 * @author Adam Banham
 *
 */
@Builder
@Data
public class SubSeries {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5543479177811798739L;
	
	@NonNull String slicingName;
	@NonNull String abvSlicingName;
	@NonNull String dataset;
	@NonNull XTrace source;
	@NonNull ExogenousDatasetType datatype;
	@NonNull ExogenousDataset comesFrom;
	@NonNull XEvent endogenous;
	@Singular List<XEvent> subEvents;
	XTrace endoSource;
	
//	public enums
	
	public enum Scaling {
		ms(1.0),
		sec(1000.0),
		min(1000.0 * 60.0),
		hour(1000.0 * 60.0 * 60.0);
		
		private Double scale;
		
		private Scaling(double scale) {
			this.scale = scale;
		}
		
		public Double scale(double value) {
			return value / scale;
		}
	}
	
	
	/**
	 * @return The length of sub-timeseries
	 */
	public int size() {
		return this.subEvents.size();
	}
	
	/**
	 * Builds a pretty prefix for a annotation.
	 * @param datasetName name of the exogenous attribute
	 * @param shorten should the name be abbreviated or not
	 * @return new attribute prefix for transformation step
	 */
	public String buildPrefix(String datasetName, boolean shorten) {
		if (!shorten) {
			return datasetName.toLowerCase().replaceAll(" ","")+":"+this.slicingName;
		} else {
			return datasetName.toLowerCase().replaceAll(" ","")+":"+this.abvSlicingName;
		}
	}
	
	public String buildPrefix(boolean shorten) {
		if (!shorten) {
			return this.dataset.toLowerCase().replaceAll(" ","")+":"+this.slicingName;
		} else {
			return this.dataset.toLowerCase().replaceAll(" ","")+":"+this.abvSlicingName;
		}
	}
	
	public List<Long> getXSeries(){
		return getXSeries(true);
	}
	
	/**
	 * Returns the time index for this subseries, relevant to the endogenous event
	 * 
	 * @param relative Should returned longs be calculated from endogenous momment
	 * 
	 * @return a list of time stamps for exogenous measurements
	 */
	public List<Long> getXSeries(Boolean relative){
		List<Long> x = new ArrayList<Long>();
		long start = relative ? ( (XAttributeTimestamp) this.endogenous.getAttributes().get("time:timestamp")).getValueMillis() : 0;
		for(XEvent ev : this.subEvents) {
			XAttributeTimestamp ts = (XAttributeTimestamp) ev.getAttributes().get("time:timestamp");
			x.add(ts.getValueMillis()- start);
		}
		return x;
	}
	
	public List<Long> getXSeries(Boolean relative, Scaling scale){
		List<Long> x = new ArrayList<Long>();
		long start = relative ? ( (XAttributeTimestamp) this.endogenous.getAttributes().get("time:timestamp")).getValueMillis() : 0;
		for(XEvent ev : this.subEvents) {
			XAttributeTimestamp ts = (XAttributeTimestamp) ev.getAttributes().get("time:timestamp");
			x.add(ts.getValueMillis()- start);
		}
		return x;
	}
	
	
	public List<Double> getYSeries(){
		List<Double> y = new ArrayList<Double>();
		for(XEvent ev : this.subEvents) {
			String val = ev.getAttributes().get("exogenous:value").toString();
			y.add(Double.parseDouble(val));
		}
		return y;
	}
	
	public RealTimeSeries getTimeSeries() {
		return getTimeSeries(Scaling.ms);
	}
	
	/**
	 * Collect a time series representation for this slice. Uses relative time 
	 * for the time axis (using the associated event).
	 * @param timeScale : How to scale the time axis
	 * @return A time series representation of slice.
	 */
	public RealTimeSeries getTimeSeries(Scaling timeScale) {
		List<Long> times = getXSeries(true);
		List<Double> values = getYSeries();
		List<RealTimePoint> points = new ArrayList();
		for(int i=0; i<times.size(); i++) {
			points.add(new RealTimePoint(
					timeScale.scale(times.get(i)),
					values.get(i)
					)
			);
		}
		return new RealTimeSeries(
				buildPrefix(true),
				getComesFrom().getColourBase(),
				points
		);
	}
	
	public RealTimeSeries getSourceTimeSeries() {
		return getSourceTimeSeries(Scaling.ms);
	}
	
	/**
	 * Collects a time series representation of the source of this slice.
	 * @param timeScale : scaling to use on time axis
	 * @return 
	 */
	public RealTimeSeries getSourceTimeSeries(Scaling timeScale) {
		List<RealTimePoint> points = new ArrayList();
		long startingTraceTime = 
				( (XAttributeTimestamp) 
						this.endoSource.get(0).getAttributes().get("time:timestamp")
				).getValueMillis();
		for( XEvent measure : source) {
			long time = ((XAttributeTimestamp) 
					measure.getAttributes().get("time:timestamp")
				).getValueMillis();
			double val = Double.parseDouble(
					measure.getAttributes().get("exogenous:value").toString()
				);
			points.add(new RealTimePoint(
					timeScale.scale(time - startingTraceTime),
					val)
				);
		}
		return new RealTimeSeries( 
				getDataset(),
				getComesFrom().getColourBase(),
				points
		);
	}
	
}
