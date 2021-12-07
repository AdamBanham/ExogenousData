package org.processmining.qut.exogenousaware.gui.workers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.qut.exogenousaware.data.ExogenousAnnotatedLog;
import org.processmining.qut.exogenousaware.data.ExogenousDatasetType;
import org.processmining.qut.exogenousaware.gui.dot.ExoDotTransition;
import org.processmining.qut.exogenousaware.gui.workers.helpers.ExogenousObserverGrouper;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class ExogenousObservedUniverse extends SwingWorker<Map<String,XYSeriesCollection>, Double>{

	@NonNull private ExogenousAnnotatedLog log;
	@NonNull private PNRepResult alignment;
	@NonNull private ExoDotTransition focus;
	@NonNull private JProgressBar progress;
	@NonNull private JLabel label;
	@NonNull private ExogenousObserverGrouper grouper;
	
	@Getter @Default private Map<String,XYSeriesCollection> observed = new HashMap<String, XYSeriesCollection>();
	@Getter @Default private Map<String, List<Map<String,Object>>> datasetStates = new HashMap<String, List<Map<String,Object>>>();
	@Getter @Default private Map<String, List<Map<String,Object>>> seriesStates = new HashMap<String, List<Map<String,Object>>>();
	@Getter @Default private Map<String, List<Integer>> seriesGroups = new HashMap<String, List<Integer>>();
	@Default private double increment = 1;
	@Default private int max = 1;
	
	public ExogenousObservedUniverse setup() {
		this.increment = 100/ this.alignment.size();
		this.max = this.alignment.size();
		this.progress.setMaximum(this.alignment.size() * 1);
		this.progress.setValue(0);
		return this;
	}
	
	public List<Object> handleAlignment(SyncReplayResult align) {
//		containers for this alignment
		List<Map<String,TransformedAttribute>> lastupdate = new ArrayList<Map<String,TransformedAttribute>>();
		List<Map<String,Object>> lastDataUpdate = new ArrayList<Map<String,Object>>();
//		find when in the alignment the focused transition occurred
		List<Integer> occured = new ArrayList<Integer>();
		for(int i=0;i < align.getStepTypes().size();i++) {
			if (align.getStepTypes().get(i) != StepTypes.L) {
				Transition trans = (Transition) align.getNodeInstance().get(i);
				if(trans.getId().toString().equals(this.focus.getControlFlowId())) {
					occured.add(i != 0 ? i : 1);
				}
			}
		}
//		nothing to do if focused event does not occur
		if (occured.size() < 1) {
			return new ArrayList<Object>();
		}
//		work out the actual event index based on when a aligned event occurred
		List<Integer> eventIds = new ArrayList<Integer>();
		for(int occurance : occured) {
			eventIds.add(this.countEventSteps(align.getStepTypes().subList(0, occurance)));
		}
//		for each trace instance of this trace variant (alignments are grouped by variants)
		for(Integer traceId: align.getTraceIndex()) {
//			get the trace for a variant instance of this alignment
			XTrace trace = this.log.get(traceId);
//			for each observed aligned transition, which occurred at i-th event of the trace
			for (Integer eventId: eventIds) {
//				for all occurrences of focus node, create a mapping with exogenous data set and last update
				Map<String,TransformedAttribute> updates = new HashMap<String,TransformedAttribute>();
				Map<String, Object> state = new HashMap<String,Object>();
//				walk through events, up to each occurrence and collect the last variable assignment
				for(int i=0;i < eventId;i++) {
//					get an events attributes and track variable assignment
					try {
					XEvent event = trace.get(i);
					XAttributeMap attrs = (XAttributeMap) event.getAttributes().clone();
//					update data state for evaluation
					for(Entry<String, XAttribute> vals : attrs.entrySet()) {
						state.put(vals.getKey(), vals.getValue());
//						check to see what transformed attributes exist
//						and what data sets they referenced
						if (TransformedAttribute.class.equals(vals.getValue().getClass())) {
							TransformedAttribute attr = (TransformedAttribute) vals.getValue();
							updates.put(attr.getKey(), attr);
						}
					}
					} catch (IndexOutOfBoundsException e) {
						continue;
					}
					state.put("aligment:event:occurance", eventId);
				}
				lastupdate.add(updates);
				lastDataUpdate.add(state);
			}
		}
		return new ArrayList<Object>() {{ add(lastupdate);add(lastDataUpdate); }};
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Map<String,XYSeriesCollection> doInBackground() throws Exception {
		List<Map<String,TransformedAttribute>> lastupdate = new ArrayList<Map<String,TransformedAttribute>>();
		List<Map<String,Object>> lastDataUpdate = new ArrayList<Map<String,Object>>();
//		process all alignments to get data states
		List<List<Object>> outs = this.alignment.parallelStream()
				.map(align -> this.handleAlignment(align))
				.collect(Collectors.toList());
		outs.stream()
				.forEach( out -> {
					this.progress.setValue(this.progress.getValue()+1);
					if (out.size() == 2) {
						try {
						lastupdate.addAll((Collection<? extends Map<String, TransformedAttribute>>) out.get(0));
						lastDataUpdate.addAll((Collection<? extends Map<String, Object>>) out.get(1));
						} catch (ClassCastException e) {
							
						}
					}
				});
//		move to next stage of work, getting the numerical series
		this.progress.setMaximum(lastupdate.size());
		this.progress.setValue(0);
		System.out.println("[ExogenousObservedUniverse] num of stored updates : "+ lastupdate.size());
//		convert exogenous transformed attributes into a mapping between exogenous data set and exogenous sub-time series
		Map<String, List<SubSeries>> datasetValues = new HashMap<String, List<SubSeries>>();
		int counter = 0;
		for(Map<String, TransformedAttribute> xattrs : lastupdate) {
			for(Entry<String, TransformedAttribute> xattr : xattrs.entrySet()) {
				if (xattr.getValue().getSource().getDatatype().equals(ExogenousDatasetType.DISCRETE)) {
					continue;
				}
				String dataset = xattr.getValue().getSource().buildPrefix(true);
				if (datasetValues.containsKey(dataset)) {
					datasetValues.get(dataset).add(xattr.getValue().getSource());
					this.datasetStates.get(dataset).add(lastDataUpdate.get(counter));
//					work out what group this series belongs to
					this.seriesGroups.get(dataset).add(
							this.grouper.findGroup(
									xattr.getValue().getSource().getEndoSource(),
									xattr.getValue().getSource(),
									(int) lastDataUpdate.get(counter).get("aligment:event:occurance")
							)
					);
				} else {
					datasetValues.put(dataset, new ArrayList<SubSeries>());
					datasetValues.get(dataset).add(xattr.getValue().getSource());
					this.datasetStates.put(dataset, new ArrayList<Map<String,Object>>());
					this.datasetStates.get(dataset).add(lastDataUpdate.get(counter));
					this.seriesGroups.put(dataset, new ArrayList<Integer>());
					this.seriesGroups.get(dataset).add(
							this.grouper.findGroup(
									xattr.getValue().getSource().getEndoSource(),
									xattr.getValue().getSource(),
									(int) lastDataUpdate.get(counter).get("aligment:event:occurance")
							)
					);
				}
			}
			counter++;
			this.progress.setValue(this.progress.getValue()+1);
		}
//		convert collections of traces to collection of series
		this.progress.setMaximum(datasetValues.keySet().size());
		this.progress.setValue(0);
		for(Entry<String, List<SubSeries>> entry : datasetValues.entrySet()) {
			int seriescount = 1;
			counter = 0;
			try {
//			check for collection existing
			if(!this.observed.containsKey(entry.getKey())) {
				this.observed.put(entry.getKey(), new XYSeriesCollection());
				this.seriesStates.put(entry.getKey(), new ArrayList<Map<String, Object>>());
			}
			XYSeriesCollection dataset = this.observed.get(entry.getKey());
//			add all series to the same dataset for visualisation later
			for(SubSeries subseries : entry.getValue()) {
				if (subseries.getDatatype().equals(ExogenousDatasetType.DISCRETE)) {
					continue;
				}
				XYSeries series = new XYSeries(subseries.getDataset()+"-"+seriescount,true);
				List<Long> xseries = subseries.getXSeries();
				List<Double> yseries = subseries.getYSeries();
				for(int i=0; i < xseries.size(); i++) {
					series.add(
							((double) xseries.get(i)) / (1000.0 * 60.0 * 60.0)
							,
							yseries.get(i)
							
					);
				}
//				System.out.println("[ExogenousObservedUniverse] num of items in series :: " + series.getItemCount() + " :: " +trace.size());
				if(series.getItemCount() > 0) {
					dataset.addSeries(series);
					this.seriesStates.get(entry.getKey()).add(this.datasetStates.get(entry.getKey()).get(counter));
					seriescount++;
				}
				counter++;
				
			}
			} catch (Exception e) {
				System.out.println("[ExogenousObservedUniverse] Failed to convert stream to XYSeries : "+e.getMessage());
				e.printStackTrace();
			}
			this.progress.setValue(this.progress.getValue()+1);
		}
		return this.observed;
	}
	
	public Integer countEventSteps(List<StepTypes> steps) {
		int count = 0;
		for(StepTypes step: steps) {
			if(!step.equals(StepTypes.MINVI) && !step.equals(StepTypes.MREAL)) {
				count++;
			}
		}
		return count;
	}
	

}
