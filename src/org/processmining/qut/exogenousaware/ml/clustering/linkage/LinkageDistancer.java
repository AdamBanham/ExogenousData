package org.processmining.qut.exogenousaware.ml.clustering.linkage;

import java.util.List;

import org.processmining.qut.exogenousaware.ml.data.Cluster;

public interface LinkageDistancer {
	
	/**
	 * Finds the next pair of clusters which should be merged together.
	 * 
	 * @param clusters current collection of clusters
	 * @return a list of two clusters to combine
	 */
	public List<Cluster> findNextPair(List<Cluster> clusters);

}
