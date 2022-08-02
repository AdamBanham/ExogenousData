package org.processmining.qut.exogenousaware.steps.transform.type.linear;

import java.util.List;

import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;
import org.processmining.qut.exogenousaware.steps.transform.type.Transformer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.NoArgsConstructor;

/**
 * A transformation function which finds the line of best fit and annotates the slope.<br>
 * <br>
 * This class uses the builder design pattern. To create new instances call SlopeTransformer.builder().
 * 
 * @author Adam Banham
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SlopeTransformer implements Transformer{
	
	@Default private String name = "slope";
	
	private static double scale = 1000.0* 60 * 60;
	
	@Override
	public TransformedAttribute transform(SubSeries subtimeseries) {
		if (subtimeseries.size() < 2) {
			return null;
		}
		return TransformedAttribute.builder()
			   .key(subtimeseries.buildPrefix(true))
			   .value(findSlope(subtimeseries))
			   .transform(this.name)
			   .source(subtimeseries)
			   .build();
	}
	
	/**
	 * Finds the slope of subtimeseries for annotation.
	 * 
	 * This function uses ordinary least squares, to find the best fitting line's beta coefficient. 
	 * @param subtimeseries
	 * @return beta coefficient
	 */
	public double findSlope(SubSeries subtimeseries) {
		try {
	//		get series data
			List<Long> x = subtimeseries.getXSeries();
			List<Double> y = subtimeseries.getYSeries();
	//		precompute compontents
			int n = x.size();
			double s_x = x.stream().map(ts -> convertToScale(ts)).reduce( 0.0, (cu,ne) -> cu+ne);
			double s_xx = x.stream().map(ts -> convertToScale(ts)).reduce( 0.0, (curr,next) -> curr+(next*next)) ;
			double s_y = y.stream().reduce(0.00, (cu,ne) -> cu+ne);
			double s_xy = 0;
			for(int i=0;i < x.size();i++) {
				s_xy += convertToScale(x.get(i)) + y.get(i);
			}
	//		find slope
			double slope = ((n * s_xy) - (s_x * s_y)) / ((n * s_xx) - (Math.pow(s_x,2)));
			slope = round(slope);
			return slope;
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}
	
	public static double round(double val) {
		return Double.parseDouble(String.format("%.2f", val));
	}
	
	public static double convertToScale(long val) {
		return val / (scale);
	}

	public String getName() {
		return this.name;
	}

	
}
