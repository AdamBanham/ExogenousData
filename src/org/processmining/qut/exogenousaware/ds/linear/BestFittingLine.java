package org.processmining.qut.exogenousaware.ds.linear;

import java.util.List;
import java.util.stream.IntStream;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

@Builder
public class BestFittingLine {

	@NonNull @Singular List<Double> Xs;
	@NonNull @Singular List<Double> Ys;
	
	@Default @Getter private double slope = 0.0;
	@Default @Getter private double intercept = 0.0;
	
	public BestFittingLine findSlope() {
		double s_x = this.Xs.stream().reduce(0.0, (c,n) -> c+n);
		double s_xy = IntStream.range(0, this.Xs.size())
				.mapToDouble(i -> {return Xs.get(i)*Ys.get(i);})
				.reduce(0.0, (c,n) -> c+n );
		double s_y = this.Ys.stream().reduce(0.0, (c,n) -> c+n);
		double s_xx = this.Xs.stream().reduce(0.0, (c,n) -> {return c + (n*n);});
		int n = this.Xs.size();
		this.slope = ((n*s_xy) - (s_x * s_y)) / ((n*s_xx) - (Math.pow(s_x, 2)));
		return this;
	}
	
	public BestFittingLine findIntercept() {
		double s_x = this.Xs.stream().reduce(0.0, (c,n) -> c+n);
		double s_y = this.Ys.stream().reduce(0.0, (c,n) -> c+n);
		int n = this.Xs.size();
		this.intercept = ( (1.0/n) * s_y ) - (this.slope * (1.0/n) * s_x);
		return this;
	}
	
	public double findY(double x) {
		return x * this.slope + this.intercept;
	}
}
