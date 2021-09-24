package org.processmining.plugins.balancedconformance;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.util.ui.widgets.helper.ProMUIHelper;
import org.processmining.framework.util.ui.widgets.helper.UserCancelledException;
import org.processmining.log.utils.XUtils;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PNWDTransition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.DataConformance.framework.VariableMatchCosts;
import org.processmining.plugins.astar.petrinet.impl.AbstractPDelegate;
import org.processmining.plugins.astar.petrinet.impl.PHead;
import org.processmining.plugins.astar.petrinet.impl.PILPTail;
import org.processmining.plugins.balancedconformance.GroupedTraces.GroupedTrace;
import org.processmining.plugins.balancedconformance.config.BalancedProcessorConfiguration;
import org.processmining.plugins.balancedconformance.config.BalancedProcessorConfiguration.ControlFlowStorageHandlerType;
import org.processmining.plugins.balancedconformance.config.BalancedProcessorConfiguration.DataStateStorageHandlerType;
import org.processmining.plugins.balancedconformance.config.DataConformancePlusConfiguration;
import org.processmining.plugins.balancedconformance.controlflow.ControlFlowAlignmentException;
import org.processmining.plugins.balancedconformance.controlflow.ControlFlowAlignmentResult;
import org.processmining.plugins.balancedconformance.controlflow.adapter.AlignmentAbstractAdapter.ControlFlowAlignmentConfig;
import org.processmining.plugins.balancedconformance.controlflow.adapter.AlignmentAbstractAdapter.EmptyTraceResult;
import org.processmining.plugins.balancedconformance.controlflow.adapter.AlignmentAdapter;
import org.processmining.plugins.balancedconformance.controlflow.adapter.AlignmentAdapter.StorageHandlerFactory;
import org.processmining.plugins.balancedconformance.controlflow.adapter.AlignmentAdapterILPGraphImpl;
import org.processmining.plugins.balancedconformance.controlflow.adapter.AlignmentAdapterILPTreeImpl;
import org.processmining.plugins.balancedconformance.controlflow.adapter.AlignmentDijkstraImpl;
import org.processmining.plugins.balancedconformance.controlflow.adapter.SearchMethod;
import org.processmining.plugins.balancedconformance.controlflow.override.storage.JavaCollectionStorageHandlerRWLock;
import org.processmining.plugins.balancedconformance.controlflow.override.storage.JavaCollectionStorageHandlerUnsynchronized;
import org.processmining.plugins.balancedconformance.controlflow.override.storage.MemoryEfficientStorageHandlerUnsynchronized;
import org.processmining.plugins.balancedconformance.dataflow.DataAlignmentAdapter;
import org.processmining.plugins.balancedconformance.dataflow.datastate.DataStateFactory;
import org.processmining.plugins.balancedconformance.dataflow.datastate.DataStateFactoryImpl;
import org.processmining.plugins.balancedconformance.dataflow.datastate.DataStateStore;
import org.processmining.plugins.balancedconformance.dataflow.datastate.DataStateStoreConcurrentImpl;
import org.processmining.plugins.balancedconformance.dataflow.datastate.DataStateStoreJavaNoLockImpl;
import org.processmining.plugins.balancedconformance.dataflow.datastate.DataStateStoreLockImpl;
import org.processmining.plugins.balancedconformance.dataflow.datastate.DataStateStoreMemoryOptimizedImpl;
import org.processmining.plugins.balancedconformance.dataflow.datastate.DataStateStoreMemoryOptimizedNoLockImpl;
import org.processmining.plugins.balancedconformance.dataflow.datastate.DataStateStoreNoLockImpl;
import org.processmining.plugins.balancedconformance.dataflow.exception.DataAlignmentException;
import org.processmining.plugins.balancedconformance.dataflow.exception.UnreliableDataAlignmentException;
import org.processmining.plugins.balancedconformance.dataflow.exception.UnreliableDataAlignmentException.UnreliableReason;
import org.processmining.plugins.balancedconformance.mapping.LogMapping;
import org.processmining.plugins.balancedconformance.mapping.Variable;
import org.processmining.plugins.balancedconformance.observer.DataConformancePlusObserver;
import org.processmining.plugins.balancedconformance.observer.DataConformancePlusObserver.ImpossibleTrace;
import org.processmining.plugins.balancedconformance.observer.DataConformancePlusObserverImpl;
import org.processmining.plugins.balancedconformance.observer.WrappedDataConformanceObserver;
import org.processmining.plugins.balancedconformance.result.BalancedDataAlignmentState;
import org.processmining.plugins.balancedconformance.result.BalancedReplayResult;
import org.processmining.plugins.balancedconformance.result.DataAlignedTrace;
import org.processmining.plugins.balancedconformance.result.MaxAlignmentCostHelper;
import org.processmining.plugins.balancedconformance.result.StatisticResult;
import org.processmining.plugins.balancedconformance.ui.DataConformanceConfigUIUtils;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset.Entry;
import com.google.common.util.concurrent.AtomicDouble;

import gnu.trove.map.hash.TObjectIntHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import nl.tue.astar.AStarThread.Canceller;
import nl.tue.astar.Tail;
import nl.tue.astar.impl.AbstractAStarThread.StorageHandler;
import nl.tue.astar.impl.DijkstraTail;
import nl.tue.astar.impl.State;
import nl.tue.astar.impl.memefficient.MemoryEfficientAStarAlgorithm;
import nl.tue.astar.impl.memefficient.MemoryEfficientStorageHandler;

public class DataConformanceJobber<L, T> {
	
	private static final class ImpossibleTraceImpl implements ImpossibleTrace {

		private XTrace trace;
		private String message;
		private UnreliableReason reason;

		public ImpossibleTraceImpl(XTrace trace, String message, UnreliableReason reason) {
			this.trace = trace;
			this.message = message;
			this.reason = reason;
		}

		public XTrace getTrace() {
			return trace;
		}

		public String getMessage() {
			return message;
		}

		public UnreliableDataAlignmentException.UnreliableReason getReason() {
			return reason;
		}

		public String toString() {
			return MessageFormat.format("Impossible to compute alignment for trace {0}. Error message: {1}",
					XUtils.getConceptName(getTrace()), getMessage());
		}

	}

	private static final int ERROR_REPORTING_LIMIT = 5;

	protected static final String RESULT_DESC = "Data Conformance Result for {0} on {1} @{1}";

	protected BalancedReplayResult preparePluginResult(DataPetriNet net, XLog log,
			DataConformancePlusConfiguration config, List<BalancedDataAlignmentState> dataAlignmentResults,
			long usedTime, AtomicDouble sumFitness) {

		config.getObserver().log(Level.INFO, "Preparing result ...");

		BalancedReplayResult resultReplay = new BalancedReplayResult(dataAlignmentResults, config.getVariableCost(),
				config.getVariableMapping(), net, log, config.getActivityMapping().getEventClassifier());

		if (config.getObserver() instanceof DataConformancePlusObserverImpl) {
			DataConformancePlusObserverImpl statisticsObserver = (DataConformancePlusObserverImpl) config.getObserver();
			Map<String, StatisticResult> statisticResults = statisticsObserver.getStatisticResults();
			resultReplay.getStatisticsStore().putAll(statisticResults);
		}

		resultReplay.setCalcTime(usedTime);

		return resultReplay;
	}

	protected BalancedDataAlignmentState convertAlignmentResult(BalancedDataAlignmentState state,
			AtomicDouble sumFitness) {
		return state;
	}
	
	public BalancedReplayResult doBalancedAlignmentDataConformanceChecking(PetrinetGraph net, Collection<XTrace> log,
			Progress progressListener, BalancedProcessorConfiguration config)
			throws ControlFlowAlignmentException, DataAlignmentException {
		return doBalancedDataConformanceChecking(convertToDPN(net), log, progressListener, config);
	}

	public static BalancedProcessorConfiguration queryConfiguration(final UIPluginContext context, DataPetriNet net,
			XLog log) throws UserCancelledException {

		BalancedProcessorConfiguration config = DataConformanceConfigUIUtils.queryBalancedAlignmentConfig(context);

		TransEvClassMapping activityMapping = DataConformanceConfigUIUtils.queryActivityMapping(context, net, log);

		config.setActivityMapping(activityMapping);
		config.setObserver(new DataConformancePlusObserverImpl(context));
		XEventClassifier eventClassifier = activityMapping.getEventClassifier();

		XEventClasses eventClasses = XUtils.createEventClasses(eventClassifier, log);

		DataConformanceConfigUIUtils.queryControlFlowAlignmentConfig(context, net, log, config, eventClasses);

		if (!net.getVariables().isEmpty()) {
			DataConformanceConfigUIUtils.queryDataAlignmentConfig(context, net, log, activityMapping, config);
		} else {

			VariableMatchCosts variableCost = BalancedProcessorConfiguration.createDefaultVariableCost(net,
					Collections.<String>emptySet(), 0, 0);
			config.setVariableCost(variableCost);
			config.setVariableMapping(Collections.<String, String>emptyMap());

			if (!hasGuards(net)) {
				ProMUIHelper.showWarningMessage(context,
						"Selected DPN-net does not define variables/guards. Alignment will not consider data!",
						"Variables/guards missing");
			}
		}

		return config;
	}

	protected static DataPetriNet convertToDPN(final PetrinetGraph net, PluginContext context) {
		if (net instanceof DataPetriNet) {
			return (DataPetriNet) net;
		} else {
			return DataPetriNet.Factory.viewAsDataPetriNet(net, context);
		}
	}

	protected static DataPetriNet convertToDPN(final PetrinetGraph net) {
		if (net instanceof DataPetriNet) {
			return (DataPetriNet) net;
		} else {
			return DataPetriNet.Factory.viewAsDataPetriNet(net);
		}
	}

	private static XLog getAsLog(Collection<XTrace> traces) {
		if (traces instanceof XLog) {
			return (XLog) traces;
		} else {
			XLog log = XFactoryRegistry.instance().currentDefault().createLog();
			log.addAll(traces);
			return log;
		}
	}

	private static boolean hasGuards(DataPetriNet net) {
		for (Transition t : net.getTransitions()) {
			if (t instanceof PNWDTransition && ((PNWDTransition) t).getGuardExpression() != null) {
				return true;
			}
		}
		return false;
	}

	public static String buildResultLabel(XLog log, PetrinetGraph net) {
		String logName = XConceptExtension.instance().extractName(log);
		return MessageFormat.format(RESULT_DESC, logName != null ? logName : "NULL", net.getLabel(), DateFormat
				.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date()));
	}

	protected BalancedReplayResult doBalancedDataConformanceChecking(final DataPetriNet net, final Collection<XTrace> traces,
			final Progress progressListener, final BalancedProcessorConfiguration config)
			throws ControlFlowAlignmentException, DataAlignmentException {

		final XLog log = getAsLog(traces);

		checkConfiguration(net, log, config);

		DataConformancePlusObserver observer = config.getObserver();

		if (!hasDataPerspective(net)) {
			config.setUsePartialDataAlignments(false);
		}

		config.setControlFlowAlignmentCanceller(new Canceller() {

			public boolean isCancelled() {
				return progressListener.isCancelled();
			}
		});

		final LogMapping logMapping = new LogMapping(config.getActivityMapping(), config.getMapEvClass2Cost(),
				config.getMapTrans2Cost(), config.getVariableMapping(), config.getVariableCost(),
				config.getUpperBounds(), config.getLowerBounds(), config.getDefaultValues(),
				config.getVariablesUnassignedMode(), net, log);

		ExecutorService pool = Executors.newFixedThreadPool(config.getConcurrentThreads());
		try {
			observer.log(Level.INFO, "Initializing the search ...");
			Stopwatch stopwatch = Stopwatch.createStarted();

			observer.startAlignment(log.size());
			observer.log(Level.INFO, "Start alignment using " + config.getConcurrentThreads() + " threads ...");

			try (DataAlignmentAdapter dataAlignmentAdapter = createDataAlignmentAdapter(config, logMapping, net)) {
				try (AlignmentAdapter controlFlowAlignmentAdapter = createControlFlowAlignmentAdapter(net, log, config,
						logMapping, dataAlignmentAdapter, observer)) {

					// Empty trace alignment & maximum cost estimation
					final MaxAlignmentCostHelper maxCostHelper = createMaxCostHelper(progressListener, config, log,
							logMapping, observer, controlFlowAlignmentAdapter);

					// Grouping of traces to avoid computations
					final GroupedTraces groupedTraces = createGroupedTraces(progressListener, log, observer,
							logMapping, pool, maxCostHelper);

					final Object[] expandedResults = new Object[log.size()];
					final AtomicDouble sumFitness = new AtomicDouble(0.0d);
					final Int2IntMap expandedIndicies = createGroupIndexMap(groupedTraces);
					// We use this Observer to 'expand' the results computed for groups and collect the real results in 'expandedResults'
					observer = createExpandingObserver(progressListener, observer, groupedTraces, expandedResults,
							expandedIndicies, sumFitness);

					// preparation progress
					progressListener.inc();

					List<Future<BalancedDataAlignmentState>> dataAlignmentFutures = computeAlignments(pool,
							progressListener, config, observer, logMapping, dataAlignmentAdapter,
							controlFlowAlignmentAdapter, maxCostHelper, groupedTraces);

					//TODO somehow LpSolve crashes when we collect the result as the come in ... that is why we wait here
					pool.shutdown();
					try {
						pool.awaitTermination(30, TimeUnit.DAYS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}

					List<BalancedDataAlignmentState> alignmentResults = gatherFutureResults(observer, groupedTraces, dataAlignmentFutures,
							expandedResults, expandedIndicies);

					stopwatch.stop();
					long usedTime = stopwatch.elapsed(TimeUnit.SECONDS);

					BalancedReplayResult result = preparePluginResult(net, log, config, alignmentResults, usedTime, sumFitness);

					observer.log(Level.INFO,
							MessageFormat.format("Alignment finished. Computation took: {0,number}s", usedTime));

					// final progress
					progressListener.inc();

					observer.finishedAlignment();

					return result;
				}
			}
		} finally {
			pool.shutdown();
		}
	}

	private GroupedTraces createGroupedTraces(final Progress progressListener, final XLog log,
			DataConformancePlusObserver observer, final LogMapping logMapping, ExecutorService pool,
			MaxAlignmentCostHelper maxCostHelper) {
		observer.log(Level.INFO, "Grouping identical traces ...");
		Stopwatch stopwatch = Stopwatch.createStarted();
		final GroupedTraces groupedTraces = groupTraces(pool, log, maxCostHelper, logMapping);
		int numResults = groupedTraces.size();
		observer.log(Level.INFO, String.format("Grouped %s traces to %s groups for alignment in %s ms...", log.size(),
				numResults, stopwatch.elapsed(TimeUnit.MILLISECONDS)));

		progressListener.setMinimum(0);
		progressListener.setValue(0);
		progressListener.setMaximum(numResults + 3);
		return groupedTraces;
	}

	private static boolean hasDataPerspective(final DataPetriNet net) {
		return !net.getVariables().isEmpty() || hasGuards(net);
	}

	private MaxAlignmentCostHelper createMaxCostHelper(final Progress progressListener,
			final BalancedProcessorConfiguration config, XLog log, LogMapping logMapping,
			DataConformancePlusObserver observer, AlignmentAdapter controlFlowAlignmentAdapter)
			throws ControlFlowAlignmentException {

		observer.log(Level.INFO, "Computing the reference alignment to an empty trace ");

		EmptyTraceResult emptyTraceResult = controlFlowAlignmentAdapter.calcEmptyTraceAlignment();
		ControlFlowAlignmentResult emptyTraceAlignment = emptyTraceResult.getControlFlowAlignment();
		DataAlignedTrace emptyTraceDataAlignment = emptyTraceResult.getDataAlignment();

		observer.log(Level.INFO, "Finished, alignment to the empty trace: ");
		observer.log(Level.INFO, emptyTraceDataAlignment.toString());

		progressListener.inc();

		return new MaxAlignmentCostHelper(emptyTraceAlignment, config.getActivityMapping(),
				logMapping.getEventClasses(), config.getMapEvClass2Cost(), config.getMapTrans2Cost(),
				config.getVariableCost());
	}

	interface RawResult {
		BalancedDataAlignmentState getState();

		int getIndex();
	}

	private DataConformancePlusObserver createExpandingObserver(final Progress progressListener,
			DataConformancePlusObserver observer, final GroupedTraces groupedTraces,
			final Object[] expandedAlignmentResults, final Int2IntMap expandedIndicies, final AtomicDouble sumFitness) {
		return new WrappedDataConformanceObserver(observer) {

			public void calculatedFitness(int resultIndex, XTrace representativeTrace, BalancedDataAlignmentState state) {
				// Called when fitness calculation is completed 
				progressListener.inc();
				int currentExpandedIndex = 0;
				int startExpandedIndex = expandedIndicies.get(resultIndex);
				// Expand the single result for the representativeTrace to all traces that will have the same alignment
				for (final BalancedDataAlignmentState expandedState : groupedTraces.getExpandedStates(state)) {
					final int ungroupedIndex = startExpandedIndex + currentExpandedIndex;
					expandedAlignmentResults[ungroupedIndex] = convertAlignmentResult(expandedState, sumFitness);
					currentExpandedIndex++;
					super.calculatedFitness(ungroupedIndex, expandedState.getOriginalTrace(), expandedState);
				}
			}

			public void foundOptimalAlignment(int resultIndex, XTrace representativeTrace,
					DataAlignedTrace alignedTrace, int partialDataAlignmentsNeeded, int cacheHit, int cacheSize,
					long queuedStates, long dataStateCount, long usedTime) {
				// Called as soon as there is an alignment
				List<XTrace> expandedTraces = groupedTraces.getTracesInGroup(representativeTrace);
				int currentExpandedIndex = 0;
				int startExpandedIndex = expandedIndicies.get(resultIndex);
				for (XTrace trace : expandedTraces) {
					int ungroupedIndex = startExpandedIndex + currentExpandedIndex;
					currentExpandedIndex++;
					super.foundOptimalAlignment(ungroupedIndex, trace, alignedTrace, partialDataAlignmentsNeeded,
							cacheHit, cacheSize, queuedStates, dataStateCount, usedTime);
				}
			}

			public void slowDataAlignmentDetected(int resultIndex, XTrace originalTrace,
					DataAlignedTrace dataAlignmentState, long usedTime) {
				List<XTrace> expandedTraces = groupedTraces.getTracesInGroup(originalTrace);
				int currentExpandedIndex = 0;
				int startExpandedIndex = expandedIndicies.get(resultIndex);
				for (XTrace trace : expandedTraces) {
					int ungroupedIndex = startExpandedIndex + currentExpandedIndex;
					currentExpandedIndex++;
					super.slowDataAlignmentDetected(ungroupedIndex, trace, dataAlignmentState, usedTime);
				}
			}

			public void unreliableAlignmentDetected(int resultIndex, XTrace originalTrace) {
				List<XTrace> expandedTraces = groupedTraces.getTracesInGroup(originalTrace);
				int currentExpandedIndex = 0;
				int startExpandedIndex = expandedIndicies.get(resultIndex);
				for (XTrace trace : expandedTraces) {
					int ungroupedIndex = startExpandedIndex + currentExpandedIndex;
					currentExpandedIndex++;
					super.unreliableAlignmentDetected(ungroupedIndex, trace);
				}
			}

		};
	}

	private GroupedTraces groupTraces(ExecutorService pool, final XLog log, MaxAlignmentCostHelper maxCostHelper,
			final LogMapping logMapping) {
		final GroupedTraces groupedTraces = new GroupedTraces(log, logMapping, maxCostHelper);
		List<List<XTrace>> partitionedLog = Lists.partition(log, 10000);
		if (partitionedLog.size() == 1) {
			for (XTrace trace : partitionedLog.iterator().next()) {
				groupedTraces.add(trace);
			}
		} else {
			List<Callable<Void>> callables = new ArrayList<>();
			for (final List<XTrace> subTraces : partitionedLog) {
				callables.add(new Callable<Void>() {

					public Void call() throws Exception {
						for (XTrace trace : subTraces) {
							groupedTraces.add(trace);
						}
						return null;
					}
				});
			}
			try {
				pool.invokeAll(callables);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		return groupedTraces;
	}

	private static void checkConfiguration(DataPetriNet net, XLog log, BalancedProcessorConfiguration config)
			throws DataAlignmentException {

		Set<Transition> transitions = ImmutableSet.copyOf(net.getTransitions());
		Set<Place> places = ImmutableSet.copyOf(net.getPlaces());

		for (Transition t : config.getActivityMapping().keySet()) {
			if (!transitions.contains(t)) {
				throw new DataAlignmentException.InvalidConfiguration(
						"Transition "
								+ t
								+ " is specified in the event class to transition mapping, but is not part of the supplied Petrinet! ");
			}
		}

		for (Place p : config.getInitialMarking()) {
			if (!places.contains(p)) {
				throw new DataAlignmentException.InvalidConfiguration("Place " + p
						+ " is specified in the initial marking, but is not part of the supplied Petrinet! ");
			}
		}

		for (Marking finalMarking : config.getFinalMarkings()) {
			for (Place p : finalMarking) {
				if (!places.contains(p)) {
					throw new DataAlignmentException.InvalidConfiguration("Place " + p
							+ " is specified in the final marking, but is not part of the supplied Petrinet! ");
				}
			}
		}

		for (XEventClass eventClass : config.getActivityMapping().values()) {
			if (!config.getMapEvClass2Cost().containsKey(eventClass)) {
				throw new DataAlignmentException.InvalidConfiguration(
						"Event class "
								+ eventClass
								+ " is specified in the event class to transition mapping, but there is no log move cost associated with this event class!");
			}
		}

		for (Transition t : config.getActivityMapping().keySet()) {
			if (!config.getMapTrans2Cost().containsKey(t)) {
				throw new DataAlignmentException.InvalidConfiguration(
						"Transition "
								+ t
								+ " is specified in the event class to transition mapping, but there is no model move cost associated with this transition!");
			}
		}

	}

	private List<Future<BalancedDataAlignmentState>> computeAlignments(ExecutorService pool,
			final Progress progressListener, final BalancedProcessorConfiguration config,
			DataConformancePlusObserver observer, final LogMapping logMapping,
			DataAlignmentAdapter dataAlignmentAdapter, AlignmentAdapter controlFlowAlignmentAdapter,
			MaxAlignmentCostHelper maxAlignmentCostHelper, final GroupedTraces groupedTraces) {

		List<Future<BalancedDataAlignmentState>> dataAlignmentTasks = new ArrayList<>();

		Iterator<Entry<GroupedTrace>> groupIter = groupedTraces.asMultiset().entrySet().iterator();
		int traceIndex = 0;
		while (groupIter.hasNext()) {
			Entry<GroupedTrace> groupedTrace = groupIter.next();
			XTrace trace = groupedTrace.getElement().getRepresentativeTrace();
			int groupSize = groupedTrace.getCount();

			float maxCostWithDelta = config.getCostDelta() + maxAlignmentCostHelper.getMaxCost(trace);
			int costLimit = (int) (maxCostWithDelta * (1.0d - config.getMaxCostFactor()));

			DataAlignmentTraceProcessor alignmentProcessor = new DataAlignmentTraceProcessor(traceIndex, trace,
					logMapping, controlFlowAlignmentAdapter, dataAlignmentAdapter, costLimit, observer,
					config.isUsePartialOrder());

			dataAlignmentTasks.add(pool.submit(new BalancedReplayResultProcessor(traceIndex, trace,
					maxAlignmentCostHelper, alignmentProcessor, observer, groupSize)));
			traceIndex++;
		}

		return dataAlignmentTasks;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private List<BalancedDataAlignmentState> gatherFutureResults(DataConformancePlusObserver observer, final GroupedTraces groupedTraces,
			List<Future<BalancedDataAlignmentState>> dataAlignmentFutures, Object[] expandedResults,
			Int2IntMap expandedIndicies) throws DataAlignmentException {

		List<ImpossibleTrace> impossibleTraces = new ArrayList<>();
		List<BalancedDataAlignmentState> alignmentResults = new ArrayList<>(expandedResults.length);

		for (int futureIndex = 0; futureIndex < dataAlignmentFutures.size(); futureIndex++) {
			Future<BalancedDataAlignmentState> futureState = dataAlignmentFutures.get(futureIndex);
			try {
				// Just collect valid results
				if (futureState.isDone()) {
					BalancedDataAlignmentState realState = null;
					// get group bounds 
					int startExpandedIndex = expandedIndicies.get(futureIndex);
					// go to the end if no next index available
					int nextGroupIndex = futureIndex + 1;
					int nextStartIndex;
					if (nextGroupIndex == expandedIndicies.size()) { // last index
						nextStartIndex = expandedResults.length;
					} else {
						nextStartIndex = expandedIndicies.get(nextGroupIndex);
					}
					// attempt to get a state
					try {
						try {
							realState = futureState.get();
						} catch (ExecutionException e) {
							if (e.getCause() instanceof ArrayIndexOutOfBoundsException) {
								// edge case for bitmask index in DataStateImpl not finding an index
								observer.log("[EDGE ERROR] bitmask failed when getting result, batch lost: " + futureIndex );
								continue;
							} else {
								throw e;
							}
						} catch (ArrayIndexOutOfBoundsException e){
							// edge case for bitmask index in DataStateImpl not finding an index
							observer.log("[EDGE ERROR] bitmask failed when getting result, batch lost: " + futureIndex );
							continue;
						}
						if (realState == null) {
							throw new DataAlignmentException("Could not retrieve result! Unkown reason.");
						}
						for (int i = startExpandedIndex; i < nextStartIndex; i++) {
							alignmentResults.add((BalancedDataAlignmentState) expandedResults[i]);
						}
					} catch (CancellationException e) {
						// Do not log, as this may affect a lot of traces
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				if (e.getCause() != null) {
					if (e.getCause() instanceof UnreliableDataAlignmentException) {
						final UnreliableDataAlignmentException alignmentException = (UnreliableDataAlignmentException) e
								.getCause();
						switch (alignmentException.getReason()) {
							case CANCELLED :
								// Ignore
								break;

							case MAXCOST :
							case IMPOSSIBLE :
								List<XTrace> expandedTraces = groupedTraces.getTracesInGroup(alignmentException
										.getTrace());
								for (XTrace t : expandedTraces) {
									impossibleTraces.add(new ImpossibleTraceImpl(t, alignmentException.getMessage(),
											alignmentException.getReason()));
								}
								break;

							default :
								throw new DataAlignmentException("Could not retrieve result!", e);
						}
					} else {
						throw new DataAlignmentException("Could not retrieve result!", e);
					}
					
					
				} else {
					throw new DataAlignmentException("Could not retrieve result!", e);
				}
			}
		}

		observer.foundImpossibleAlignments(impossibleTraces);
		logImpossibleAlignments(observer, impossibleTraces);

		return alignmentResults;
	}

	private void logImpossibleAlignments(DataConformancePlusObserver observer, List<ImpossibleTrace> impossibleTraces) {
		List<String> exceptionsMessages = new ArrayList<>(ERROR_REPORTING_LIMIT);
		// Add first few impossible traces to the list of exceptions
		for (ImpossibleTrace trace : FluentIterable.from(impossibleTraces).limit(ERROR_REPORTING_LIMIT)) {
			exceptionsMessages.add(trace.getMessage());
		}
		if (!exceptionsMessages.isEmpty()) {
			observer.log(
					Level.WARNING,
					"No alignment could be computed for "
							+ exceptionsMessages.size()
							+ " traces. Please check whether the model is sound and attributes in the event log have the correct data type. "
							+ "Showing the stack traces of the first 5 failures as debug information:\n\n"
							+ Joiner.on("\n\n").join(exceptionsMessages));
		}
	}

	private static Int2IntMap createGroupIndexMap(GroupedTraces groupedTraces) {
		Int2IntMap indexMap = new Int2IntOpenHashMap(groupedTraces.size());
		int groupIndex = 0;
		int expandedStartIndex = 0;
		for (Entry<GroupedTrace> group : groupedTraces.asMultiset().entrySet()) {
			indexMap.put(groupIndex++, expandedStartIndex);
			expandedStartIndex += group.getCount();
		}
		return indexMap;
	}

	private static ControlFlowAlignmentConfig convertToControlFlowConfig(final BalancedProcessorConfiguration config) {
		ControlFlowAlignmentConfig cfConfig = new ControlFlowAlignmentConfig();
		cfConfig.activateCache = config.isActivateDataViewCache();
		cfConfig.canceller = config.getControlFlowAlignmentCanceller();
		cfConfig.costDelta = config.getCostDelta();
		cfConfig.finalMarkings = config.getFinalMarkings();
		cfConfig.initialMarking = config.getInitialMarking();
		cfConfig.mapEvClass2Cost = config.getMapEvClass2Cost();
		cfConfig.mapTransition2Cost = config.getMapTrans2Cost();
		cfConfig.timeLimit = config.getTimeLimitPerTrace();
		cfConfig.maxNumOfStates = config.getMaxQueuedStates();
		cfConfig.queueingModel = config.getQueueingModel();
		cfConfig.sorting = config.getSorting();
		cfConfig.transitionMapping = config.getActivityMapping();
		cfConfig.useOptimizations = config.isUseOptimizations();
		cfConfig.usePartialOrder = config.isUsePartialOrder();
		return cfConfig;
	}

	static AlignmentAdapter createControlFlowAlignmentAdapter(final DataPetriNet net, final XLog log,
			final BalancedProcessorConfiguration config, LogMapping logMapping,
			DataAlignmentAdapter dataAlignmentAdapter, DataConformancePlusObserver observer) {
		ControlFlowAlignmentConfig cfConfig = convertToControlFlowConfig(config);
		AlignmentAdapter controlFlowAlignmentAdapter;
		switch (config.getSearchMethod()) {
			case DIJKSTRA :
				controlFlowAlignmentAdapter = new AlignmentDijkstraImpl(log, net, logMapping, cfConfig,
						BalancedDataConformancePlusPlugin.<DijkstraTail>getStorageHandlerFactory(
								config.getControlFlowStorageHandler(), config.getDataStateStorageHandler(),
								dataAlignmentAdapter), config.isKeepControlFlowSearchSpace(),
						config.isKeepDataFlowSearchSpace(), observer, dataAlignmentAdapter);
				break;

			case ASTAR_GRAPH :
				controlFlowAlignmentAdapter = new AlignmentAdapterILPGraphImpl(log, net, logMapping, cfConfig,
						BalancedDataConformancePlusPlugin.<PILPTail>getStorageHandlerFactory(
								config.getControlFlowStorageHandler(), config.getDataStateStorageHandler(),
								dataAlignmentAdapter), config.isKeepControlFlowSearchSpace(),
						config.isKeepDataFlowSearchSpace(), observer, dataAlignmentAdapter);
				break;

			case ASTAR_TREE :
				controlFlowAlignmentAdapter = new AlignmentAdapterILPTreeImpl(log, net, logMapping, cfConfig,
						BalancedDataConformancePlusPlugin.<PILPTail>getStorageHandlerFactory(
								config.getControlFlowStorageHandler(), config.getDataStateStorageHandler(),
								dataAlignmentAdapter), config.isKeepControlFlowSearchSpace(),
						config.isKeepDataFlowSearchSpace(), observer, dataAlignmentAdapter);
				break;

			default :
				throw new UnsupportedOperationException("Unkown search method " + config.getSearchMethod());
		}
		return controlFlowAlignmentAdapter;
	}

	static DataAlignmentAdapter createDataAlignmentAdapter(final BalancedProcessorConfiguration config,
			LogMapping logMapping, DataPetriNet net) {
		@SuppressWarnings("rawtypes")
		DataStateFactory dataStateFactory = new DataStateFactoryImpl(Maps.transformValues(logMapping.getVariables(),
				new Function<Variable, Class>() {

					@SuppressWarnings("rawtypes")
					public Class apply(Variable variable) {
						return variable.getType();
					}
				}));
		if (hasDataPerspective(net)) {
			if (!config.isUsePartialDataAlignments()) {
				return new DataAlignmentAdapter.LpSolveWithoutPartialDataAlignmentAdapter(logMapping, dataStateFactory,
						config.getConcurrentThreads(), config.getCostDelta(),
						config.getIncludeVirtualVariablesInTrace());
			} else {
				switch (config.getIlpSolver()) {
					case ILP_GUROBI :
						return new DataAlignmentAdapter.GurobiDataAlignmentAdapter(logMapping, dataStateFactory,
								config.getCostDelta(), config.getIncludeVirtualVariablesInTrace(),
								(config.getSearchMethod() != SearchMethod.ASTAR_TREE ? true : false));
					case ILP_LPSOLVE :
						return new DataAlignmentAdapter.LpSolveDataAlignmentAdapter(logMapping, dataStateFactory,
								config.getConcurrentThreads(), config.getCostDelta(),
								config.getIncludeVirtualVariablesInTrace(),
								(config.getSearchMethod() == SearchMethod.ASTAR_GRAPH ? true : false));
					default :
						throw new UnsupportedOperationException("Unkown ILSolver " + config.getIlpSolver());

				}
			}
		} else {
			return new DataAlignmentAdapter.NoDataAlignmentAdapter(logMapping, dataStateFactory);
		}
	}

	protected static <T extends Tail> StorageHandlerFactory<T> getStorageHandlerFactory(
			final ControlFlowStorageHandlerType controlFlowStorageHandler,
			final DataStateStorageHandlerType dataStateStorageHandler, final DataAlignmentAdapter dataAlignmentAdapter) {
		return new StorageHandlerFactory<T>() {

			public StorageHandler<PHead, T> newStorageHandler(AbstractPDelegate<T> delegate) {
				switch (controlFlowStorageHandler) {
					case MEMORY_EFFICIENT :
						return new MemoryEfficientStorageHandlerUnsynchronized<>(new MemoryEfficientAStarAlgorithm<>(
								delegate, 32 * 1024, 32 * 1024, 8));

					case MEMORY_EFFICIENT_LOCKING :
						return new MemoryEfficientStorageHandler<>(new MemoryEfficientAStarAlgorithm<>(delegate,
								32 * 1024, 32 * 1024, 8));

					case CPU_EFFICIENT :
						return new JavaCollectionStorageHandlerUnsynchronized<>(delegate,
								new TObjectIntHashMap<PHead>(), new ArrayList<State<PHead, T>>());

					case CPU_EFFICIENT_LOCKING :
						return new JavaCollectionStorageHandlerRWLock<>(delegate, new TObjectIntHashMap<PHead>(),
								new ArrayList<State<PHead, T>>());

					default :
						throw new UnsupportedOperationException("Unkown type" + controlFlowStorageHandler);
				}
			}

			public DataStateStore newDataStateStore() {
				switch (dataStateStorageHandler) {
					case CAS :
						return new DataStateStoreConcurrentImpl(32 * 1024);

					case RWLOCK :
						return new DataStateStoreLockImpl(32 * 1024);

					case MEMORY_OPTIMIZED :
						return new DataStateStoreMemoryOptimizedImpl(dataAlignmentAdapter.getDataStateFactory(),
								32 * 1024);

					case MEMORY_OPTIMIZED_NOLOCK :
						return new DataStateStoreMemoryOptimizedNoLockImpl(dataAlignmentAdapter.getDataStateFactory(),
								32 * 1024);

					case PRIMITIVE_NOLOCK :
						return new DataStateStoreNoLockImpl(32 * 1024);

					case JAVA_NOLOCK :
						return new DataStateStoreJavaNoLockImpl(32 * 1024);

					default :
						throw new UnsupportedOperationException("Unkown type" + dataStateStorageHandler);
				}
			}
		};
	}
}
