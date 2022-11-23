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
					put("height", "1");
					put("width" , "1");
					put("fixedsize", "true");
					put("xlabel", label);
				}}
		);
		setOption("layer", "net");
	}
	
	@Override
	public String toString() {
		String result = "\"" + getId() + "\" [label=" + labelToString() + ", id=\"" + getId() + "\"";
		for (String key : getOptionKeySet()) {
			result += "," + key + "=" + escapeString(getOption(key));
		}
		return result + "];";
	}
}