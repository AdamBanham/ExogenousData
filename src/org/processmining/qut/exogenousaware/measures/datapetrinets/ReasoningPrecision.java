package org.processmining.qut.exogenousaware.measures.datapetrinets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.processmining.datapetrinets.exception.VariableNotFoundException;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PNWDTransition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithData;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousDiscoveryProgresser;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousDiscoveryProgresser.ProgressState;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousDiscoveryProgresser.ProgressType;
import org.processmining.qut.exogenousaware.measures.PetriNetMeasure;
import org.processmining.qut.exogenousaware.stats.models.ModelStatistics;
import org.processmining.qut.exogenousaware.stats.models.ProcessModelStatistics;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class ReasoningPrecision implements PetriNetMeasure {

	@NonNull Integer progressInc;
	@NonNull private ExogenousDiscoveryProgresser progresser;
	@NonNull Map<String,String> variableMapping;
	
	public static String NAME = "reasoning-precision";
	
	/**
	 * Computes reasoning-precision for the given log, model and alignment, whereby <br>
	 * a score of 0.0 denotes that no transition guard is true, and <br>
	 * a score of 1.0 denotes that only the transition guard for the true outcome is enabled,
	 * for all observations of all decision points.
	 */
	public double measure(XLog log, Object model, ModelStatistics statistics, Object alignment) {
		ProgressState state = progresser.getState(ProgressType.Measurements);
		
		double measure = 0.0;
//		check for the right type of alignment result
		PNRepResult alignmentResult;
		if (alignment instanceof PNRepResult) {
			alignmentResult = (PNRepResult) alignment;
		} else {
			throw new IllegalArgumentException("Unknown alignment result of :: "+ alignment.getClass().toGenericString());
		}
		
//		check for the right type of modelstatistics
		ProcessModelStatistics statisticResult;
		if (statistics instanceof ProcessModelStatistics) {
			statisticResult = (ProcessModelStatistics) statistics;
		} else {
			throw new IllegalArgumentException("Unknown statistics result of :: "+ statistics.getClass().toGenericString());
		}
		
//		check for the right type of model 
		PetriNetWithData modeller;
		if (model instanceof PetriNetWithData) {
			modeller = (PetriNetWithData) model;
		}
		
		
//		compute total measure but store local measures in statistics
		for( Place dplace : statisticResult.getDecisionMoments()) {
			double decisionfreq = statisticResult.getInformation(dplace).getRelativeFrequency();
			double localmeasure = 0.0;
			List<Observation> obs = getObservations(log, alignmentResult, dplace, statisticResult);
			for(Transition outcome : statisticResult.getDecisionOutcomes(dplace)) {
				double outcomemeasure = 0.0;
				double trueOutcomeTruths = 0.0;
				double untrueOutcomeTruths = 0.0;
				double outcomefreq = 0.0;
				
				if (statisticResult.getInformation(dplace).getMapToFrequency().containsKey(outcome)) {
					outcomefreq = statisticResult.getInformation(dplace).getMapToFrequency().get(outcome);
				}
				
				List<Observation> outcomeObs = obs.stream().filter(o -> o.getOutcome().equals(outcome)).collect(Collectors.toList());
				
				if (outcome instanceof PNWDTransition) {
					PNWDTransition trans = (PNWDTransition) outcome;
					GuardExpression  expr = trans.getGuardExpression();
					List<Observation> possibleEvals = outcomeObs.stream()
							.filter(o -> isExpressionTrue(o, expr))
							.collect(Collectors.toList());
					trueOutcomeTruths = possibleEvals.size();
				}
				
				for(Transition otherOutcome : statisticResult.getDecisionOutcomes(dplace)) {
					
					if (otherOutcome.equals(outcome)) {
						continue;
					}
					
					PNWDTransition trans = (PNWDTransition) otherOutcome;
					GuardExpression  expr = trans.getGuardExpression();
					List<Observation> possibleEvals = outcomeObs.stream()
							.filter(o -> isExpressionTrue(o, expr))
							.collect(Collectors.toList());
					
					untrueOutcomeTruths += possibleEvals.size();
				}
				
				
				if (outcomeObs.size() > 0) {
//					System.out.println(String.format(
//							"[ReasoningPrecision] Calculating Precision = %.3f * (%.0f/(%d + %.0f)) "
//							, outcomefreq, trueOutcomeTruths, outcomeObs.size(), untrueOutcomeTruths));
					outcomemeasure = trueOutcomeTruths / (outcomeObs.size() + untrueOutcomeTruths);
					outcomemeasure = outcomefreq * outcomemeasure;
					localmeasure += outcomemeasure;
				}
				state.increment(progressInc);
				
//				System.out.println("[ReasoningPrecision] outcome reasoning precision for "+ outcome.getLabel().toLowerCase() + " was :: "+ outcomemeasure);
			}
			localmeasure = decisionfreq * localmeasure;
			statisticResult.addMeasureToDecisionMoment(dplace, NAME , localmeasure);
			
			System.out.println("[ReasoningPrecision] local reasoning precision for "+ dplace.getLabel().toLowerCase() + " was :: "+ localmeasure);
			measure += localmeasure;
		}
		System.out.println("[ReasoningPrecision] computed reasoning precision was :: "+ measure);
		
		
		return measure;
	}
	
	/**
	 * Tests that for a observation we have a datastate with variables within expr, thus proving that they are related.
	 * @param obs
	 * @param expr
	 * @return do we have all variables for expr
	 */
	private Boolean couldEvaluateExpression(Observation obs, GuardExpression expr) {
		
		boolean test = true;
		
		if (expr == null) {
			return true;
		}
		
		try {
			for (String var : expr.getNormalVariables()) {
				test = test & obs.getDatastate().containsKey(var);
			}
			return test;
		} catch (Exception e) {
			
			if (e.getCause() instanceof VariableNotFoundException) {
				
			} else {
				System.out.println("[ReasoningPrecision] checking :: "+ expr.toCanonicalString());
				System.out.println("[ReasoningPrecision] for :: "+obs.getDatastate().toString());
				System.out.println("[ReasoningPrecision] failed to compute expr :: " + e.getMessage());
				e.printStackTrace();
			}
			return false;
		}
	}
	
	/**
	 * Tests the given expression, to see if for the given observation it would evaluate to true.
	 * @param obs
	 * @param expr
	 * @return the result of the test
	 */
	private Boolean isExpressionTrue(Observation obs, GuardExpression expr) {
		boolean test = true;
		
		if (expr == null) {
			return true;
		}
		
		if (expr.isTrue()) {
			return test;
		} else if (expr.isFalse()) {
			return false;
		}
		
		if (couldEvaluateExpression(obs, expr)) {
			
			test = expr.isTrue(obs.getDatastate());
			
		} else {
			return false;
		}
		
		return test;
	}
	
	private List<Observation> getObservations(XLog log, PNRepResult alignments, Place decisionPoint, ProcessModelStatistics stats){
		List<Observation> obs = new ArrayList();
		
		for(SyncReplayResult alignment  : alignments) {
//			walk alignment and count when needed
			
			int partialTracePoint = -1;
			for(int i=0; i < alignment.getNodeInstance().size(); i++) {
				
				StepTypes step = alignment.getStepTypes().get(i);
				
				if (step == StepTypes.L || step == StepTypes.LMGOOD) {
					partialTracePoint++;
				}
				
//				(1) search for decision outcome node, such that it was correctly aligned or routing
				Object node = alignment.getNodeInstance().get(i);
				if (stats.getDecisionOutcomes(decisionPoint).contains(node)) {
					if ( step == StepTypes.LMGOOD || alignment.getStepTypes().get(i) == StepTypes.MINVI) {
//						(2) if it is a decision node outcome, compute observation data state
						for(int traceIndex : alignment.getTraceIndex()) {
							
							List<XEvent> partial = new ArrayList();
							if (partialTracePoint > -1) {
								partial = log.get(traceIndex).subList(0, partialTracePoint);
							}
							
							obs.add(Observation.builder()
								.outcome((Transition) node)
								.partial(partial)
								.variableMapping(variableMapping)
								.build()
								.setup()
								);
						}
					
					}
				}
				
			}
		}
		
		return obs;
	}
	
	
	@Builder
	public static class Observation {
		
		@NonNull @Getter Transition outcome;
		@NonNull List<XEvent> partial;
		@NonNull Map<String,String> variableMapping;
		
		@Default @Getter Map<String, Object> datastate = new HashMap();
		@Default @Getter Map<String, Object> postDatastate = new HashMap();
		
		public Observation setup() {
//			build data state
			
			List<XEvent> preEvents = new ArrayList();
			if (partial.size() > 1) {
				preEvents = partial.subList(0, partial.size()-1);
			}
			XEvent postEvent = null;
			if (partial.size() > 0) {
				postEvent = partial.get(partial.size() -1);
			}
			
			for( XEvent ev: preEvents) {
				for( Entry<String, XAttribute> attr : ev.getAttributes().entrySet()) {
					String key;
					
					if (variableMapping.containsKey(attr.getKey())) {
						key = variableMapping.get(attr.getKey());
					} else {
						key = attr.getKey();
					}
					
					processValue(key, datastate, attr.getValue());
				}
			}
			
//			create post state
			postDatastate.putAll(datastate);
			if (postEvent != null) {
				for ( Entry<String, XAttribute> attr: postEvent.getAttributes().entrySet()) {
					String key;
					
					if (variableMapping.containsKey(attr.getKey())) {
						key = variableMapping.get(attr.getKey());
					} else {
						key = attr.getKey();
					}
					
					processValue(key, postDatastate, attr.getValue());
				}
			}
			
			return this;
		}
		
		private void processValue(String key, Map<String,Object> mapper, Object value) {
			Object realValue = null;
			
			if (value instanceof XAttributeContinuous) {
				realValue = ((XAttributeContinuous) value).getValue();
			} else if (value instanceof XAttributeBoolean) {
				realValue = ((XAttributeBoolean) value).getValue();
			} else if (value instanceof XAttributeDiscrete) {
				realValue = ((XAttributeDiscrete) value).getValue();
			} else {
				try {
					realValue = Double.valueOf(value.toString());
				} catch (Exception e) {
					
//					System.out.println("process failback occured on :: " + value.getClass().toGenericString());
					realValue = value.toString();
				}
				
				
				
			}
			
			mapper.put(key,realValue);
		}
		
	}

}
