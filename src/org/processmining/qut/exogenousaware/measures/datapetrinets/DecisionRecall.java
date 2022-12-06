package org.processmining.qut.exogenousaware.measures.datapetrinets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
public class DecisionRecall implements PetriNetMeasure {
	
	@NonNull Integer progressInc;
	@NonNull private ExogenousDiscoveryProgresser progresser;
	@NonNull Map<String,String> variableMapping;


	public static String NAME = "decision-recall";
	
	/**
	 * Computes decision-recall for the given log, model and alignment, <br>
	 * whereby, the measure returns 1.0, when for each observation, we could have
	 *  evaluated the described guard of a choice, and <br>
	 * otherwise returns 0.0 when for each observation, we couldn't evaluate the
	 *  described guard or the guard was a truth.
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
		
		double totalrfsum = 0.0;
//		compute total measure but store local measures in statistics
		for( Place dplace : statisticResult.getDecisionMoments()) {
			double decisionfreq = statisticResult.getInformation(dplace).getRelativeFrequency();
			double localmeasure = 0.0;
			double localrfsum = 0.0;
			List<Observation> obs = getObservations(log, alignmentResult, dplace, statisticResult);
			for(Transition outcome : statisticResult.getDecisionOutcomes(dplace)) {
				double outcomemeasure = 0.0;
				double outcomefreq = 0.0;
				if (statisticResult.getInformation(dplace).getMapToFrequency().containsKey(outcome)) {
					outcomefreq = statisticResult.getInformation(dplace).getMapToFrequency().get(outcome);
				}
				final double freq = outcomefreq * decisionfreq;
				List<Observation> outcomeObs = obs.stream().filter(o -> o.getOutcome().equals(outcome)).collect(Collectors.toList());
				if (outcome instanceof PNWDTransition) {
					PNWDTransition trans = (PNWDTransition) outcome;
					Optional<Double> totalOutcomeRF = null;
					if (trans.hasGuardExpression()) {
						GuardExpression  expr = trans.getGuardExpression();
						totalOutcomeRF = outcomeObs.stream()
								.filter(o -> !isExpressionSimpleTruth(expr))
								.filter(o -> couldEvaluateExpression(o, expr))
								.map( o -> o.likelihood * freq)
								.reduce(Double::sum);
						double measurerf = 0.0;
						if (totalOutcomeRF.isPresent()) {
							measurerf = totalOutcomeRF.get();
						}
						outcomemeasure += measurerf;
						double guardwise = (measurerf/(freq * outcomeObs.size())) ;
						statisticResult.getInformation(dplace).addMeasure(outcome.getId().toString()+"-recall", guardwise);
					}
					localrfsum += freq * outcomeObs.size();
				}
//				add to decision point's measure
				localmeasure += outcomemeasure;
				state.increment(progressInc);
				System.out.println("[DecisionRecall] outcome ("+outcome.getLabel()+") decision-recall for "+ outcome.getLabel().toLowerCase() + " was :: "+ outcomemeasure+ "/"+ freq * outcomeObs.size());
			}
			statisticResult.addMeasureToDecisionMoment(dplace, NAME, (localmeasure/localrfsum));
			totalrfsum += localrfsum;
			System.out.println("[DecisionRecall] local decision-recall for "+ dplace.getLabel().toLowerCase() + " was :: "+localmeasure+"/"+localrfsum);
			measure += localmeasure;
		}
		System.out.println("[DecisionRecall] computed decision-recall was :: "+ measure+"/"+totalrfsum);
		return (measure/totalrfsum);
		
		
	}
	
	/**
	 * Tests the given expression, to see its a (simple) truth or not.
	 * @param obs
	 * @param expr
	 * @return the result of the test
	 */
	private Boolean isExpressionSimpleTruth(GuardExpression expr) {
		boolean test = true;
		
		if (expr == null) {
			return true;
		}
		test = expr.isTrue();
		return test;
	}
	
	/**
	 * Tests that for a observation we have a datastate with variables within expr, thus proving that they are related.
	 * @param obs
	 * @param expr
	 * @return do we have all variables for expr
	 */
	private Boolean couldEvaluateExpression(Observation obs, GuardExpression expr) {
		boolean test = true;
		try {
			for (String var : expr.getNormalVariables()) {
				test = test & obs.getDatastate().containsKey(var);
			}
			return test;
		} catch (Exception e) {
			
			if (e.getCause() instanceof VariableNotFoundException) {
				
			} else {
				System.out.println("[DecisionRecall] checking :: "+ expr.toCanonicalString());
				System.out.println("[DecisionRecall] for :: "+obs.getDatastate().toString());
				System.out.println("[DecisionRecall] failed to compute expr :: " + e.getMessage());
				e.printStackTrace();
			}
			return false;
		}
	}
	
	private List<Observation> getObservations(XLog log, PNRepResult alignments, Place decisionPoint, ProcessModelStatistics stats){
		List<Observation> obs = new ArrayList();
		
		for(SyncReplayResult alignment  : alignments) {
//			walk alignment and count when needed
			
			int partialTracePoint = -1;
			double missteps = 0;
			double steps = 0;
			for(int i=0; i < alignment.getNodeInstance().size(); i++) {
				
				StepTypes step = alignment.getStepTypes().get(i);
				
				if (step == StepTypes.L || step == StepTypes.LMGOOD) {
					partialTracePoint++;
				}
				
				if (step != StepTypes.LMGOOD && step != StepTypes.MINVI) {
					missteps += 1;
				}
				steps += 1;
				
//				(1) search for decision outcome node, such that it was correctly aligned or routing
				Object node = alignment.getNodeInstance().get(i);
				if (stats.getDecisionOutcomes(decisionPoint).contains(node)) {
					if ( step == StepTypes.LMGOOD || alignment.getStepTypes().get(i) == StepTypes.MINVI) {
//						(2) if it is a decision node outcome, compute observation data state
						for(int traceIndex : alignment.getTraceIndex()) {
							
							List<XEvent> partial = new ArrayList();
							if (partialTracePoint > -1) {
								partial = log.get(traceIndex).subList(0, partialTracePoint+1);
							}
//							System.out.println("[DecisionRecall] Computed likelihood is :: "+(1.0f - (missteps/steps)));
							obs.add(Observation.builder()
								.outcome((Transition) node)
								.partial(partial)
								.likelihood(1.0f - (missteps/steps))
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
		@NonNull @Default @Getter double likelihood = 0.0f;
		
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
				postEvent = partial.get(partial.size()-1);
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
			
//			System.out.println("Final pre-transition state :: "+datastate.toString());
			
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
				realValue = value.toString();
			}
//			System.out.println("Addding :: ("+key+","+realValue+")");
			mapper.put(key,realValue);
		}
		
	}
	
	

}
