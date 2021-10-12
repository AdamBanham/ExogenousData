package org.processmining.qut.exogenousaware.ml.clustering.distance;

import java.util.ArrayList;

import org.processmining.qut.exogenousaware.ml.clustering.distance.data.WarpElement;
import org.processmining.qut.exogenousaware.ml.clustering.distance.data.WarpingPath;
import org.processmining.qut.exogenousaware.ml.data.FeatureVector;
import org.processmining.qut.exogenousaware.ml.data.FeatureVectorImpl;

import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import lombok.Builder;
import lombok.Builder.Default;

@Builder
public class DynamicTimeWarpingDistancer implements Distancer {

	
	@Default Distancer cellDistancer = EuclideanDistancer.builder().build();
	
	public double distance(FeatureVector A, FeatureVector B) {
		// step one construct distance matrix of double[|A|][|B|]
		// [row][column]
		double[][] distanceMatrix = new double[A.getSize()][B.getSize()];
		// initalise everything to max value
		for(int i=0; i < A.getSize(); i++) {
			for (int j=0; j < B.getSize(); j++) {
				distanceMatrix[i][j] = Double.MAX_VALUE;
			}
		}
		// compute cell distance between ith and jth values
		for(int i=A.getSize()-1; i >= 0; i--) {
			for (int j=0; j < B.getSize(); j++) {
				distanceMatrix[i][j] = cellDistancer.distance(
						FeatureVectorImpl.builder().value(A.getValues().get(A.getSize() - (i+1))).build(), 
						FeatureVectorImpl.builder().value(B.getValues().get(j)).build()
				);
				
			}
		}
		// create wrapping path that minimises warping cost
		// path always starts at 1,1 or in this case |A|,0
		int lasti = A.getSize()-1;
		int currentLeft = 0;
		int currentRight = 0;
		WarpingPath path = WarpingPath.builder()
				.elements(new ArrayList<WarpElement>())
				.element(WarpElement.builder().LID(currentLeft).RID(currentRight).warpDistance(distanceMatrix[lasti][currentRight]).build())
				.LeftVector(A)
				.RightVector(B)
				.build();
		// find the next element to add
		while (!path.isDone()) {
			double lowestscore = Double.MAX_VALUE;
			WarpElement update = null;
//			search through options 
			for(int newLeft=currentLeft; (newLeft <= (currentLeft+1)) & (newLeft < A.getSize()); newLeft++) {
				for(int newRight=currentRight; (newRight <= (currentRight+1)) & (newRight < B.getSize()); newRight++) {
					if ((currentLeft == newLeft) & (currentRight == newRight)) {
						continue;
					}
					WarpElement newUpdate = WarpElement.builder()
							.LID(newLeft)
							.RID(newRight)
							.warpDistance(distanceMatrix[lasti - newLeft][newRight])
							.build();
					double score = path.checkCost(newUpdate);
					if (score < lowestscore) {
						update = newUpdate;
						lowestscore = score;
					}
				}
			}
			if (update == null) {
				throw new ValueException("Unable to find the next warp element along warping path");
			} else {
				currentLeft = update.getLID();
				currentRight = update.getRID();
				path.addWarpElement(update);
			}
		}
//		return the cost of path
//		System.out.println(path.showPath());
		return path.getCost();
	}

}
