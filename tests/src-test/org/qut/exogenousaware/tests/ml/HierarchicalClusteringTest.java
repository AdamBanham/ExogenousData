package org.qut.exogenousaware.tests.ml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.qut.exogenousaware.ml.clustering.HierarchicalClustering;
import org.qut.exogenousaware.ml.data.Cluster;
import org.qut.exogenousaware.ml.data.FeatureVector;
import org.qut.exogenousaware.ml.data.FeatureVectorImpl;

class HierarchicalClusteringTest {

	List<FeatureVector> clearCut = new ArrayList<FeatureVector>() {{
		add(FeatureVectorImpl.builder()
			.value(2.0)
			.column("x")
			.value(2.0)
			.column("y")
			.identifier(1)
			.build()
		);
		add(FeatureVectorImpl.builder()
			.value(1.0)
			.column("x")
			.value(1.0)
			.column("y")
			.identifier(2)
			.build()
		);
		add(FeatureVectorImpl.builder()
			.value(4.0)
			.column("x")
			.value(4.0)
			.column("y")
			.identifier(3)
			.build()
		);
		add(FeatureVectorImpl.builder()
			.value(5.0)
			.column("x")
			.value(5.0)
			.column("y")
			.identifier(4)
			.build()
		);
	}};
	
	List<FeatureVector> lessClearCut = new ArrayList<FeatureVector>() {{
		add(FeatureVectorImpl.builder()
			.value(2.0)
			.column("x")
			.value(2.0)
			.column("y")
			.identifier(1)
			.build()
		);
		add(FeatureVectorImpl.builder()
			.value(1.0)
			.column("x")
			.value(1.0)
			.column("y")
			.identifier(2)
			.build()
		);
		add(FeatureVectorImpl.builder()
				.value(3.3)
				.column("x")
				.value(3.3)
				.column("y")
				.identifier(3)
				.build()
		);
		add(FeatureVectorImpl.builder()
				.value(2.7)
				.column("x")
				.value(2.7)
				.column("y")
				.identifier(4)
				.build()
		);
		add(FeatureVectorImpl.builder()
			.value(4.0)
			.column("x")
			.value(4.0)
			.column("y")
			.identifier(5)
			.build()
		);
		add(FeatureVectorImpl.builder()
			.value(5.0)
			.column("x")
			.value(5.0)
			.column("y")
			.identifier(6)
			.build()
		);
	}};
	
	public List<FeatureVector> makeRandomObservations(int num) {
		List<FeatureVector> randomObs = new ArrayList<FeatureVector>();
		for(int i=0;i < num;i++) {
			randomObs.add(
					FeatureVectorImpl.builder()
					.value(Math.random() * 10)
					.column("x")
					.value(Math.random() * 10)
					.column("y")
					.identifier(i)
					.build()
			);
		}
		return randomObs;
	}
	
	
	@Test
	void clearCutTest() {
		System.out.println("Starting clear cut test...");
		HierarchicalClustering clusterer = HierarchicalClustering.builder()
				.clusterNum(2)
				.distance(HierarchicalClustering.DistanceType.EUCLID)
				.linkage(HierarchicalClustering.LinkageType.WARD)
				.observations(this.clearCut)
				.build();
		
		clusterer.fit();
		
		for(Cluster cluster: clusterer.getClusters()) {
			System.out.println("Found cluster : C"+cluster.getName()+" with a total of " + cluster.size() + " members.");
			System.out.println("Members are :");
			for(FeatureVector mem: cluster.getMembers()) {
				System.out.println(
					mem.getIdentifier() +
					" with values of "  +
					mem.getValues().toString());
			}
		}
		
		assertEquals(clusterer.getClusters().size(), 2);
		assertEquals(clusterer.getClusters().get(1).getMembers().contains(this.clearCut.get(0)),true);
		assertEquals(clusterer.getClusters().get(1).getMembers().contains(this.clearCut.get(1)),true);
		assertEquals(clusterer.getClusters().get(0).getMembers().contains(this.clearCut.get(2)),true);
		assertEquals(clusterer.getClusters().get(0).getMembers().contains(this.clearCut.get(3)),true);
	}
	
	@Test
	void lessClearCutTest() {
		System.out.println("Starting less clear cut test...");
		HierarchicalClustering clusterer = HierarchicalClustering.builder()
				.clusterNum(3)
				.distance(HierarchicalClustering.DistanceType.EUCLID)
				.linkage(HierarchicalClustering.LinkageType.WARD)
				.observations(this.lessClearCut)
				.build();
		
		clusterer.fit();
		
		for(Cluster cluster: clusterer.getClusters()) {
			System.out.println("Found cluster : C"+cluster.getName()+" with a total of " + cluster.size() + " members.");
			System.out.println("Members are :");
			for(FeatureVector mem: cluster.getMembers()) {
				System.out.println(
						"Identifier="   +
					mem.getIdentifier() +
					" ,Values="  +
					mem.getValues().toString());
			}
		}
		
		assertEquals(clusterer.getClusters().size(), 3);
//		check that [1.0,1.0] is in a lone cluster
		for(Cluster cluster : clusterer.getClusters()) {
			if (cluster.size() == 1) {
				assertEquals(cluster.getMembers().contains(this.lessClearCut.get(1)), true);
			}
		}
		
//		check that [2.0,2.0] & [2.7,2.7] are in the same cluster
		for(Cluster cluster : clusterer.getClusters()) {
			if (cluster.size() == 2) {
				assertEquals(cluster.getMembers().contains(this.lessClearCut.get(0)), true);
				assertEquals(cluster.getMembers().contains(this.lessClearCut.get(3)), true);
			}
		}
//		assertEquals(clusterer.getClusters().get(1).getMembers().contains(this.lessClearCut.get(0)),true);
//		assertEquals(clusterer.getClusters().get(1).getMembers().contains(this.lessClearCut.get(1)),true);
//		assertEquals(clusterer.getClusters().get(2).getMembers().contains(this.lessClearCut.get(4)),true);
//		assertEquals(clusterer.getClusters().get(2).getMembers().contains(this.lessClearCut.get(5)),true);
	}
	
	@Test
	void randomTest() {
		System.out.println("Starting random obs test over 5 data sets...");
		for(int rd=0; rd < 5; rd++) {
			System.out.println("Starting on data set "+(rd+1)+"...");
			HierarchicalClustering clusterer = HierarchicalClustering.builder()
					.clusterNum(5)
					.distance(HierarchicalClustering.DistanceType.EUCLID)
					.linkage(HierarchicalClustering.LinkageType.WARD)
					.observations(this.makeRandomObservations(250))
					.build();
			
			clusterer.fit();
			
			for(Cluster cluster: clusterer.getClusters()) {
				System.out.println("Found cluster : C"+cluster.getName()+" with a total of " + cluster.size() + " members.");
			}
			
			assertEquals(clusterer.getClusters().size(), 5);
		}
	}

}
