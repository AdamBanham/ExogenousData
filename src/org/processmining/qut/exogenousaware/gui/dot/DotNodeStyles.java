package org.processmining.qut.exogenousaware.gui.dot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringEscapeUtils;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.datapetrinets.expression.syntax.ExprRoot;
import org.processmining.datapetrinets.expression.syntax.ExpressionParser;
import org.processmining.datapetrinets.expression.syntax.SimpleNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.qut.exogenousaware.data.dot.GuardExpressionHandler;

/**
 * Static class for making exogenous dot nodes for a dot visusalisation.
 * 
 * @author Adam Banham
 *
 */
public class DotNodeStyles {
	private DotNodeStyles() {};
	
	public static DotNode buildPlaceNode(String label) {
		return new ExoDotPlace(label);
	}
	
	public static DotNode buildStartingPlaceNode(String label) {
		DotNode p = buildPlaceNode(label);
		p.setOption("fillcolor", "green");
		p.setOption("style", "filled");
		p.setOption("xlabel","START");
		return p;
	}
	
	public static DotEdge buildEdge(DotNode source, DotNode target) {
		return new ExoDotEdge(source, target);
	}
	
	public static DotNode buildEndingPlaceNode(String label) {
		DotNode p = buildPlaceNode(label);
		p.setOption("fillcolor", "red");
		p.setOption("style", "filled");
		p.setOption("xlabel","END");
		return p;
	}
	
	public static DotNode buildTauTransition(Transition t) {
		String label = createTransitionLabel(t.getLabel());
		return new ExoDotTransition(label, t.getId().toString(), t.getLabel(), new GuardExpressionHandler(null, null));
	}
	
	public static DotNode buildNoRuleTransition(Transition t) {
		String label = createTransitionLabel(t.getLabel());
		return new ExoDotTransition(label, t.getId().toString(), t.getLabel(), new GuardExpressionHandler(null, null));
	}
	
	public static DotNode buildNoRuleTransition(Transition t, Map<String,String> swapper) {
		String label = createTransitionLabel(t.getLabel());
		return new ExoDotTransition(label, t.getId().toString(), t.getLabel(), new GuardExpressionHandler(null, swapper));
	}
	
	public static DotNode buildRuleTransition(Transition t, GuardExpression g, Map<String,String> swapper) {
		String label = createTransitionLabel(t.getLabel(), g, swapper);
		return new ExoDotTransition(label,t.getId().toString(), t.getLabel(), new GuardExpressionHandler(g, swapper));
	}
	
	private static String createTransitionLabel(String label) {
		String labelFortmat = ""
				+ "<<TABLE BGCOLOR=\"%s\" BORDER=\"1\" CELLBORDER=\"0\" SCALE=\"BOTH\" CELLPADDING=\"0\" CELLSPACING=\"0\" PORT=\"HEAD\">"
				+ "<TR>"
				+ "<TD BORDER=\"0\" SCALE=\"WIDTH\" CELLPADDING=\"2\" CELLBORDER=\"0\" >"
				+ "<FONT COLOR=\"%s\">%s </FONT>"
				+ "</TD>"
				+ "</TR>"
				+ "<TR>"
				+ "<TD BGCOLOR=\"black\" HEIGHT=\"5\" SCALE=\"WIDTH\" ALIGN=\"LEFT\" BORDER=\"0\" CELLPADDING=\"2\" CELLBORDER=\"0\" CELLSPACING=\"0\"></TD>"
				+ "</TR><TR>"
				+ "<TD BGCOLOR=\"black\" HEIGHT=\"5\" SCALE=\"WIDTH\" ALIGN=\"LEFT\" BORDER=\"0\" CELLPADDING=\"2\" CELLBORDER=\"0\" CELLSPACING=\"0\"></TD>"
				+ "</TR>";
		
		String end = ""
				+ "</TABLE>>";
		String formattedlabel = String.format(labelFortmat, 
				label.toLowerCase().contains("tau ") ? "BLACK" : "WHITE",
				label.toLowerCase().contains("tau ") ? "WHITE" : "BLACK",
				label.toLowerCase().contains("tau ") ? "&tau;" : label
		);
		formattedlabel = formattedlabel + end;
		return formattedlabel;
	}
	
	private static String createTransitionLabel(String label, GuardExpression g, Map<String,String> swapper) {
		String labelFortmat = ""
				+ "<<TABLE BGCOLOR=\"%s\" BORDER=\"1\" CELLBORDER=\"0\" SCALE=\"BOTH\" CELLPADDING=\"0\" CELLSPACING=\"0\" PORT=\"HEAD\">"
				+ "<TR>"
				+ "<TD BORDER=\"0\" SCALE=\"WIDTH\" CELLPADDING=\"2\" CELLBORDER=\"0\" >"
				+ "<FONT COLOR=\"%s\">%s </FONT>"
				+ "</TD>"
				+ "</TR>"
				+ "<TR>"
				+ "<TD BGCOLOR=\"black\" HEIGHT=\"5\" SCALE=\"WIDTH\" ALIGN=\"LEFT\" BORDER=\"0\" CELLPADDING=\"2\" CELLBORDER=\"0\" CELLSPACING=\"0\"></TD>"
				+ "</TR><TR>"
				+ "<TD BGCOLOR=\"black\" HEIGHT=\"5\" SCALE=\"WIDTH\" ALIGN=\"LEFT\" BORDER=\"0\" CELLPADDING=\"2\" CELLBORDER=\"0\" CELLSPACING=\"0\"></TD>"
				+ "</TR>";
		
		String end = ""
				+ "</TABLE>>";
		String formattedlabel = String.format(labelFortmat, 
				label.toLowerCase().contains("tau ") ? "BLACK" : "WHITE",
				label.toLowerCase().contains("tau ") ? "WHITE" : "BLACK",
				label.toLowerCase().contains("tau ") ? "&tau;" : label
		);
		List<String> exprList = new ArrayList<String>();
		try {
			String expr = g.toString();
			ExprRoot root  = new ExpressionParser(g.toString()).parse();
			int curr_left = 1;
			int curr_right = 0;
			for(int i=0;i < root.jjtGetNumChildren(); i++) {
				SimpleNode node = (SimpleNode) root.jjtGetChild(i);
				if (node.jjtGetFirstToken().kind == 16) {
//					If the first conjuction is a OR, make rows
					curr_right = node.jjtGetFirstToken().beginColumn-1;
					exprList.add(formatExpression(expr.substring(curr_left, curr_right),exprList.size(), swapper));
					curr_left = node.jjtGetFirstToken().endColumn;
					exprList.add(formatExpression(expr.substring(curr_left, expr.length()-1),exprList.size(), swapper));
				} else if (node.jjtGetFirstToken().kind == 15) {
//					If the first conjuction is a AND, make a table
					List<String> tmp = new ArrayList<String>();
					curr_right = node.jjtGetFirstToken().beginColumn-1;
					tmp.add(expr.substring(curr_left, curr_right));
					curr_left = node.jjtGetFirstToken().endColumn;
					tmp.add(expr.substring(curr_left, expr.length()-1));
					exprList.add(createTableRow(tmp, exprList.size(), swapper));
					
				} else {
//					for anything else just make a row
					exprList.add(formatExpression(expr, exprList.size(), swapper));
				}

				
			}
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
		String expression = "";
		for( String element: exprList) {
			expression = expression + element;
		}
		formattedlabel = formattedlabel + expression;
		formattedlabel = formattedlabel + end;
		return formattedlabel;
	}
	
	private static String formatExpression(String expr, int key, String prefix, Map<String,String> swapper) {
		String ruleFormat = "<TR>"
				+ "<TD BGCOLOR=\"WHITE\" CELLPADDING=\"5\" ALIGN=\"LEFT\" BORDER=\"0\"><FONT COLOR=\"RED\" POINT-SIZE=\"9\">[%s%d]</FONT><FONT POINT-SIZE=\"8\" COLOR=\"BLACK\"> %s </FONT> </TD>"
				+ "</TR>";
		for (Entry<String, String> val : swapper.entrySet()) {
			expr = expr.replace(val.getValue(), val.getKey());
		}
//		escape html entities
		expr =  StringEscapeUtils.escapeHtml(expr);
		expr =  String.format(ruleFormat, prefix, key+1, expr);
		return expr;
	}
	
	private static String formatExpression(String expr, int key, Map<String,String> swapper) {
		String ruleFormat = "<TR>"
				+ "<TD BGCOLOR=\"WHITE\" CELLPADDING=\"5\" BORDER=\"1\"><FONT COLOR=\"RED\" POINT-SIZE=\"9\">[R%d]</FONT><FONT POINT-SIZE=\"8\" COLOR=\"BLACK\"> %s </FONT> </TD>"
				+ "</TR>";
		for (Entry<String, String> val : swapper.entrySet()) {
			expr = expr.replace(val.getValue(), val.getKey());
		}
//		escape html entities
		expr =  StringEscapeUtils.escapeHtml(expr);
		expr =  String.format(ruleFormat, key+1, expr);
		return expr;
	}
	
	private static String createTableRow(List<String> exprs, int key, Map<String,String> swapper) {
		List<String> rows = new ArrayList<String>();
		String tableFormat = "<TR><TD><TABLE BORDER=\"1\"><TR><TD CELLPADDING=\"5\" ALIGN=\"LEFT\" BORDER=\"0\"><FONT COLOR=\"RED\" POINT-SIZE=\"9\">[R%d]</FONT>"
				+ "<FONT POINT-SIZE=\"9\" COLOR=\"BLACK\"> - Requires the following to be true: </FONT>"
				+ "</TD></TR>";
		tableFormat = String.format(tableFormat, key+1);
		for(String expr: exprs) {
			rows.add(formatExpression(expr,rows.size(), "R"+(key+1)+"-", swapper));
		}
		for(String expr: rows) {
			tableFormat = tableFormat + expr;
		}
		return tableFormat + "</TABLE></TD></TR>";
	}

}
