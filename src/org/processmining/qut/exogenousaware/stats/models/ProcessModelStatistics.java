package org.processmining.qut.exogenousaware.stats.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Builder
public class ProcessModelStatistics implements ModelStatistics<Place,Transition,ProcessModelStatistics.DecisionPoint> {
//	builder parameters
	@NonNull List<DecisionPoint> decisionPoints;
	@NonNull Map<Transition, Integer> observations;
	
	
//	Internal states
	private List<Place> decisionMoments;
	private Map<Place, List<Transition>> decisionOutcomes;
	private Map<Place, DecisionPoint> decisionInformation;
	@Getter private Set<String> seenMomentMeasures;
	@Getter private Map<String, Double> graphMeasures;
	@Getter private Set<String> seenGraphMeasures;
	
	public ProcessModelStatistics setup() {
		decisionMoments = new ArrayList();
		decisionOutcomes = new HashMap();
		decisionInformation = new HashMap();
		seenMomentMeasures = new HashSet();
		graphMeasures = new HashMap();
		seenGraphMeasures = new HashSet();
		
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
	
	public DecisionPoint findDecision(Transition outcome) {
		DecisionPoint point = null;
		
		for (Place dpplace : getDecisionMoments()) {
			if (isOutcome(outcome, dpplace)) {
				point = getInformation(dpplace);
				break;
			}
		}
				
		return point;
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
	
	
	
		
	/**
	 * Resets the state of measurements for decision points
	 */
	public void clearMeasures() {
		seenGraphMeasures.clear();
		seenMomentMeasures.clear();
		graphMeasures.clear();
		
		for ( DecisionPoint info:  decisionInformation.values()) {
			info.clearMeasures();
		}
	}
	
	/**
	 * Adds a computed measure for a decision moment.
	 * @param moment
	 * @param key
	 * @param measure
	 */
	public void addMeasureToDecisionMoment(Place moment, String key, Double measure) {
		
		if (!seenMomentMeasures.contains(key)) {
			seenMomentMeasures.add(key);
		}
		
		decisionInformation.get(moment).addMeasure(key, measure);
	}
	
	public void addGraphMeasure(String measureName, double measure) {
		
		if (!seenGraphMeasures.contains(measureName)) {
			seenGraphMeasures.add(measureName);
		}
		
		graphMeasures.put(measureName, measure);
		
	}
	
	
	@Builder
	public static class DecisionPoint {
//		builder parameters
		@NonNull @Getter private Place decisionPlace;
		
//		internal states
		@Getter @Setter private int totalInstances;
		@Getter @Setter private float relativeFrequency;
		@Default @Getter @Setter private List<Transition> outcomes = new ArrayList();
		@Default @Getter private Map<Transition, Integer> mapToObservations= new HashMap();
		@Default @Getter private Map<Transition, Float> mapToFrequency= new HashMap();
		@Default @Getter private Map<String, Double> mapToMeasures= new HashMap(); 
		
		public void addOutcome(Transition trans, int instances) {
			outcomes.add(trans);
			mapToObservations.put(trans, instances);
			computeFrequencies();
		}
		
		private void computeFrequencies() {
			int total = 0;
			for( Transition trans : outcomes) {
				total += mapToObservations.get(trans);
			}
			setTotalInstances(total);
			for( Transition trans : outcomes) {
				float freq = 0.0f;
				if (total > 0.0f) {
					freq = mapToObservations.get(trans) / (total * 1.0f);
				}
				mapToFrequency.put(trans, freq);
			}
			
		}
		
		public void addMeasure(String key, Double measure) {
			mapToMeasures.put(key, measure);
		}
		
		public void clearMeasures() {
			mapToMeasures = new HashMap();
		}
		
		public String toString() {
			return decisionPlace.toString() + " has "+ outcomes.size() + " outcomes ("+totalInstances+String.format("/ %3.1f%%)", (relativeFrequency * 100));
		}
	}

	
}
