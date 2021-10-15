package org.processmining.qut.exogenousaware.gui.dot;

import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.dot.DotNode;

public class ExoDotEdge extends DotEdge {

	public ExoDotEdge(DotNode source, DotNode target) {
		super(source, target);
		if (source.getClass().equals(ExoDotTransition.class)) {
			setOption("tailport", "HEAD");
		}
		if (target.getClass().equals(ExoDotTransition.class)) {
			setOption("headport", "HEAD");
		}
	}
	
}