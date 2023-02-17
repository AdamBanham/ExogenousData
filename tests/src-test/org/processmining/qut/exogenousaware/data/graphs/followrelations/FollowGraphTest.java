package org.processmining.qut.exogenousaware.data.graphs.followrelations;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.processmining.qut.exogenousaware.ds.timeseries.data.DiscreteTimePoint;
import org.processmining.qut.exogenousaware.ds.timeseries.data.DiscreteTimeSeries;

public class FollowGraphTest {
	
	public static DiscreteTimeSeries TEST_TIMESERIES = new DiscreteTimeSeries(
		new DiscreteTimePoint("a", 1.0),
		new DiscreteTimePoint("b", 2.0),
		new DiscreteTimePoint("a", 1.0),
		new DiscreteTimePoint("b", 2.0),
		new DiscreteTimePoint("c", 3.0)
	); 
	public static List<FollowPair<String>> TEST_PAIRS = new ArrayList<FollowPair<String>>() {{
		add(new FollowPair<String>("a","b",1.0));
		add(new FollowPair<String>("b","c",1.0));
		add(new FollowPair<String>("b","a",1.0));
	}};

	@Test
	public void testPairs() {
		FollowGraph graph = new FollowGraph(TEST_TIMESERIES);
		
		for(FollowPair expected: TEST_PAIRS) {
			if (!graph.contains(expected)) {
				fail("Expected pair not found :: " + expected.toString());
			}
		}
	}
	
	@Test
	public void testEventuallyFollows() {
		FollowGraph graph = new FollowGraph(TEST_TIMESERIES);
		
		if (!graph.checkForEventualFollowsBetween("a", "b")) {
			fail("Expected to find :: a -> b");
		}
		if (!graph.checkForEventualFollowsBetween("b", "a")) {
			fail("Expected to find :: b -> a");
		}
		if (!graph.checkForEventualFollowsBetween("b", "c")) {
			fail("Expected to find :: b -> c");
		}
		if (!graph.checkForEventualFollowsBetween("a", "c")) {
			fail("Expected to find :: a -> c");
		}
		if (graph.checkForEventualFollowsBetween("c", "c")) {
			fail("Did not expect to find :: c -> c");
		}
	}

}
