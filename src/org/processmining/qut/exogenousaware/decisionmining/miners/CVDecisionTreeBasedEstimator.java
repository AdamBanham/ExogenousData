package org.processmining.qut.exogenousaware.decisionmining.miners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.processmining.datadiscovery.estimators.AbstractDecisionTreeFunctionEstimator;
import org.processmining.datadiscovery.estimators.DecisionTreeBasedFunctionEstimator;
import org.processmining.datadiscovery.estimators.FunctionEstimation;
import org.processmining.datadiscovery.estimators.Type;

import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;
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
	extends AbstractDecisionTreeFunctionEstimator {
	
    //states
	protected T bestEstimator = null;
	protected int cvFolds = 5;
	protected Instances data;
	
	public CVDecisionTreeBasedEstimator(Map<String, Type> attributeType,
			Map<String, Set<String>> literalValues, Object[] outputClasses, int capacity, String name) {
		super(name, attributeType, literalValues, outputClasses, capacity);
		
	}

	public Map<Object, FunctionEstimation> getFunctionEstimation(Object[] option) throws Exception {
		Map<Object, FunctionEstimation> retValue = new HashMap();
		// create folds
		// Make a copy of the data we can reorder
		Instances data = new Instances(this.data);
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

	public void addInstance(Map<String, Object> attributes, Object classObject, float weight) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public Object classifyInstance(Map<String, Object> attributes) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public double computeQualityMeasure() {
		// TODO Auto-generated method stub
		return 0;
	}

	protected ArrayList<Attribute> createAttributeList(Map<String, Type> attributeType,
			Map<String, Set<String>> literalValues, Object[] outputClasses) {
		// TODO Auto-generated method stub
		return null;
	}

	protected AbstractClassifier createClassifier(Object[] option, boolean saveData) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
}
