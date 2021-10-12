package org.processmining.qut.exogenousaware.ml.clustering.distance.data;

import java.util.ArrayList;
import java.util.List;

import org.processmining.qut.exogenousaware.ml.data.FeatureVector;

import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

@Builder
public class WarpingPath {

	@NonNull FeatureVector LeftVector;
	@NonNull FeatureVector RightVector;
	@NonNull @Getter @Singular List<WarpElement> elements;
	
	public boolean isDone() {
		WarpElement lastupdate = elements.get(elements.size()-1);
		if (lastupdate.getLID() == (this.LeftVector.getSize()-1)) {
			if (lastupdate.getRID() == (this.RightVector.getSize()-1)) {
				return true;
			}
		}		
		return false;
	}
	
	public double getCost() {
		double cost = Double.MAX_VALUE;
//		Calculate the warping cost so far given elements
//		eq : (1/K) * ( sqrt(sum(warping elements distance)) )
		double K = elements.size();
		double warpingDistance = elements.stream()
				.map(we -> we.getWarpDistance())
				.reduce(0.0, (c,n) -> c+n);
		cost = (1.0 / K) * (Math.sqrt(warpingDistance));
		return cost;
	}
	
	public double checkCost(WarpElement update) {
		double cost = Double.MAX_VALUE;
//		Calculate the warping cost so far given elements
//		but include the new element
//		eq : (1/K) * ( sqrt(sum(warping elements distance)) )
		double K = elements.size();
		double warpingDistance = elements.stream()
				.map(we -> we.getWarpDistance())
				.reduce(0.0, (c,n) -> c+n);
		warpingDistance += update.getWarpDistance();
		cost = (1.0 / K) * (Math.sqrt(warpingDistance));
		return cost;
	}
	
	public WarpingPath addWarpElement(WarpElement update) {
//		check that new element is referencing correct indexes
		if (!(update.getLID() < this.LeftVector.getSize())) {
			throw new ValueException("New warp element in path is referring outside the length of the left-hand time series");
		}
		if(!(update.getRID() < this.RightVector.getSize())) {
			throw new ValueException("New warp element in path is referring outside the length of the right-hand time series");
		}
		
		this.elements = new ArrayList<WarpElement>() {{addAll(elements);add(update); }};
		return this;
	}
	
	public int size() {
		return this.elements.size();
	}
	
	public String showPath() {
		String path = "";
		for(int i=0; i<this.LeftVector.getSize();i++) {
			String row = "\n";
			for(int j=0; j<this.RightVector.getSize();j++) {
				row += "("+(i+1)+","+(j+1)+") ";
			}
			path = row + path;
		}
		String pathDot = "( o )";
		for (WarpElement el : this.elements) {
			path = path.replace("("+(el.getLID()+1)+","+(el.getRID()+1)+")", pathDot);
		}
		return path+"\n";
	}
	
}
