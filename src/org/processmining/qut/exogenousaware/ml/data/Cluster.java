package org.processmining.qut.exogenousaware.ml.data;

import java.util.List;

public interface Cluster {
	
	public List<FeatureVector> getMembers();
	
	public FeatureVector getMember(int index);
	
	public String getName();

	public int size();
}
