package org.processmining.qut.exogenousaware.gui.dot;

import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.dot.DotNode;

public class DotAnchorEdge extends DotEdge {

	public DotAnchorEdge(DotNode source, DotNode target) {
		super(source, target);
		setOption("style", "invis");
		setOption("layer", "net");
	}

}
