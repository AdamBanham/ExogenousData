package org.processmining.qut.exogenousaware.gui.workers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingWorker;

import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithData;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.qut.exogenousaware.data.ExogenousAnnotatedLog;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousDiscoveryProgresser;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousDiscoveryProgresser.ProgressState;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousDiscoveryProgresser.ProgressType;
import org.processmining.qut.exogenousaware.stats.models.ProcessModelStatistics;
import org.processmining.qut.exogenousaware.stats.models.ProcessModelStatistics.DecisionPoint;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;
import lombok.Setter;

@Builder
public class ExogenousDiscoveryStatisticWorker extends SwingWorker<ProcessModelStatistics, Integer> {

//	builder parameters 
	@NonNull private PNRepResult alignment;
	@NonNull private ExogenousDiscoveryProgresser progresser;
	@NonNull private PetriNetWithData controlflow;
	@NonNull private ExogenousAnnotatedLog log;
	
	
	
	
//	internal variables
	@Default @Setter boolean cancelled = false;
	private ProgressState state;
	@Default private Map<Transition, Integer> seenObserverations = new HashMap();
	@Default private List<Place> decisionPlaces = new ArrayList();
	@Default private List<DecisionPoint> decisionPoints = new ArrayList();
	
	
	public ExogenousDiscoveryStatisticWorker setup() {
		state = progresser.getState(ProgressType.Stats);
		state.setProgress(0);
		
//		add all transitions before counting
		for(Transition trans : controlflow.getTransitions()) {
				seenObserverations.put(trans, 0);
		}
		
//		work out the number of decision points (for petri nets)
		for( Place place: controlflow.getPlaces()) {
			
			if (controlflow.getOutEdges(place).size() > 1) {
				
				decisionPlaces.add(place);
			}
		}
		
		System.out.println("[ExogenousDiscoveryStatisticWorker] No. of Decision Points :: " + decisionPlaces.size());
		
		state.setTotal(alignment.size() + (50f * decisionPlaces.size()) );
		return this;
	}
	
	protected ProcessModelStatistics doInBackground() throws Exception {

		
//		handle each replay result to extract the following:
//		(1) number of times a transition was seen
//		(2) decision points
//			(2.1) relative frequency of a point
//			(2.2) relative frequency of outcomes at a point
		
		
//		(1) using sync or invisible moves count frequency of transitions
//		Remember that each result may mapping to one or more traces
		for(SyncReplayResult sync: alignment) {
//			get the number of variant instances
			int mut = sync.getTraceIndex().size();
//			walk alignment and count when needed
			for(int i=0; i < sync.getNodeInstance().size(); i++) {
				
				if (sync.getStepTypes().get(i) == StepTypes.LMGOOD || sync.getStepTypes().get(i) == StepTypes.MINVI) {
					Object node = sync.getNodeInstance().get(i);
					
					if ( controlflow.getTransitions().contains(node) ) {
						seenObserverations.put((Transition) node, seenObserverations.get(node)+ mut);
					}
				}
				
			}
			
			state.increment();
			
			if (isCancelled()) {
				return null;
			}
		}
		
//		find local relative frequency statistics 
		int totalDecisionInstances = 0;
		for(Place dplace : decisionPlaces) {
			if (isCancelled()) {
				return null;
			}
			
			
//			swingworkers error silently by design
			try {
				
				
			DecisionPoint dp = DecisionPoint.builder()
					.decisionPlace(dplace)
					.build();
//			find output transitions
			List<Transition> outcomes = new ArrayList();
			for( PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> arcs : controlflow.getOutEdges(dplace)) {
				if (arcs.getTarget() instanceof Transition) {
					outcomes.add((Transition) arcs.getTarget() );
				}
			}
//			add outcome to decision point
			for(Transition trans : outcomes) {
				int obs = 0;
				if (seenObserverations.containsKey(trans)) {
					obs = seenObserverations.get(trans);
				}
				dp.addOutcome(trans, obs);
			}
			
			decisionPoints.add(dp);
			state.increment(40);
			totalDecisionInstances += dp.getTotalInstances();
			
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (isCancelled()) {
				return null;
			}
		}
		
//		find global frequencies
		float total = 0.0f;
		for(DecisionPoint dp: decisionPoints) {
			dp.setRelativeFrequency( dp.getTotalInstances() /(totalDecisionInstances * 1.0f));
			state.increment(10);
			System.out.println("[ExogenousDiscoveryStatisticWorker] "+ dp.toString());
			total += dp.getRelativeFrequency();
		}
		
//		prep result
		ProcessModelStatistics result = ProcessModelStatistics.builder()
				.decisionPoints(decisionPoints)
				.observations(seenObserverations)
				.build()
				.setup();
		
		
		
		return result;
	}

	
	protected void done() {
		super.done();
//		state.increment(alignment.size());
	}
	
	
	protected void process(List<Integer> chunks) {
		super.process(chunks);
//		state.increment(chunks.size());
	}
	
	
	
}
