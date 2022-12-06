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
public class DecisionPrecision implements PetriNetMeasure {

	@NonNull Integer progressInc;
	@NonNull private ExogenousDiscoveryProgresser progresser;
	@NonNull Map<String,String> variableMapping;
	
	public static String NAME = "decision-precision";
	
	/**
	 * Computes decision-precision for the given log, model and alignment, whereby <br>
	 * this measure returns 1.0, when for each observation, only the described guard
	 *  was supported by the observed data, and <br>
	 * otherwise the measure returns 0.0, when for each observation, the described
	 *  guard does not support the observed data.
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
		} else {
			throw new IllegalArgumentException("Unsupported type of model :: "+ model.getClass().getSimpleName());
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
				double trueOutcomeTruths = 0.0;
				double untrueOutcomeTruths = 0.0;
				double outcomefreq = 0.0;
				
				if (statisticResult.getInformation(dplace).getMapToFrequency().containsKey(outcome)) {
					outcomefreq = statisticResult.getInformation(dplace).getMapToFrequency().get(outcome);
				}
				
				List<Observation> outcomeObs = obs.stream().filter(o -> o.getOutcome().equals(outcome)).collect(Collectors.toList());
				final double freq = outcomefreq * decisionfreq;
				if (outcome instanceof PNWDTransition) {
//					work out how many times this outcome supported observed data
					PNWDTransition trans = (PNWDTransition) outcome;
					GuardExpression  expr = trans.getGuardExpression();
//					compute support
					Optional<Double> possibleEvals = outcomeObs.stream()
							.filter(o -> !isSimpleTruth(expr))
							.filter(o -> isExpressionTrue(o, expr))
							.map( o -> o.likelihood * freq)
							.reduce(Double::sum);
					if (possibleEvals.isPresent()) {
						trueOutcomeTruths = possibleEvals.get();
					}
					System.out.println("[DecisionPrecision] Number of times observed data was supported by "+
							outcome.getLabel()+
							" :: "+ 
							outcomeObs.stream()
								.filter(o -> !isSimpleTruth(expr))
								.filter(o -> isExpressionTrue(o, expr))
								.count()
					);
					localrfsum += freq * outcomeObs.size();
				}
				
//				work out how many times the observed data was supported by other
//				outcomes
				for(Transition otherOutcome : statisticResult.getDecisionOutcomes(dplace)) {
					if (otherOutcome.equals(outcome)) {
						continue;
					}
//					compute likelihood of other outcome
					final double otherFreq = decisionfreq * statisticResult.getInformation(dplace).getMapToFrequency().get(otherOutcome);
					
					if (otherOutcome instanceof PNWDTransition) {
//						work out how often observed data was supported by other
						PNWDTransition trans = (PNWDTransition) otherOutcome;
						GuardExpression  expr = trans.getGuardExpression();
						Optional<Double> possibleEvals = outcomeObs.stream()
								.filter(o -> isExpressionTrue(o, expr))
								.map( o -> o.likelihood * otherFreq)
								.reduce(Double::sum);
						System.out.println("[DecisionPrecision] Number of times observed data was supported by "+
								trans.getLabel()+ " for " + outcome.getLabel() +
								" :: "+ 
								outcomeObs.stream()
									.filter(o -> !isSimpleTruth(expr))
									.filter(o -> isExpressionTrue(o, expr))
									.count()
						);
						if (possibleEvals.isPresent()) {
							untrueOutcomeTruths += possibleEvals.get();
						}
					}
				}
				
				
				if (outcomeObs.size() > 0) {
					outcomemeasure = (trueOutcomeTruths) * (1.0/(1.0+untrueOutcomeTruths));
					localmeasure += outcomemeasure;
				}
				double limit = (freq * outcomeObs.size());
				double guardwise = (1.0/limit) * (outcomemeasure);
				statisticResult.getInformation(dplace).addMeasure(outcome.getId().toString()+"-precision", guardwise);
				state.increment(progressInc);
				System.out.println("[DecisionPrecision] outcome decision-precision for "+ outcome.getLabel() + " was :: "+ outcomemeasure +"/"+limit);
			}
			measure += localmeasure;
			totalrfsum += localrfsum;
			localmeasure = (1.0 / localrfsum) * localmeasure;
			statisticResult.addMeasureToDecisionMoment(dplace, NAME , localmeasure);
			System.out.println("[DecisionPrecision] local decision-precision for "+ dplace.getLabel() + " was :: "+ localmeasure);
		}
		System.out.println("[DecisionPrecision] computed decision-precision was :: "+ measure +"/"+totalrfsum);
		measure = (1.0 / totalrfsum) * measure;
		return measure;
	}
	
	private boolean isSimpleTruth(GuardExpression expr) {
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
				System.out.println("[DecisionPrecision] checking :: "+ expr.toCanonicalString());
				System.out.println("[DecisionPrecision] for :: "+obs.getDatastate().toString());
				System.out.println("[DecisionPrecision] failed to compute expr :: " + e.getMessage());
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
		@NonNull @Default @Getter double likelihood = 0.0;
		
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
