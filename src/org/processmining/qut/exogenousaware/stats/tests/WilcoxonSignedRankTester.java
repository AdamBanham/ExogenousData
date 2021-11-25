package org.processmining.qut.exogenousaware.stats.tests;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;
import org.processmining.qut.exogenousaware.ml.data.FeatureVector;

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
	
	public static List<Integer> findLongestMatchingVector(FeatureVector x1, FeatureVector x2){
		List<Integer> matchingZone = new ArrayList<Integer>();
		// go through columns of x1 and find longest matching
		for(int start: IntStream.range(0, x1.getSize()).boxed().collect(Collectors.toList())) {
			int end = start-1;
			int starter = start;
			for (int id: IntStream.range(0, x2.getSize()).boxed().collect(Collectors.toList())) {
				String key = x1.getColumns().get(starter);
				String compKey = x2.getColumns().get(id);
	//			System.out.println("[CommonLength] comparing between "+key +" and  "+compKey+"::"+key.compareTo(compKey));
				if (key.compareTo(compKey) == 0 && (end+1) < Math.min(x1.getSize(), x2.getSize())) {
					end++;
				} else {
					break;
				}
				if (starter+1 < x1.getSize()) {
					starter++;
				} else {
					break;
				}
			}
			// edge case (1) we didn't ever set length or we need to close a length
			if (start < end) {
				List<Integer> temp = IntStream.range(start, end).boxed().collect(Collectors.toList());
				if (temp.size() > matchingZone.size() && temp.size() <= Math.min(x1.getSize(), x2.getSize()) ) {
	//				System.out.println("[CommonLength] new length::"+temp.size());
					matchingZone = temp;
				}
			}
		}
		
		return matchingZone;
	}
}
