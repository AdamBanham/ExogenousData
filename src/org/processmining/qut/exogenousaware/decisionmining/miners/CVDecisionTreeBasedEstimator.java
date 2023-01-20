package org.processmining.qut.exogenousaware.decisionmining.miners;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.processmining.datadiscovery.estimators.DecisionTreeBasedFunctionEstimator;
import org.processmining.datadiscovery.estimators.FunctionEstimation;
import org.processmining.datadiscovery.estimators.Type;
import org.processmining.datadiscovery.estimators.impl.DecisionTreeFunctionEstimator;

import weka.core.Instances;

/*
 * This a thin wrapper around OverlappingEstimatorLocalDecisionTree, which 
 * performs cross validation for the selection of the built model. X subsets 
 * of the given instances are made, then X models are made training on X - 1 
 * subsets and testing on the held out subset for the selection of the best model.
 * 
 * See org.processmining.datadiscovery.estimators.impl.OverlappingEstimatorLocalDecisionTree;
 */
public class CVDecisionTreeBasedEstimator<T extends DecisionTreeBasedFunctionEstimator> 
	extends DecisionTreeFunctionEstimator {
	
    //states
	protected T dummyEstimator = null;
	protected T bestEstimator = null;
	protected int cvFolds = 5;
	
	public CVDecisionTreeBasedEstimator(Map<String, Type> attributeType,
			Map<String, Set<String>> literalValues, Object[] outputClasses, int capacity, String name) {
		super(attributeType, literalValues, outputClasses, name, capacity);
		try {
			Class[] clazzes = new Class[5];
			dummyEstimator = (T) dummyEstimator.getClass()
					.getConstructor()
					.newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Map<Object, FunctionEstimation> getFunctionEstimation(Object[] option) throws Exception {
		Map<Object, FunctionEstimation> retValue = new HashMap();
		// create folds
		// Make a copy of the data we can reorder
		Instances data = new Instances(this.instances);
	    data.randomize(new Random());
	    if (data.classAttribute().isNominal()) {
	      data.stratify(cvFolds);
	    }
	    // store evaluation of each fold
	    Map<Integer, Float> scores = new HashMap();
	    Map<Integer, Map<Object, FunctionEstimation>> results = new HashMap();
	    // perform construction
		for(int i=0; i < cvFolds;i++ ) {
			
		}
		
		return retValue;
	}
	
}
