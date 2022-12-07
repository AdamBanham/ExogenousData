package org.processmining.qut.exogenousaware.gui.dot;

import org.processmining.plugins.graphviz.dot.DotCluster;
import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.dot.DotNode;

public class BackwardDotEdge extends DotEdge {

	public BackwardDotEdge(DotNode source, DotNode target) {
		super(source, target);
		
		if (source.getClass().equals(ExoDotTransition.class)) {
			setOption("headport", "TITLE");
		}
		if (target.getClass().equals(ExoDotTransition.class)) {
			setOption("tailport", "TITLE");
		}
		
		setOption("dir","back");
		setOption("penwidth", "3.0");
	}
	
	@Override
	public String toString() {
		/**
		 * Dot does not support edges from/to clusters. I such edges are added,
		 * use an arbitrary node in the cluster as the target.
		 */
		DotNode localSource = getSource();
		DotNode localTarget = getTarget();
		{
			if (localSource instanceof DotCluster && !((DotCluster) localSource).getNodes().isEmpty()) {
				localSource = ((DotCluster) localSource).getNodes().get(0);
			}
			if (localTarget instanceof DotCluster && !((DotCluster) localTarget).getNodes().isEmpty()) {
				localTarget = ((DotCluster) localTarget).getNodes().get(0);
			}
		}

		String result = "\"" + localTarget.getId() + "\" -> \"" + localSource.getId() + "\" [ id=\"" + getId() + "\"";

		for (String key : getOptionKeySet()) {
			result += "," + key + "=" + escapeString(getOption(key));
		}
		
		/**
		 * If the edges goes to/from a cluster, we need to set the lhead/ltail.
		 */
		if (localSource != getTarget()) {
			result += ",ltail=" + escapeString(getTarget().getId());
		}
		if (localTarget != getSource()) {
			result += ",lhead=" + escapeString(getSource().getId());
		}

		return result + "];";
	}

}
