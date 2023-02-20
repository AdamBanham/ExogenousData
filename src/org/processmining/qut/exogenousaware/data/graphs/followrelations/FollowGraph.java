package org.processmining.qut.exogenousaware.data.graphs.followrelations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.processmining.qut.exogenousaware.ds.timeseries.data.DiscreteTimePoint;
import org.processmining.qut.exogenousaware.ds.timeseries.data.DiscreteTimeSeries;

/**
 * A simple interface to create directly or eventually follow relations from a 
 * graph structure.
 * @author Adam Banham
 *
 */
public class FollowGraph {

	protected List<FollowPair<String>> nodes;
	protected List<FollowPair<String>> strictNodes;
	
//	constructors for discrete time series
	public FollowGraph(DiscreteTimeSeries ... series) {
		this.nodes = new ArrayList<FollowPair<String>>();
		for(int i=0; i < series.length; i++) {
			DiscreteTimeSeries tseries = series[i];
			processTimeSeries(tseries);
		}
	}
	
	public FollowGraph(List<DiscreteTimeSeries> series) {
		this.nodes = new ArrayList<FollowPair<String>>();
		for(DiscreteTimeSeries tseries : series) {
			processTimeSeries(tseries);
		}
	}
	
	protected void processTimeSeries(DiscreteTimeSeries tseries) {
		List<DiscreteTimePoint> points = tseries.getPoints();
		for(int j=0; j < tseries.getSize()-1; j ++) {
			DiscreteTimePoint left = points.get(j);
			DiscreteTimePoint right = points.get(j+1);
			FollowPair<String> pair = new FollowPair<String>(
					left.getValue(),
					right.getValue(),
					1
			);
			if (nodes.contains(pair)) {
				FollowPair<String> oldPair = nodes.get(nodes.indexOf(pair));
				nodes.remove(oldPair);
				nodes.add(oldPair.combine(pair));
			} else {
				nodes.add(pair);
			}
		}
	}
	
//	utility functions
	public boolean checkForEventualFollowsBetween(String left, String right) {
//		check for a direct pair
		FollowPair<String> pair = new FollowPair<String>(left, right , 1);
		if (nodes.contains(pair)) {
			return true;
		}
//		check for an indirect path
		List<FollowPair<String>> starts = nodes.stream()
				.filter(n -> n.getLeft().equals(left))
				.collect(Collectors.toList());
		for(FollowPair<String> starter : starts) {
			Set<FollowPair<String>> seenPairs = new HashSet();
			seenPairs.add(starter);
			List<FollowPair<String>> next = nodes.stream()
					.filter(n -> n.getLeft().equals(starter.getRight()))
					.filter(n -> !seenPairs.contains(n))
					.collect(Collectors.toList());
			while( next.size() > 0) {
				FollowPair<String> nextPair = next.remove(0);
				if (nextPair.getRight().equals(right)) {
					return true;
				} else {
					seenPairs.add(nextPair);
					List<FollowPair<String>> others = nodes.stream()
							.filter(n -> n.getLeft().equals(nextPair.getRight()))
							.filter(n -> !seenPairs.contains(n))
							.collect(Collectors.toList());
					next.addAll(others);
				}
			}
		}
		return false;
	}
	
	public boolean contains(FollowPair<String> other) {
		return nodes.contains(other);
	}
	
	@Override
	public String toString() {
		return this.nodes.toString();
	}
}
