package org.processmining.qut.exogenousaware.stats.tests;
import java.util.List;

import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;

/**
 * The test is named for Frank Wilcoxon (1892–1965) who, in a single paper, proposed both it and the rank-sum test for two independent samples. <br>
 * The test was popularized by Sidney Siegel (1956) in his influential textbook on non-parametric statistics.<br>
 * Siegel used the symbol T for the test statistic, and consequently, the test is sometimes referred to as the Wilcoxon T-test.<br>
 * <br>
 * link: https://en.wikipedia.org/wiki/Wilcoxon_signed-rank_test<br>
 * <br>
 * Implementation used is from apache commons: <br>
 * https://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math4/stat/inference/WilcoxonSignedRankTest.html <br>
 * 
 * @author Adam Banham
 *
 */
public class WilcoxonSignedRankTester {

	private WilcoxonSignedRankTester() {};
	
	public static double computeTest(List<Double> X1, List<Double> X2) {
		double p = 0.0;
		WilcoxonSignedRankTest tester = new WilcoxonSignedRankTest();
		p = tester.wilcoxonSignedRankTest( 
			X1.stream().mapToDouble(d -> d).toArray(),
			X2.stream().mapToDouble(d -> d).toArray(),
			false
		);
		return p;
	}
	
	public static double computeRank(List<Double> X1, List<Double> X2) {
		double rank = 0.0;
		WilcoxonSignedRankTest tester = new WilcoxonSignedRankTest();
		rank = tester.wilcoxonSignedRank(
				X1.stream().mapToDouble(d -> d).toArray(),
				X2.stream().mapToDouble(d -> d).toArray()
		);
		return rank;
	}
}
