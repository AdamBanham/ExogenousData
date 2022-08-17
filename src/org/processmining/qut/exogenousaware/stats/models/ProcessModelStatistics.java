package org.processmining.qut.exogenousaware.stats.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.qut.exogenousaware.gui.workers.ExogenousDiscoveryStatisticWorker.DecisionPoint;

import lombok.Builder;
import lombok.NonNull;

@Builder
public class ProcessModelStatistics implements ModelStatistics<Place,Transition,DecisionPoint> {
//	builder parameters
	@NonNull List<DecisionPoint> decisionPoints;
	@NonNull Map<Transition, Integer> observations;
	
	
//	Internal states
	private List<Place> decisionMoments;
	private Map<Place, List<Transition>> decisionOutcomes;
	private Map<Place, DecisionPoint> decisionInformation;
	
	public ProcessModelStatistics setup() {
		decisionMoments = new ArrayList();
		decisionOutcomes = new HashMap();
		decisionInformation = new HashMap();
		
//		collect decision moments and outcomes
		for(DecisionPoint dp : decisionPoints) {
			decisionMoments.add(dp.getDecisionPlace());
			decisionInformation.put(dp.getDecisionPlace(), dp);
			decisionOutcomes.put(dp.getDecisionPlace(), dp.getOutcomes());
		}
		return this;
	}
	
	
	public List<Place> getDecisionMoments() {
		return Collections.unmodifiableList(decisionMoments);
	}

	public List<Transition> getDecisionOutcomes(Place moment) {
		List<Transition> out = null;
		
		if (decisionOutcomes.containsKey(moment)) {
			out = Collections.unmodifiableList(decisionOutcomes.get(moment));
		}
		
		return out;
	}

	public Boolean isDecisionMoment(Place moment) {
		return decisionMoments.contains(moment);
	}

	public Boolean isOutcome(Transition outcome, Place moment) {
		boolean out = false;
		
		if (decisionMoments.contains(moment)) {
			if (decisionOutcomes.get(moment).contains(outcome)) {
				out = true;
			}
		}

		return out;
	}

	public DecisionPoint getInformation(Place moment) {
		DecisionPoint out = null;
		
		if (decisionMoments.contains(moment)) {
			out = decisionInformation.get(moment);
		}
		
		return out;
	}


	public Integer getObservations(Transition action) {
		int out = 0;
		
		if (observations.containsKey(action)) {
			out = observations.get(action);
		}
		
		return out;
	}
	
	
	

	
}
