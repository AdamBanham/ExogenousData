package org.processmining.qut.exogenousaware.ml.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.JProgressBar;

import org.processmining.qut.exogenousaware.ml.clustering.distance.Distancer;
import org.processmining.qut.exogenousaware.ml.clustering.distance.DynamicTimeWarpingDistancer;
import org.processmining.qut.exogenousaware.ml.clustering.distance.EuclideanDistancer;
import org.processmining.qut.exogenousaware.ml.clustering.linkage.LinkageDistancer;
import org.processmining.qut.exogenousaware.ml.clustering.linkage.WardLinkage;
import org.processmining.qut.exogenousaware.ml.data.Cluster;
import org.processmining.qut.exogenousaware.ml.data.ClusterImpl;
import org.processmining.qut.exogenousaware.ml.data.FeatureVector;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

@Builder
public class HierarchicalClustering {
	
	int clusterNum;
	@NonNull DistanceType distance;
	@NonNull LinkageType linkage;
	@NonNull @Singular List<FeatureVector> observations;
	
	@Default JProgressBar progress = null;
	@Default @Getter List<Cluster> clusters = new ArrayList<Cluster>();
	@Default @Getter Distancer distancer = null;
	@Default @Getter LinkageDistancer linkager = null;
	@Default int bulkprocess = 10;
		
	public HierarchicalClustering fit() {
		setDistancer();
		setLinkage();
//		for each observation create a cluster 
		this.clusters = IntStream.range(0, this.observations.size())
				.parallel()
				.mapToObj( id -> ClusterImpl.builder().member(this.observations.get(id)).name(Integer.toString(id)).build() )
				.collect(Collectors.toList());
//		continue to reduce cluster numbers until we have the desired cluster size
		int counter = this.clusters.size();
		long starttime = System.currentTimeMillis();
		while (this.clusters.size() > this.clusterNum) {
//			System.out.println("[HierarchicalClustering] Current cluster count : "+this.clusters.size());
//			shortcut one (half the num of clusters)
			if ((this.clusters.size() /2) > (this.clusterNum < 500 ? 500 : this.clusterNum) ) {
				// keep removing until every cluster has a partner 
				List<List<Cluster>> candiatePairs = new ArrayList<List<Cluster>>();
				List<Cluster> candiatePair = new ArrayList<Cluster>();
				// find candiate pairs
				int processSize = this.clusters.size() - this.bulkprocess;
				while (this.clusters.size() > processSize) {
					candiatePair = this.linkager.findNextPair(this.clusters);
					this.clusters.remove(candiatePair.get(0));
					this.clusters.remove(candiatePair.get(1));
					candiatePairs.add(candiatePair);
					if ((this.progress != null)) {
						this.progress.setValue(counter - this.clusters.size());
					}
				}
				// add all pairs back to pool
				for(List<Cluster> pair: candiatePairs) {
					Cluster newCluster = ClusterImpl.builder()
						.members(pair.get(0).getMembers())
						.members(pair.get(1).getMembers())
						.name(pair.get(0).getName())
						.build();
					this.clusters.add(newCluster);
				}
			} else {
	//			find the next pair to combine
				List<Cluster> candiatePair = this.linkager.findNextPair(this.clusters);
	//			remove pair parts from list and create new cluster
	//			System.out.println("Combining the following clusters : ("+candiatePair.get(0).getName()+","+candiatePair.get(1).getName()+")");
				this.clusters.remove(candiatePair.get(0));
				this.clusters.remove(candiatePair.get(1));
				Cluster newCluster = ClusterImpl.builder()
						.members(candiatePair.get(0).getMembers())
						.members(candiatePair.get(1).getMembers())
						.name(candiatePair.get(0).getName())
						.build();
				this.clusters.add(newCluster);
				if ((this.progress != null)) {
					this.progress.setValue(counter - this.clusters.size());
				}
			}
		}
//		reset cluster names to be inline with number of desired clusters
		IntStream.range(1, this.clusterNum+1)
			.forEach( id -> ((ClusterImpl) this.clusters.get(id-1)).setName(Integer.toString(id)));
		System.out.println("[HierarchicalClustering] Clustering took : "+ (System.currentTimeMillis() - starttime));
		return this;
	}
	
	private void setDistancer() {
		if (DistanceType.EUCLID.equals(this.distance)) {
			this.distancer = EuclideanDistancer.builder().build();
		} else if (DistanceType.DTW.equals(this.distance)) {
			this.distancer = DynamicTimeWarpingDistancer.builder().build();
		}
	}
	
	private void setLinkage() {
		if (LinkageType.WARD.equals(this.linkage)) {
			this.linkager = WardLinkage.builder().distancer(this.distancer).build();
		}
	}
	
	static public enum LinkageType {
		WARD("ward"),
		MAX("complete-linkage"),
		MIN("single-linkage");
		
		public String label;
		
		private LinkageType(String label) {
			this.label = label;
		}
	}
	
	static public enum DistanceType {
		EUCLID("Euclidean distance"),
		SQEUC("Squared Euclidean distance"),
		MAN("Manhattan distance"),
		DTW("Dynamic Time Warping");
		
		public String label;
		
		private DistanceType(String label) {
			this.label = label;
		}
	}
}
