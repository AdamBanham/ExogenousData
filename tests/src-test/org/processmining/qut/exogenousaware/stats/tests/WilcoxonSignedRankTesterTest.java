package org.processmining.qut.exogenousaware.stats.tests;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class WilcoxonSignedRankTesterTest {

	List<Double> X1_1 = new ArrayList<Double>() {{
		add(110.0);
		add(122.0);
		add(125.0);
		add(120.0);
		add(140.0);
		add(124.0);
		add(123.0);
		add(137.0);
		add(135.0);
		add(145.0);		
	}};
	
	List<Double> X2_1 = new ArrayList<Double>() {{
		add(125.0);
		add(115.0);
		add(130.0);
		add(140.0);
		add(140.0);
		add(115.0);
		add(140.0);
		add(125.0);
		add(140.0);
		add(135.0);
	}};
	
	
	@Test
	public void sameLength() {
		
		List<Double> X1 = new ArrayList();
		X1.addAll(X2_1);
		X1.remove(0);
		
		List<Double> X2 = new ArrayList();
		X2.addAll(X1_1);
		
		try {
			double p = WilcoxonSignedRankTester.computeTest(X1, X2);
		} catch (Exception e) {
			assertTrue(true);
			System.out.println("Could not compute as X1 and X2 are not the same length.");
			return;
		}
		assertTrue(false);
	}
	
	@Test
	public void canCompute() {
		List<Double> X1 = new ArrayList();
		X1.addAll(X2_1);
		List<Double> X2 = new ArrayList();
		X2.addAll(X1_1);
		
		double T = WilcoxonSignedRankTester.computeRank(X1, X2);
		double p = WilcoxonSignedRankTester.computeTest(X1, X2);
		
		System.out.println("test statistic was :: "+ T);
		System.out.println("p-value was :: "+ p);
	}

}
