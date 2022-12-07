package org.processmining.qut.exogenousaware.gui.dot;

import org.processmining.plugins.graphviz.dot.DotCluster;
import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.dot.DotNode;

public class ExoDotEdge extends DotEdge {

	public ExoDotEdge(DotNode source, DotNode target) {
		super(source, target);
		if (source.getClass().equals(ExoDotTransition.class)) {
			setOption("tailport", "TITLE");
		}
		if (target.getClass().equals(ExoDotTransition.class)) {
			setOption("headport", "TITLE");
		}
		setOption("penwidth", "3.0");
		setOption("xlabel", "");
		setOption("layer", "net");
//		setOption("label", null);
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

		String result = "\"" + localSource.getId() + "\" -> \"" + localTarget.getId() + "\" [ id=\"" + getId() + "\"";

		for (String key : getOptionKeySet()) {
			result += "," + key + "=" + escapeString(getOption(key));
		}
		
		/**
		 * If the edges goes to/from a cluster, we need to set the lhead/ltail.
		 */
		if (localSource != getSource()) {
			result += ",ltail=" + escapeString(getSource().getId());
		}
		if (localTarget != getTarget()) {
			result += ",lhead=" + escapeString(getTarget().getId());
		}

		return result + "];";
	}
	
}