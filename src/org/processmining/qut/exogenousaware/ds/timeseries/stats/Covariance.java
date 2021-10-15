package org.processmining.qut.exogenousaware.ds.timeseries.stats;

import java.util.List;
import java.util.stream.IntStream;

import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

@Builder
public class Covariance implements Measure{
	
	@NonNull @Singular private List<Double> dependents;
	@NonNull @Singular private List<Double> independents;
	
	public double calculate() {
		if (dependents.size() != independents.size()) {
			throw new ValueException("The two given vectors for dependents and indepents are not the same length");
		}
		double imean = independents.stream().reduce(0.0, Double::sum) / independents.size();
		double dmean = dependents.stream().reduce(0.0, Double::sum) / dependents.size();
		double sum = IntStream.range(0, this.dependents.size())
				.mapToDouble(id -> {
					double xi = dependents.get(id) - dmean;
					double yi = independents.get(id) - imean;
					return xi * yi;
				})
				.sum();
		return (1.0 / dependents.size()) * sum;
	}

}
