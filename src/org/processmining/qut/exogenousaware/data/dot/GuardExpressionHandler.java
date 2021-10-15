package org.processmining.qut.exogenousaware.data.dot;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.deckfour.xes.model.XAttributeContinuous;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;

import lombok.Data;

@Data
public class GuardExpressionHandler {
	
	private GuardExpression guardExpression;
	private Map<String,String> convertedNames;
	private double initalValue = -1.0;
	
	public GuardExpressionHandler(GuardExpression guard, Map<String,String> names) {
		this.guardExpression = guard;
		this.convertedNames = names;
	}
	
	public Boolean hasExpression() {
		return this.guardExpression != null;
	}
	
	public String getRepresentation() {
		if (this.guardExpression != null) {
			String rep = this.guardExpression.toCanonicalString();
			for(Entry<String,String> entry : this.convertedNames.entrySet()) {
				rep = rep.replace(entry.getValue(), entry.getKey());
		}
		return "Guard expression : " + rep;
		} else {
			return "Guard expression always evaluates to true";
		}
	}
	
	public Boolean evaluate(Map<String, Object> state) {
//		do swapping with encoded variables
		Map<String, Object> newState = new HashMap<String,Object>();
		Set<String> expressionVariables = this.guardExpression.getNormalVariables();
		for(Entry<String, Object> entry: state.entrySet()) {
//			get this varaible's name
			String stateVariableName = entry.getKey();
//			check if it need to have a special name instead of original
			if(this.convertedNames.containsKey(stateVariableName)) {
				stateVariableName = this.convertedNames.get(stateVariableName);
			}
//			if varaible is used in the expression add the value
			if (expressionVariables.contains(stateVariableName)) {
				double val = this.initalValue;
//				find the right class for this object
				
				if (TransformedAttribute.class.isInstance(entry.getValue())) {
					val = ((TransformedAttribute) entry.getValue()).getRealValue();
				} else if (XAttributeContinuous.class.isInstance(entry.getValue())) {
					val = ((XAttributeContinuous) entry.getValue()).getValue();
				}
//				add variable name's value to new state for evaluation
				newState.put(stateVariableName, val);
			}
		}
//		ensure that all variables are included
		for(String var: expressionVariables) {
			if (!newState.containsKey(var)) {
				newState.put(var, this.initalValue);
			}
		}
		return (Boolean) this.guardExpression.evaluate(newState);
	}
	
}