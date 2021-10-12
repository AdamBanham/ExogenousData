package org.processmining.qut.exogenousaware.data.storage.workers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingWorker;

import org.processmining.datadiscovery.estimators.DecisionTreeBasedFunctionEstimator;
import org.processmining.datadiscovery.estimators.FunctionEstimation;
import org.processmining.datadiscovery.estimators.FunctionEstimator;
import org.processmining.datadiscovery.estimators.Type;
import org.processmining.datadiscovery.estimators.impl.OverlappingEstimatorLocalDecisionTree;
import org.processmining.datadiscovery.model.DecisionPointResult;
import org.processmining.datadiscovery.model.DiscoveredPetriNetWithData;
import org.processmining.datadiscovery.plugins.DecisionMining;
import org.processmining.datadiscovery.plugins.alignment.TraceProcessor;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.framework.plugin.Progress;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PNWDTransition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithDataFactory;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.qut.exogenousaware.data.storage.ExogenousDiscoveryInvestigation;

import com.google.common.util.concurrent.AtomicLongMap;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class InvestigationTask extends SwingWorker<DiscoveredPetriNetWithData, Integer> {

	@NonNull private ExogenousDiscoveryInvestigation source;
	
	@Default @Getter private int maxConcurrentThreads = Runtime.getRuntime().availableProcessors() > 3 ? Runtime.getRuntime().availableProcessors() - 2 : 1;
	@Default @Getter private ThreadPoolExecutor pool = null;
	@Default private Map<Place, FunctionEstimator> estimators = new HashMap<>();
	@Default private AtomicLongMap<Transition> numberOfExecutions = AtomicLongMap.create();
	@Default private Map<Transition, AtomicLongMap<String>> numberOfWritesPerTransition = new HashMap<>();
	@Default private double fitnessThreshold = 0.33;
	@Default private double instancesPerLeaf = .15;
	@Default @Getter private Map<String,String> converetedNames = new HashMap<String,String>();
	@Default @Getter private Map<Transition,Transition> transMap = null;
	
	public InvestigationTask setup() {
//		setup pool for later use
		this.pool = new ThreadPoolExecutor(maxConcurrentThreads, maxConcurrentThreads, 60,
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		pool.allowCoreThreadTimeOut(false);

		for (Transition trans : this.source.getModel().getTransitions()) {
			/*
			 * For each transition, set the numberOfExecutions to 0 and
			 * initialize the HashMap<String, Integer>
			 */
			this.numberOfExecutions.put(trans, 0);
			this.numberOfWritesPerTransition.put(trans, AtomicLongMap.<String>create());
		}
//		Need to create class types and literalvalues for selected variables
		Map<String, Type> classTypes = new HashMap<String,Type>();
		for (Entry<String, Type> val : this.source.makeClassTypes().entrySet()) {
			classTypes.put(DecisionMining.fixVarName(val.getKey()), val.getValue());
			this.converetedNames.put(val.getKey(), DecisionMining.fixVarName(val.getKey()));
		}
		Map<String, Set<String>> literalValues = new HashMap<String,Set<String>>();
				
		for( Entry<String, Set<String>> val : this.source.makeLiteralValues().entrySet() ) {
			literalValues.put(DecisionMining.fixVarName(val.getKey()), val.getValue());
		}
		
		
//		### Pirate Code from DecisionMining:393 ###
		/*
		 * For each place with at least 2 outgoing edges in the net ..
		 */
		PetrinetGraph net = this.source.getModel();
		for (Place place : net.getPlaces()) {
			// If 'place' does not represent an OR-split (outgoing edges < 2), goto next iteration of for-loop (skip the place, not interesting)
			if (net.getOutEdges(place).size() < 2) {
				continue;
			}
			/*
			 * 'place' is part of an OR-split decision point with >= 2 outgoing
			 * edges. Prepare an array outputValues[] to collect the target
			 * Transitions of the outgoing edges of 'place'
			 */
			Transition outputValues[] = new Transition[net.getOutEdges(place).size()];
			int index = 0;
			/*
			 * For each outgoing edge 'arc' of Place 'place', store that edge's
			 * target Transition in the array.
			 */
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> arc : net.getOutEdges(place)) {
				outputValues[index++] = (Transition) arc.getTarget();
			}
			/*
			 * Prepare variable f for a functionEstimator, and create one with
			 * the attribute types, values and resulting Transitions for Place
			 * place
			 */
			FunctionEstimator f;

			f = new OverlappingEstimatorLocalDecisionTree(classTypes, literalValues, outputValues, this.source.getLog().size(), place.getLabel());

			/*
			 * Associate the created FunctionEstimator 'f' with Place 'place' by
			 * putting them in a Mapping from <Place> to <FunctionEstimator>
			 */
			this.estimators.put(place, f);
		}
		
		return this;
	}
	
	@Override
	protected DiscoveredPetriNetWithData doInBackground() throws Exception {
		try {
//		setup progress size
		this.source.getProgress().setMaximum(100);
		this.source.getProgress().setValue(0);
		dummyProgress progress = new dummyProgress(this, this.source.getLog().size() + this.estimators.entrySet().size());
//		create some futures
		List<Future<Integer>> traceFutures = new ArrayList<>();
		// Prepare trace counters
		int traceSkipped = 0;
		int totalNumTraces = 0;
		// For each alignment in the PNRepResult 'input'
		for (SyncReplayResult alignment : this.source.getAlignment()) {
			// Count the number of traces encountered in total
			totalNumTraces += alignment.getTraceIndex().size();
			// If the alignment's fitness complies with the specified minimum fitness in fitnessThresholdSlider
			if (alignment.getInfo().get(PNRepResult.TRACEFITNESS).floatValue() >= this.fitnessThreshold) {
				// Then for each trace in the alignment
				for (Integer index : alignment.getTraceIndex()) {
					/*
					 * Have a thread in the pool process the trace in the
					 * alignment, adding instances to the estimator and keeping
					 * track of attribute values. For each alignment, For each
					 * step in the alignment, For each in-edge of the
					 * transition, If the source-Place of that in-edge has an
					 * estimator, Add an instance corresponding to the variable
					 * values in that place, before executing the transition
					 * corresponding to the step. An instance is a set of
					 * attribute values' pre-values and a transition to be
					 * executed
					 */
					traceFutures.add(this.pool.submit(new TraceProcessor(this.source.getModel(), this.source.getLog().get(index), this.estimators, alignment,
							this.numberOfExecutions, this.numberOfWritesPerTransition, progress), index));
				}
			} else {
				// The alignment's fitness is lower than the fitness threshold; skip the trace and count the skipped traces
				traceSkipped += alignment.getTraceIndex().size();
			}
		}
		
		for (Future<Integer> traceFuture : traceFutures) {
			// This blocks until the trace processor is done
			traceFuture.get();
		}
		System.out.println("[ExogenousInvestigationTask] Completed trace futures...");
		System.out.println("[ExogenousInvestigationTask] skipped traces : " + traceSkipped);
		
		
		DiscoveredPetriNetWithData discoveredDPN = null;
		Map<Place, Future<DecisionPointResult>> results = new HashMap<>();

		// For each place
		for (Place place : this.source.getModel().getPlaces()) {
			// Configure the corresponding function estimator's decision tree parameters
			FunctionEstimator f = estimators.get(place);
			if (f != null) {
				if (f instanceof DecisionTreeBasedFunctionEstimator) {
					int numInstances = ((DecisionTreeBasedFunctionEstimator) f).getNumInstances();
					// Set the minimal number of instances per leaf, in per mille (relative to numInstances)
					((DecisionTreeBasedFunctionEstimator) f)
							.setMinNumObj((int) (numInstances * this.instancesPerLeaf ));
					// Prune the tree?
					((DecisionTreeBasedFunctionEstimator) f).setUnpruned(false);
//					// Binary split?
					((DecisionTreeBasedFunctionEstimator) f).setBinarySplit(false);
					((DecisionTreeBasedFunctionEstimator) f).setCrossValidate(true);
					System.out.println("[ExogenousInvestigationTask] Setting estimator ("+place.getLabel()+") : "+ f.getNumInstances() +" :min: " +(int) (numInstances * this.instancesPerLeaf ) );
				}
			}
		}
		
		System.out.println("[ExogenousInvestigationTask] Configured estimators...");
		
		/*
		 * Collect result for each decision point
		 */
		for (Entry<Place, FunctionEstimator> estimatorPlacePair : estimators.entrySet()) {

			final Place place = estimatorPlacePair.getKey();
			final FunctionEstimator f = estimatorPlacePair.getValue();

			Future<DecisionPointResult> result = pool.submit(new Callable<DecisionPointResult>() {

				public DecisionPointResult call() throws Exception {

					// Calculate the conditions with likelihoods for each target transition of place entry2.getKey()
					final Map<Object, FunctionEstimation> estimationTransitionExpression = f
							.getFunctionEstimation(null);
					
					double sumFMeasures = 0;
					int numRules = 0;
					for (FunctionEstimation val : estimationTransitionExpression.values()) {
						sumFMeasures += val.getQualityMeasure();
						numRules++;
					}

					final double singleFScore = numRules > 0 ? sumFMeasures / numRules : 0;
					final String decisionPointClassifier = f.toString();

					DecisionPointResult result = new DecisionPointResult() {

						public String toString() {
							return decisionPointClassifier;
						}

						public double getQualityMeasure() {
							return singleFScore;
						}

						public Map<Object, FunctionEstimation> getEstimatedGuards() {

							return estimationTransitionExpression;
						}

					};

					System.out.println(String.format("[ExogenousInvestigationTask] Generated the conditions for decision point %s with f-score %s",
								place.getLabel(), result.getQualityMeasure()));
					progress.inc();
					return result;
				}

			});
			results.put(place, result);
		}
		System.out.println("[ExogenousInvestigationTask] Completed training estimators...");
		
		/*
		 * Prepare the mining algorithm's resulting PetriNetWithData.
		 */
		String dpnName = String.format("%s (%s, min instances per leaf: %.3f, pruning: %s, binary: %s)", this.source.getModel().getLabel(),
				"Overlapping Decision Tree", this.instancesPerLeaf, false,
				false);
		final PetriNetWithDataFactory factory = new PetriNetWithDataFactory(this.source.getModel(),
				new DiscoveredPetriNetWithData(dpnName), false);
		discoveredDPN = (DiscoveredPetriNetWithData) factory.getRetValue(); // cast if safe
		
		/*
		 * For each entry in classTypes, <String, Type> representing (Attribute
		 * name, attribute type), depending on the type add a new Variable to
		 * the new PetriNetWithData without min or max values
		 */

		for (Entry<String, Type> entry : this.source.makeClassTypes().entrySet()) {
			Class<?> classType = null;
			switch (entry.getValue()) {
				case BOOLEAN :
					classType = Boolean.class;
					break;
				case CONTINUOS :
					classType = Double.class;
					break;
				case DISCRETE :
					classType = Long.class;
					break;
				case LITERAL :
					classType = String.class;
					break;
				case TIMESTAMP :
					classType = Date.class;
					break;
				default :
					break;

			}
			String wekaUnescaped = DecisionMining.wekaUnescape(entry.getKey());
			String saneVariableName = GuardExpression.Factory.transformToVariableIdentifier(wekaUnescaped);
			discoveredDPN.addVariable(saneVariableName, classType, null, null);
		}
		
		//TODO FM, we should not change the default Locale!! What about concurrent operations that rely on the correct Locale? Why is it done anyway?
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.US);

		double sumFScores = 0;
		int numEstimators = 0;

		Map<Place, Place> placeMapping = factory.getPlaceMapping();
		this.transMap = factory.getTransMapping();

		for (Entry<Place, Future<DecisionPointResult>> futureEntry : results.entrySet()) {
			try {
				DecisionPointResult result = futureEntry.getValue().get();
				discoveredDPN.storeDecisionPointResult(placeMapping.get(futureEntry.getKey()), result);

				// If, for any Transition at this decision point, an expression is found..
				if (!result.getEstimatedGuards().isEmpty()) {
					// Then, for each such Transition, set the guard in the PetriNetWithData
					for (Entry<Object, FunctionEstimation> transitionEntry : result.getEstimatedGuards().entrySet()) {
						Transition transitionInPNWithoutData = (Transition) transitionEntry.getKey();
						PNWDTransition transitionInPNWithData = (PNWDTransition) factory.getTransMapping()
								.get(transitionInPNWithoutData);
						FunctionEstimation value = transitionEntry.getValue();
						if (transitionInPNWithData.getGuardExpression() != null) {
							//TODO find correct method to update f-score / what to do in the general case for more than two incoming arcs
							Double combinedFScore = (value.getQualityMeasure() + transitionInPNWithData.getQuality())
									/ 2;
							GuardExpression existingGuard = transitionInPNWithData.getGuardExpression();
							GuardExpression additionalGuard = value.getExpression();
							GuardExpression combinedGuard = GuardExpression.Operation.and(existingGuard,
									additionalGuard);
//							System.out.println(
//									String.format("[ExogenousInvestigationTask] Combining two guards for non-free choice construct: %s (%s) %s (%s)",
//											existingGuard, value.getQualityMeasure(), additionalGuard,
//											transitionInPNWithData.getQuality()));
							discoveredDPN.setGuard(transitionInPNWithData, combinedGuard, combinedFScore);
						} else {
//							System.out.println("[ExogenousInvestigationTask] found guard expression : "+ value.getExpression());
							
							discoveredDPN.setGuard(transitionInPNWithData, value.getExpression(),
									value.getQualityMeasure());
						}
					}

					sumFScores += result.getQualityMeasure();
					numEstimators++;
				} else {
//					System.out.println("[ExogenousInvestigationTask] hmmm :" +result.toString());
				}
			} catch (ExecutionException e) {
				System.out.println("[ExogenousInvestigationTask] error occured :" +e);
			}
		}

		//TODO see above!
		Locale.setDefault(defaultLocale);
		
		if (sumFScores > 0) {
			System.out.println("[ExogenousInvestigationTask] Average F Score: " + sumFScores / numEstimators);
		}
		discoveredDPN.removeAllVariablesNotInGuard();
		pool.shutdown();
		return discoveredDPN;
		
		
		} catch (Exception e) {
			System.out.println("[ExogenousInvestigationTask] Error occured in work : "+ e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	public class dummyProgress implements Progress {
		
		private InvestigationTask source;
		private double value;
		private int max;
		
		public dummyProgress(InvestigationTask source, int max) {
			this.source = source;
			this.value = 0.0;
			this.max = max;
		}

		@Override
		public void setMinimum(int value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setMaximum(int value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setValue(int value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setCaption(String message) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getCaption() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getValue() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void inc() {
			// TODO Auto-generated method stub
			this.source.setProgress((int) ((this.value++)/this.max));
		}

		@Override
		public void setIndeterminate(boolean makeIndeterminate) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isIndeterminate() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int getMinimum() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getMaximum() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean isCancelled() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void cancel() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
}