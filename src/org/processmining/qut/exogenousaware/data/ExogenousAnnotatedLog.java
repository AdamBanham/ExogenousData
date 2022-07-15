package org.processmining.qut.exogenousaware.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.XVisitor;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.qut.exogenousaware.exceptions.LinkNotFoundException;
import org.processmining.qut.exogenousaware.gui.colours.ExoPanelPicker;
import org.processmining.qut.exogenousaware.steps.Slicing;
import org.processmining.qut.exogenousaware.steps.Transforming;
import org.processmining.qut.exogenousaware.steps.slicing.data.SlicingConfiguration;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.slicing.gui.SlicingConfigurationDialog;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.Singular;

/**
 * Data Construct for storing both endogenous and exogenous data within a event log structure
 * 
 * 
*/
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExogenousAnnotatedLog implements XLog {

	@Getter @NonNull XLog endogenousLog;
	@Singular @Getter List<ExogenousDataset> exogenousDatasets;
	@Singular List<XEventClassifier> classifiers;
	@Singular List<XAttribute> globalTraceAttributes;
	@Singular List<XAttribute> globalEventAttributes;
	@Default XAttributeMap attributes = new XAttributeMapImpl();
	@Singular Set<XExtension> extensions;
	@Default @Setter @Getter HashMap<String, Map<String,List<SubSeries>>> linkedSubseries = new HashMap<String, Map<String,List<SubSeries>>>();
	@Default XLog exoSubseries = null;
	@NonNull Boolean parsed;
	
//	configuration setup for exogenous aware log
	@Default private Boolean useDefaultConfiguration = false;
	@Default @Getter private SlicingConfiguration slicingConfig = null; 
	
	
	
	/**
	 * Creates an identical copy of this element.
	 * 
	 * @return An identical clone.
	 */
	@Override
	public ExogenousAnnotatedLog clone() {
		return new ExogenousAnnotatedLog(
				this.endogenousLog,
				this.exogenousDatasets,
				this.classifiers,
				this.globalTraceAttributes,
				this.globalEventAttributes,
				this.attributes,
				this.extensions,
				this.linkedSubseries,
				this.exoSubseries,
				false,
				false,
				this.slicingConfig
		);
	}
	
	private void handleConfigurationSetup(UIPluginContext context) {
//		TODO #2 ask for user setup if no configuration is given
		if (slicingConfig == null) {
			SlicingConfigurationDialog sdialog = SlicingConfigurationDialog.builder()
					.datasets(exogenousDatasets)
					.build()
					.setup();
			context.showWizard("Create your slicing configuration", true, false, sdialog);
			InteractionResult result = context.showWizard("Create your transforming configuration", false, true, sdialog);
			if (result == InteractionResult.FINISHED) {
				this.slicingConfig = sdialog.generateConfig();
			}
		}
	}
	
	public ExogenousAnnotatedLog setup(UIPluginContext context) {
//		check that configuration is setup
		handleConfigurationSetup(context);
		if (!this.parsed) {
//			set base colours for exo-panels
			ExoPanelPicker picker = ExoPanelPicker.builder().build();
			for( ExogenousDataset dset : this.exogenousDatasets) {
				dset.setColourBase(picker.getColour());
			}
			this.exoSubseries = new XFactoryNaiveImpl().createLog();
//			#1 for each endogenous trace, search each exogenous dataset for linked signals
//			## but first setup a progress bar
			Progress progress = context.getProgress();
			progress.setMaximum(this.endogenousLog.size());
			progress.setValue(0);
			progress.setCaption("Linking endogenous traces...");
//			run parallel work pool over handler
			List<List<XTrace>> subseries = this.endogenousLog.parallelStream()
				.map( trace -> handleEndogenousTraceLinkage(trace, progress))
				.collect(Collectors.toList());
//			add subseries collection into subseries Xlog
			progress.setMaximum(this.endogenousLog.size()+subseries.size());
			for(List<XTrace> seriesSet: subseries) {
				this.exoSubseries.addAll(seriesSet);
				progress.inc();
			}
		} else {
			System.out.println("[ExogenousAnnotatedLog] Finishing Import...");
			Progress progress = context.getProgress();
			progress.setMaximum(this.exoSubseries.size());
			progress.setValue(0);
			System.out.println(String.format("[ExogenousAnnotatedLog] Handling %d subseries...", this.exoSubseries.size()));
			for(XTrace subseries: this.exoSubseries) {
				String targetEv = subseries.getAttributes().get("exogenous:link:target").toString();
				SubSeries events = SubSeries.builder()
						.abvSlicingName("S")
						.slicingName("SliceImport")
						.subEvents(subseries)
						.build();
				Map<String, List<SubSeries>> mapping = new HashMap<String, List<SubSeries>>();
				List<SubSeries> lister = new ArrayList<SubSeries>();
				lister.add(events);
				mapping.put(subseries.getAttributes().get("exogenous:dataset:source").toString(), lister);
//				check for event linkage before hand, if so then update, otherwise wipe over key
				if (this.linkedSubseries.containsKey(targetEv)) {
					this.linkedSubseries.get(targetEv).putAll(mapping);
				} else {
					this.linkedSubseries.put(targetEv, mapping);
				}
				progress.inc();
			}
		}

		return this;
	}
	
	public List<XTrace> handleEndogenousTraceLinkage(XTrace endo, Progress progress) {
		List<XTrace> subseriesTraces = new ArrayList<XTrace>();
		// don't do work if we are cancelled :(
		if (progress.isCancelled()) {
			return subseriesTraces;
		}
//		the work
		for(ExogenousDataset elog: this.exogenousDatasets) {
//			if (elog.dataType.equals(ExogenousDatasetType.DISCRETE)) {
//				continue;
//			}
			List<XTrace> linked;
			try {
				linked = elog.findLinkage(endo); //Linking.findLinkedExogenousSignals(endo, elog);
			} catch (LinkNotFoundException e) {
				// if no link can be found then move on
				continue;
			}
//			#2 perform slicing and annotate subseries on each event 
			Map<String, Map<String, List<SubSeries>>> subseries;
			try {
//				TODO #1 handle to slicing configuration
				if (this.useDefaultConfiguration || this.slicingConfig == null) {
					subseries = Slicing.naiveEventSlicing(endo, linked, elog);
				} else {
					subseries = this.slicingConfig.slice(endo, linked, elog);
				}
			} catch (UnsupportedOperationException err) {
				System.out.println(
						"For endogenous trace ("
						+ endo.getAttributes().get("concept:name").toString()
						+ ") slicing hit edge case :: "
						+ err.getMessage()
						);
				return subseriesTraces;
			} catch (Exception err) {
				System.out.println("Unexpected error occured with naiveEventSlicing");
				err.printStackTrace();
				return subseriesTraces;
			}
			for(String evId: subseries.keySet()) {
				if (progress.isCancelled()) {
					return subseriesTraces;
				}
//				check for id if not add 
				if (!this.linkedSubseries.containsKey(evId)) {
					this.linkedSubseries.put(evId, subseries.get(evId));
				} 
//				else update keys
				else {
					if (this.linkedSubseries.containsKey(evId) && subseries.containsKey(evId)) {
						this.linkedSubseries.get(evId).putAll(subseries.get(evId));
					}
				}
//				addd number of links to each event
				Optional<XEvent> ev = endo.stream()
						.filter( event -> event.getID().toString().compareTo(evId) == 0)
						.findFirst();
				if (ev.isPresent()) {
					XEvent event = ev.get();
					XAttributeMap attrs = event.getAttributes();
					for(String exoDatasetName: subseries.get(evId).keySet() ) {
//						do not add links to datasets without any events
						if (subseries.get(evId).get(exoDatasetName).size() < 1) {
							continue;
						}
						String attrkey = "exogenous:dataset:"+exoDatasetName.toLowerCase().replaceAll(" ","")+":links";
						attrs.put(attrkey, 
								new XAttributeLiteralImpl(attrkey, 
										String.format("%d",subseries.get(evId).get(exoDatasetName).size())
								)
						);
						List<SubSeries> collectedSubSeries = subseries.get(evId).get(exoDatasetName);
						for(SubSeries subtimeseries: collectedSubSeries) {
	//						#3 perform transformation annotate with aggregated attributes
							List<TransformedAttribute> transforms = Transforming.applySlopeTransform(subtimeseries,exoDatasetName.toLowerCase().replaceAll(" ",""));
							for(TransformedAttribute transform: transforms) {
								attrs.put(transform.getKey(), transform);
							}
	//						#4 add subseries to subseries universe
							XAttributeMap trace_attrs = new XAttributeMapImpl();
							trace_attrs.put("concept:name", new XAttributeLiteralImpl("concept:name", exoDatasetName.toLowerCase().replaceAll(" ","")+"-event-"+event.getID().toString()));
							trace_attrs.put("exogenous:link:target", new XAttributeLiteralImpl("exogenous:link:target", event.getID().toString()));
							trace_attrs.put("exogenous:dataset:source", new XAttributeLiteralImpl("exogenous:dataset:source", exoDatasetName.toLowerCase().replaceAll(" ","")));
							XTrace subseries_trace = new XFactoryNaiveImpl().createTrace(trace_attrs);
							subseries_trace.addAll(subtimeseries.getSubEvents());
							attrs.put(
									"exogenous:subseries:"+exoDatasetName.toLowerCase().replaceAll(" ","")+"receiver", 
									new XAttributeLiteralImpl(
											"exogenous:subseries:"+exoDatasetName.toLowerCase().replaceAll(" ","")+"receiver",
											exoDatasetName.toLowerCase().replaceAll(" ","")+"-event-"+event.getID().toString()
									)
							);
							if (subseries_trace == null) {
								System.out.println("[ExogenousAnnotated] subseries trace is null");
							} else {
								subseriesTraces.add(subseries_trace);
							}
						}
					}
					attrs.put( 
							"endogenous:id",
							new XAttributeLiteralImpl(
									"endogenous:id",
									event.getID().toString()
							)
							
					);
					event.setAttributes(attrs);		
				}
			}
		}
		progress.inc();
		return subseriesTraces;
	}
	
	public String getEventExogenousLinkId(XEvent ev) {
		String id = null;
		if (!this.parsed) {
			id = ev.getID().toString();
		} else {
			if (ev.getAttributes().containsKey("endogenous:id")) {
				id = ev.getAttributes().get("endogenous:id").toString();
			} else {
				id = ev.getID().toString();
			}
		}
		return id;
	}
	
	public List<XTrace> getExogenousStream(String exoLink) {
		List<XTrace> exoseries = this.exoSubseries.stream().filter(trace -> trace.getAttributes().get("concept:name").toString().equals(exoLink)).collect(Collectors.toList());
		if (exoseries.size() < 1) {
			System.out.println("[ExogenousAnnotatedLog] no link found with id : " + exoLink);
		} else {
			System.out.println("[ExogenousAnnotatedLog] found links : "+exoLink+" :: "+exoseries.size());
			
		}
		return exoseries;
	}
	
	public List<XTrace> getExogenousStreams(List<String> exoLink) {
		List<XTrace> exoseries = this.exoSubseries.stream()
				.filter(trace -> exoLink.contains(trace.getAttributes().get("concept:name").toString()))
				.collect(Collectors.toList());
		if (exoseries.size() < 1) {
			System.out.println("[ExogenousAnnotatedLog] no link found with any of supplied ids ");
		} else {
			System.out.println("[ExogenousAnnotatedLog] found links :: "+exoseries.size());
		}
		return exoseries;
	}
	
	public XLogInfo getInfo(XEventClassifier classifier)
	{
		return this.endogenousLog.getInfo(classifier);
	}
	
	public void setInfo(XEventClassifier classifier, XLogInfo info)
	{
		this.endogenousLog.setInfo(classifier, info);
	}

	@Override
	public boolean hasAttributes() {
		// TODO Auto-generated method stub
		return this.endogenousLog.hasAttributes();
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return this.endogenousLog.size();
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return this.endogenousLog.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		// TODO Auto-generated method stub
		return this.endogenousLog.contains(o);
	}

	@Override
	public Iterator<XTrace> iterator() {
		// TODO Auto-generated method stub
		return this.endogenousLog.iterator();
	}

	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return this.endogenousLog.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		// TODO Auto-generated method stub
		return this.endogenousLog.toArray(a);
	}

	@Override
	public boolean add(XTrace e) {
		// TODO Auto-generated method stub
		return this.endogenousLog.add(e);
	}

	@Override
	public boolean remove(Object o) {
		// TODO Auto-generated method stub
		return this.endogenousLog.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return this.endogenousLog.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends XTrace> c) {
		// TODO Auto-generated method stub
		return this.endogenousLog.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends XTrace> c) {
		// TODO Auto-generated method stub
		return this.endogenousLog.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return this.endogenousLog.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return this.endogenousLog.retainAll(c);
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public XTrace get(int index) {
		// TODO Auto-generated method stub
		return this.endogenousLog.get(index);
	}

	@Override
	public XTrace set(int index, XTrace element) {
		// TODO Auto-generated method stub
		return this.endogenousLog.set(index, element);
	}

	@Override
	public void add(int index, XTrace element) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not Implemented");
	}

	@Override
	public XTrace remove(int index) {
		// TODO Auto-generated method stub
		return this.endogenousLog.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		// TODO Auto-generated method stub
		return this.endogenousLog.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		// TODO Auto-generated method stub
		return this.endogenousLog.lastIndexOf(o);
	}

	@Override
	public ListIterator<XTrace> listIterator() {
		// TODO Auto-generated method stub
		return this.endogenousLog.listIterator();
	}

	@Override
	public ListIterator<XTrace> listIterator(int index) {
		// TODO Auto-generated method stub
		return this.endogenousLog.listIterator(index);
	}

	@Override
	public List<XTrace> subList(int fromIndex, int toIndex) {
		// TODO Auto-generated method stub
		return this.endogenousLog.subList(fromIndex, toIndex);
	}

	@Override
	public boolean accept(XVisitor arg0) {
		// TODO Auto-generated method stub
		return this.endogenousLog.accept(arg0);
	}

}
