package org.processmining.qut.exogenousaware.gui.dot;

import java.util.HashMap;

import org.processmining.plugins.graphviz.dot.DotNode;

public class DotAnchor extends DotNode {

	public DotAnchor(String side) {
		super(side+"-anchor", new HashMap());
		// TODO Auto-generated constructor stub
		setOption("style", "invis");
		setOption("layer", "net");
	}

}
