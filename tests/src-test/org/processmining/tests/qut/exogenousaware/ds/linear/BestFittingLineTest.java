package org.processmining.tests.qut.exogenousaware.ds.linear;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.processmining.qut.exogenousaware.ds.linear.BestFittingLine;

public class BestFittingLineTest {
	
	Double simple_x_1 = 25.0;
	Double simple_x_2 = 50.0;
	Double simple_x_3 = 75.0;
	
	Double simple_y_1 = 25.0;
	Double simple_y_2 = 42.0;
	Double simple_y_3 = 59.0;

	
	
	
	@Test
	public void twoPointtest() {
		BestFittingLine controller = BestFittingLine.builder()
				.X(simple_x_1)
				.X(simple_x_2)
				.Y(simple_y_1)
				.Y(simple_y_2)
				.build()
				.findSlope()
				.findIntercept();
		
		System.out.println("Slope found was :: "+ controller.getSlope());
		System.out.println("Intercept found was  :: "+ controller.getIntercept());
		
		boolean near = Math.abs(simple_y_1 - controller.findY(simple_x_1)) < 0.01;
		assertTrue(near);
		near = Math.abs(simple_y_2 - controller.findY(simple_x_2)) < 0.01;
		assertTrue(near);
	}
	
	@Test
	public void threePointtest() {
		BestFittingLine controller = BestFittingLine.builder()
				.X(simple_x_1)
				.X(simple_x_2)
				.X(simple_x_3)
				.Y(simple_y_1)
				.Y(simple_y_2)
				.Y(simple_y_3)
				.build()
				.findSlope()
				.findIntercept();
		
		System.out.println("Slope found was :: "+ controller.getSlope());
		System.out.println("Intercept found was  :: "+ controller.getIntercept());
		
		boolean near = Math.abs(simple_y_1 - controller.findY(simple_x_1)) < 0.01;
		assertTrue(near);
		near = Math.abs(simple_y_2 - controller.findY(simple_x_2)) < 0.01;
		assertTrue(near);
		near = Math.abs(simple_y_3 - controller.findY(simple_x_3)) < 0.01;
		assertTrue(near);
	}


}
