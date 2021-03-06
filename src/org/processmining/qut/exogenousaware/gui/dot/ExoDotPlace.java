package org.processmining.qut.exogenousaware.gui.dot;

import java.util.HashMap;

import org.processmining.plugins.graphviz.dot.DotNode;

/**
 * Default styling class for a place in a dot graph.
 * 
 * @author Adam Banham
 *
 */
public class ExoDotPlace extends DotNode {
	protected ExoDotPlace(String label) {
		super("", 
				new HashMap<String,String>() {{ 
					put("style", "filled");
					put("fillcolor", "white");
					put("shape", "circle");
					put("height", "0.25");
					put("width" , "0.25");
					put("fixedsize", "true");
					put("xlabel", label);
				}}
		);
	}
}