package org.processmining.qut.exogenousaware.steps.slicing.data;

import java.util.ArrayList;
import java.util.List;

import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.qut.exogenousaware.data.ExogenousDatasetType;

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
	@NonNull XEvent endogenous;
	@Singular List<XEvent> subEvents;
	XTrace endoSource;
	
	
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
	
	public List<Double> getYSeries(){
		List<Double> y = new ArrayList<Double>();
		for(XEvent ev : this.subEvents) {
			String val = ev.getAttributes().get("exogenous:value").toString();
			y.add(Double.parseDouble(val));
		}
		return y;
	}
}
