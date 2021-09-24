package org.qut.exogenousaware.ml.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.Singular;


@Builder
public class ClusterImpl implements Cluster {

	@NonNull @Getter @Singular List<FeatureVector> members;
	@NonNull @Getter @Setter String name;
	
	@Default @Getter Map<Cluster, Double> cachedDistances = new HashMap<Cluster, Double>(); 
	
	public FeatureVector getMember(int index) {
		return this.members.get(index);
	}

	public int size() {
		return this.members.size();
	}
	
	public boolean checkDistance(Cluster B) {
		return this.cachedDistances.containsKey(B);
	}
	
	public double getDistance(Cluster B) {
		return this.cachedDistances.containsKey(B) ? this.cachedDistances.get(B) : null;
	}
	
	public boolean addDistanceToCache(Cluster B, double distance) {
		if (this.cachedDistances.containsKey(B)) {
			return false;
		} else {
			this.cachedDistances.put(B, distance);
			return true;
		}
	}

}
