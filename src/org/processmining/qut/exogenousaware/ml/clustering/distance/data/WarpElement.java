package org.processmining.qut.exogenousaware.ml.clustering.distance.data;

import lombok.Builder;
import lombok.Getter;

@Builder
public class WarpElement {

	@Getter int LID;
	@Getter int RID;
	@Getter double warpDistance;
}
