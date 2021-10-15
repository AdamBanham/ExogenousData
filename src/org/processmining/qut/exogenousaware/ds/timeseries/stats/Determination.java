package org.processmining.qut.exogenousaware.ds.timeseries.stats;

import java.util.List;

import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

/**
 * This class calculates the coefficient of determination, denoted as R<sup>2</sup>.
 * <br><br>
 * The coefficient describes the proportion of the variation in the dependent variable that is predictable from the independent variable(s).
 * 
 * @author n7176546
 *
 */
@Builder
public class Determination {

	@NonNull @Singular private List<Double> dependents;
	@NonNull @Singular private List<Double> independents;
	
	public void calculate() {
//		check that dependents and independents are the same length
		if (dependents.size() != independents.size()) {
			throw new ValueException("The two given vectors for dependents and indepents are not the same length");
		}
		double cov = Covariance.builder().independents(independents).dependents(dependents).build().calculate();
	}
}
