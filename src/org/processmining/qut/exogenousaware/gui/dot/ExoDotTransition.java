package org.processmining.qut.exogenousaware.gui.dot;

import java.util.HashMap;

import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.qut.exogenousaware.data.dot.GuardExpressionHandler;


public class ExoDotTransition extends DotNode {

	private String oldLabel;
	private String controlFlowId;
	private String transLabel;
	private GuardExpressionHandler guard;

	private String highlightLabel = "";
	
	public ExoDotTransition(String oldLabel, String controlFlowId, String transLabel, GuardExpressionHandler guard) {
		super(oldLabel, 
				new HashMap<String,String>(){{
					put("style", "filled");
					put("fontcolor", "black");
					put("fillcolor", "none");
					put("shape", "none");
					put("width" , "1");
					put("height" , "1");
					put("margin", "0");
					put("border", "0");
				}}
		
		);
		setOption("layer", "net");
		this.oldLabel = oldLabel;
		this.transLabel = transLabel;
		this.controlFlowId = controlFlowId;
		this.highlightLabel = this.makeHighlightLabel(oldLabel);
		this.guard = guard;
	}
	
	public GuardExpressionHandler getGuardExpression() {
		return this.guard;
	}
	
	public String getControlFlowId() {
		return this.controlFlowId;
	}
	
	public void setControlFlowId(String id) {
		this.controlFlowId = id;
	}
	
	public String getTransLabel() {
		return this.transLabel;
	}
	
	public void setTransLabel(String label) {
		this.highlightLabel = this.highlightLabel.replace(this.transLabel, label);
		this.oldLabel = this.oldLabel.replace(this.transLabel, label);
		this.transLabel = label;
	}
	
	public String makeHighlightLabel(String label) {
		return label.replaceFirst("BGCOLOR=\".*?\"","COLOR=\"YELLOW\" BGCOLOR=\"YELLOW\"");
	}
	
	public void highlightNode() {
		this.setLabel(this.highlightLabel);
	}
	
	public void revertHighlight() {
		this.setLabel(this.oldLabel);
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
