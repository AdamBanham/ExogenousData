package org.processmining.qut.exogenousaware.ml.data;

import java.util.List;

public interface FeatureVector {

	public int getSize();
	
	public List<Double> getValues();
	
	public List<String> getColumns();
	
	public double get(int index);
	
	public int getIdentifier();
	
}
