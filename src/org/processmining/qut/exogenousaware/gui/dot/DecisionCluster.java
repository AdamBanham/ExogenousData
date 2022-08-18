package org.processmining.qut.exogenousaware.gui.dot;

import java.util.List;

import org.processmining.plugins.graphviz.dot.DotCluster;
import org.processmining.plugins.graphviz.dot.DotNode;

import lombok.Builder;
import lombok.NonNull;


@Builder
public class DecisionCluster extends DotCluster{
//	builder parameters
	@NonNull List<DotNode> members;
	@NonNull String label;
	@NonNull Integer group;
	
//	internal states
	private static String fillcolor = "#90ee9025";
	
	public DecisionCluster setup() {
		styleCluster();
		addMembers();
		
		return this;
	}
	
	public void styleCluster() {
		setOption("label", label);
		setOption("color", "black");
		setOption("margin", "10.0");
//		setOption("compound", "true");
//		setOption("group", "2");
		
		setGraphOption("style", "filled,dashed");
		setGraphOption("fillcolor", fillcolor);
		setGraphOption("clusterrank","global");
//		setGraphOption("rank" , "min");
//		setGraphOption("rankdir", "LR");
//		setGraphOption("compound", "true");
	}
	
	public void addMembers() {
		for(DotNode node: members) {
			addNode(node);
		}
	}
	
	@Override
	public String getId() {
		return super.getId();
	}

	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("subgraph \"" + getId() + "\"{\n");

		result.append("id=\"" + getId() + "\";");
//		result.append("xlabel=" + labelToString() + ";");

		appendOptions(result);

		contentToString(result);

		result.append("}");

		return result.toString();
	}
}
