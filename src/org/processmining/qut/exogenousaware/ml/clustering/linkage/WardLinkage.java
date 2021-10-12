package org.processmining.qut.exogenousaware.ml.clustering.linkage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.processmining.qut.exogenousaware.ml.clustering.distance.Distancer;
import org.processmining.qut.exogenousaware.ml.data.Cluster;
import org.processmining.qut.exogenousaware.ml.data.ClusterImpl;
import org.processmining.qut.exogenousaware.ml.data.FeatureVector;

import lombok.Builder;
import lombok.NonNull;

@Builder
public class WardLinkage implements LinkageDistancer {

	@NonNull Distancer distancer;
	
	
	
	
	public List<Cluster> findNextPair(List<Cluster> clusters) {
		AtomicFloat lowestScore = new AtomicFloat(Double.MAX_VALUE);
		List<Cluster> combinePair = new ArrayList<Cluster>();
		combinePair.add(null);
		combinePair.add(null);
//		loop through each cluster and calculate the new within cluster combined that could occur if combined
//		keep track the lowest combination noted
		long starttime = System.currentTimeMillis();
		IntStream.range(0, clusters.size()-1)
			.parallel()
			.forEach(i -> compare(clusters.get(i), lowestScore, clusters.subList(i+1,clusters.size())));
//		int sublist = 1;
//		for(Cluster A: clusters) {
//			ClusterImpl a = (ClusterImpl) A;
//			for(Cluster B: clusters.subList(sublist, clusters.size())) {
//				if(A.equals(B)) {
//					continue;
//				}
//				double score;
//				if (!a.checkDistance(B)) {
//	//				find distance between clusters
//					score = checkCombineOutcome(A, B);
//	//				save distance for future
//					a.addDistanceToCache(B, score);
//				} else {
//					score = a.getDistance(B);
//				}
//				if (!started) {
//					lowestScore = score;
//					combinePair.set(0, A);
//					combinePair.set(1, B);
//					started = true;
//					
//				} else if (score < lowestScore) {
//					lowestScore = score;
//					combinePair.set(0, A);
//					combinePair.set(1, B);
//				}
//				
//			}
//			sublist++;
//		}
//		System.out.println("[HierarchicalClustering] Finding pair to combine took :: " + (System.currentTimeMillis() - starttime));
		return lowestScore.getLowestPair();
	}
	
	public double checkCombineOutcome(Cluster A, Cluster B) {
		double withinClusterDifference = 0;
		int submembercount = 1;
//		calculate the inter member distance if A and B where combined
		List<FeatureVector> members = new ArrayList<FeatureVector>();
		members.addAll(A.getMembers());
		members.addAll(B.getMembers());
		for(FeatureVector a : members) {
			for(FeatureVector b: members.subList(submembercount, members.size())) {
				withinClusterDifference += this.distancer.distance(a, b);
			}
			submembercount++;
		}
		return withinClusterDifference;
	}
	
	public void compare(Cluster A, AtomicFloat scorer, List<Cluster> others) {
		ClusterImpl a = (ClusterImpl) A;
		double score;
		for(Cluster B: others) {
			if(A.equals(B)) {
				continue;
			}
			if (!a.checkDistance(B)) {
//				find distance between clusters
				score = checkCombineOutcome(A, B);
//				save distance for future
				a.addDistanceToCache(B, score);
			} else {
//				need to add catch for exception that cannot occur
//				already tested in previous if but cannot return null to build
				try {
					score = a.getDistance(B);
				} catch (Exception e) {
					score = -1;
				}
			}
			if (scorer.scoreIsLessThan(score)) {
				scorer.swap(score,a,B);
			}
			
		}
	}

	public static class AtomicFloat extends Number {
		
		private AtomicLong bits;
		private List<Cluster> combinePair;
		
		 public AtomicFloat() {
		        this(0.0);
		    }

	    public AtomicFloat(double initialValue) {
	        bits = new AtomicLong(Double.doubleToLongBits(initialValue));
	        combinePair = new ArrayList<Cluster>();
	        combinePair.add(null);
	        combinePair.add(null);
	    }
	    
	    public boolean scoreIsLessThan(double update) {
	    	return get() > update;
	    }
	    
	    public synchronized boolean swap(double update, Cluster A, Cluster B) {
	    	boolean changed = scoreIsLessThan(update);
	    	bits.set(Double.doubleToLongBits(update));
	    	combinePair.set(0, A);
	    	combinePair.set(1, B);
	    	return changed;
	    }
		
		public double get() {
			return Double.longBitsToDouble(bits.get());
		}

		public int intValue() { return (int) get();	}

		public long longValue() { return (long)	get();}

		public float floatValue() {return (float) get();}

		public double doubleValue() { return get();}
		
		public List<Cluster> getLowestPair(){
			return this.combinePair;
		}
		
	}
}
