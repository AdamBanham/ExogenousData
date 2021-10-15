package org.processmining.qut.exogenousaware.ds.timeseries.stats;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;

public class CovarianceTest {

	@Test
	public void canCompute() {
		System.out.println("CovarianceTest::canCompute");
		List<Double> dependents = new ArrayList<Double>();
		List<Double> independents = new ArrayList<Double>();
		int count = 0;
		while(count < 20) {
			dependents.add(Math.random() * 20);
			independents.add(Math.pow(Math.random() * 35, 1.25));
			count++;
		}
		
		System.out.println("dvalues="+dependents.toString());
		System.out.println("ivalues="+independents.toString());
		
		Covariance handler = Covariance.builder()
				.independents(independents)
				.dependents(dependents)
				.build();
		
		double cov = handler.calculate();
		
		System.out.println("cov="+cov);
		System.out.println("CovarianceTest::computed value");		
	}
	
	@Test
	public void shouldFail() {
		System.out.println("CovarianceTest::shouldFail");
		List<Double> dependents = new ArrayList<Double>();
		List<Double> independents = new ArrayList<Double>();
		int count = 0;
		while(count < 20) {
			dependents.add(Math.random() * 20);
			independents.add(Math.pow(Math.random() * 35, 1.25));
			count++;
		}
		
		independents.remove(0);
		
		System.out.println("dsize="+dependents.size());
		System.out.println("isize="+independents.size());
		
		try {	
			Covariance handler = Covariance.builder()
				.independents(independents)
				.dependents(dependents)
				.build();
			handler.calculate();
		} catch (Exception e) {
			assertTrue(e.getClass().equals(ValueException.class));
			System.out.println("CovarianceTest::failure occured as expected");
			return;
		}
		System.out.println("CovarianceTest::failure did not occured!!!");
		assertFalse(true);
	}

}
