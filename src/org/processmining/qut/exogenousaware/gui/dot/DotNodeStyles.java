package org.processmining.qut.exogenousaware.gui.dot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringEscapeUtils;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.datapetrinets.expression.syntax.ExpressionParser;
import org.processmining.datapetrinets.expression.syntax.SimpleNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.qut.exogenousaware.data.dot.GuardExpressionHandler;
import org.processmining.qut.exogenousaware.stats.models.ProcessModelStatistics;
import org.processmining.qut.exogenousaware.stats.models.ProcessModelStatistics.DecisionPoint;

/**
 * Static class for making exogenous dot nodes for a dot visusalisation.
 * 
 * @author Adam Banham
 *
 */
public class DotNodeStyles {
	private DotNodeStyles() {};
	
	private static String TransitionInfoParams = " (%d) ";
	private static String TransitionInfoFreqParams = " (%d / %.1f%%) ";
	
	public static DotNode buildPlaceNode(String label) {
		return new ExoDotPlace(label);
	}
	
	public static DotNode buildStartingPlaceNode(String label) {
		DotNode p = buildPlaceNode(label);
		p.setOption("fillcolor", "green");
		p.setOption("style", "filled");
		p.setOption("xlabel","START");
		p.setOption("width", "1.0");
		return p;
	}
	
	public static DotEdge buildEdge(DotNode source, DotNode target) {
		return new ExoDotEdge(source, target);
	}
	
	public static DotEdge buildBackwardsEdge(DotNode source, DotNode target) {
		return new BackwardDotEdge(source, target);
	}
	
	public static DotNode buildEndingPlaceNode(String label) {
		DotNode p = buildPlaceNode(label);
		p.setOption("fillcolor", "red");
		p.setOption("style", "filled");
		p.setOption("xlabel","END");
		p.setOption("width", "1.0");
		return p;
	}
	
	public static DotNode buildDecisionCluster(String label, List<DotNode> members, int group) {
		return DecisionCluster.builder().label(label).members(members).group(group).build().setup();
	}
	
	private static String buildStatLabel(Transition t, ProcessModelStatistics stats) {
		int totalObs = 0;
		float relativeFreq = 0.0f;
		boolean relative = false;
		String label = t.getLabel();
		
		totalObs = stats.getObservations(t);
		
		for(Place dplace: stats.getDecisionMoments()) {
			if (stats.isOutcome(t, dplace)) {
				relativeFreq = stats.getInformation(dplace).getMapToFrequency().get(t);
				relative = true;
				break;
			}
		}
		
		if (relative) {
			label += String.format(TransitionInfoFreqParams, totalObs, (relativeFreq*100f));
		} else {
			label += String.format(TransitionInfoParams, totalObs);
		}
		
		return label;
	}
	
	private static String buildTauStatLabel(Transition t, ProcessModelStatistics stats) {
		int totalObs = 0;
		float relativeFreq = 0.0f;
		boolean relative = false;
		String label = "";
		
		totalObs = stats.getObservations(t);
		
		for(Place dplace: stats.getDecisionMoments()) {
			if (stats.isOutcome(t, dplace)) {
				relativeFreq = stats.getInformation(dplace).getMapToFrequency().get(t);
				relative = true;
				break;
			}
		}
		
		if (relative) {
			label += String.format(TransitionInfoFreqParams, totalObs, (relativeFreq*100f));
		} else {
			label += String.format(TransitionInfoParams, totalObs);
		}
		
		return label;
	}
	
	public static DotNode buildTauTransition(Transition t) {
		String label = createTransitionLabel("", true);
		return new ExoDotTransition(label, t.getId().toString(), t.getLabel(), new GuardExpressionHandler(null, null));
	}
	
	public static DotNode buildTauTransition(Transition t, ProcessModelStatistics stats) {
		String label = createTransitionLabel(buildTauStatLabel(t, stats), true);
		return new ExoDotTransition(label, t.getId().toString(), t.getLabel(), new GuardExpressionHandler(null, null));
	}
	
	public static DotNode buildNoRuleTransition(Transition t) {
		boolean istau = t.getLabel().startsWith("tau ");
		String label = createTransitionLabel(istau ? "" : t.getLabel(), istau);
		return new ExoDotTransition(label, t.getId().toString(), t.getLabel(), new GuardExpressionHandler(null, null));
	}
	
	public static DotNode buildNoRuleTransition(Transition t, ProcessModelStatistics stats) {
		boolean istau = t.getLabel().startsWith("tau ");
		String label = createTransitionLabel(istau ? buildTauStatLabel(t, stats) : buildStatLabel(t, stats), istau);
		return new ExoDotTransition(label, t.getId().toString(), t.getLabel(), new GuardExpressionHandler(null, null));
	}
	
	public static DotNode buildNoRuleTransition(Transition t, Map<String,String> swapper) {
		boolean istau = t.getLabel().startsWith("tau ");
		String label = createTransitionLabel(istau ? "" : t.getLabel(), istau );
		return new ExoDotTransition(label, t.getId().toString(), t.getLabel(), new GuardExpressionHandler(null, swapper));
	}
	
	public static DotNode buildNoRuleTransition(Transition t, ProcessModelStatistics stats, Map<String,String> swapper) {
		boolean istau = t.getLabel().startsWith("tau ");
		String label = createTransitionLabel(istau ? buildTauStatLabel(t, stats) : buildStatLabel(t, stats), istau);
		return new ExoDotTransition(label, t.getId().toString(), t.getLabel(), new GuardExpressionHandler(null, swapper));
	}
	
	public static DotNode buildRuleTransition(Transition t, GuardExpression g, Map<String,String> swapper) {
		boolean istau = t.getLabel().startsWith("tau ");
		System.out.println("Not Deciding if we need recall...");
		String label = createTransitionLabel(istau ? "" : t.getLabel(), istau, g, swapper);
		return new ExoDotTransition(label,t.getId().toString(), t.getLabel(), new GuardExpressionHandler(g, swapper));
	}
	
	public static DotNode buildRuleTransition(Transition t, ProcessModelStatistics stats, GuardExpression g, Map<String,String> swapper) {
		boolean istau = t.getLabel().startsWith("tau ");
//		work out if we have a guard wise recall measurement
		String recallKey = t.getId().toString()+"-recall";
		String precisionKey = t.getId().toString()+"-precision";
		DecisionPoint dp = stats.findDecision(t);
		double recallMeasure = 0.0;
		double precisionMeasure = 0.0;
		if (dp != null) {
			if (dp.getMapToMeasures().containsKey(recallKey)) {
				recallMeasure = dp.getMapToMeasures().get(recallKey);
			}
			if (dp.getMapToMeasures().containsKey(precisionKey)) {
				precisionMeasure = dp.getMapToMeasures().get(precisionKey);
			}
		}
//		call the big function if recall is set
		String label;
		if (recallMeasure > 0.0 && precisionMeasure < 0.01) {
			label = createTransitionLabel(istau ? buildTauStatLabel(t, stats) : buildStatLabel(t, stats), istau, g, swapper, recallMeasure, true);
		} 
		else if (recallMeasure < 0.01 && precisionMeasure > 0.00) {
			label = createTransitionLabel(istau ? buildTauStatLabel(t, stats) : buildStatLabel(t, stats), istau, g, swapper, precisionMeasure, false);
		}	
		else if (recallMeasure > 0.00 && precisionMeasure > 0.00) {
			label = createTransitionLabel(istau ? buildTauStatLabel(t, stats) : buildStatLabel(t, stats), istau, g, swapper, recallMeasure, precisionMeasure);
		}
		else {
			label = createTransitionLabel(istau ? buildTauStatLabel(t, stats) : buildStatLabel(t, stats), istau, g, swapper);
		}
		return new ExoDotTransition(label,t.getId().toString(), t.getLabel(), new GuardExpressionHandler(g, swapper));
	}
	
	private static String createTransitionLabel(String label, boolean istau) {
		String labelFortmat = ""
				+ "<<TABLE WIDTH=\"110\" COLUMNS=\"10\" BGCOLOR=\"%s\" BORDER=\"1\"  style=\"rounded\"  CELLPADDING=\"5\" CELLSPACING=\"5\" PORT=\"HEAD\">"
				+ "<TR>"
				+ "<TD colspan=\"10\" BORDER=\"0\" CELLPADDING=\"2\" ALIGN=\"CENTER\">"
				+ "<FONT COLOR=\"%s\">%s </FONT>"
				+ "</TD>"
				+ "</TR>"
				+ "<TR>"
				+ "<TD colspan=\"10\" BORDER=\"0\" HEIGHT=\"18\" ALIGN=\"CENTER\" VALIGN=\"MIDDLE\"><FONT COLOR=\"red\">(Guard-wise) Reasoning-Recall:</FONT></TD>"
				+ "</TR><TR>"
				+ "<TD colspan=\"10\" HEIGHT=\"10\" WIDTH=\"110\" BGCOLOR=\"gray\" style=\"rounded\"  ALIGN=\"LEFT\" BORDER=\"0\" CELLPADDING=\"2\"   CELLSPACING=\"0\"></TD>"
				+ "</TR><TR>"
				+ "<TD colspan=\"10\" BORDER=\"0\" HEIGHT=\"18\" ALIGN=\"CENTER\"><FONT COLOR=\"red\">(Guard-wise) Reasoning-Precision:</FONT></TD>"
				+ "</TR><TR>"
				+ "<TD colspan=\"10\" HEIGHT=\"10\" WIDTH=\"110\" BGCOLOR=\"gray\" style=\"rounded\"  ALIGN=\"LEFT\" BORDER=\"0\" CELLPADDING=\"2\"   CELLSPACING=\"0\"></TD>"
				+ "</TR>"
				+ "<TR><TD colspan=\"10\" style=\"rounded\" BGCOLOR=\"WHITE\"><TABLE BORDER=\"0\"><TR><TD BORDER=\"0\"><FONT COLOR=\"red\">Guard:</FONT></TD></TR>";
		
		String end = ""
				+ "</TABLE></TD></TR></TABLE>>";
		String formattedlabel = String.format(labelFortmat, 
				istau ? "BLACK" : "WHITE",
				istau ? "WHITE" : "BLACK",
				istau ? "&tau;"+label : label
		);
		String guard = "<TR><TD BGCOLOR=\"WHITE\" CELLPADDING=\"5\" ALIGN=\"CENTER\" BORDER=\"0\" style=\"rounded\"><FONT POINT-SIZE=\"8\" COLOR=\"BLACK\">TRUE</FONT></TD></TR>";
		formattedlabel = formattedlabel + guard + end;
		return formattedlabel;
	}
	
	private static String createTransitionLabel(String label, boolean istau, GuardExpression g, Map<String,String> swapper) {
		return createTransitionLabel(label, istau, g, swapper, 0.00f, 0.00f);
	}
	
	private static String createTransitionLabel(String label, boolean istau, GuardExpression g, Map<String,String> swapper, double measure, boolean isrecall) {
		if (isrecall) {
			return createTransitionLabel(label, istau, g, swapper, measure, 0.01f);
		}
		return createTransitionLabel(label, istau, g, swapper, 0.01f, measure);
	}

	private static String createTransitionLabel(String label, boolean istau, GuardExpression g, Map<String,String> swapper, double recall, double precision) {
		String labelFortmat = ""
				+ "<<TABLE WIDTH=\"110\" COLUMNS=\"*\" ROWS=\"4\" BGCOLOR=\"%s\" BORDER=\"1\" style=\"rounded\" CELLPADDING=\"5\" CELLSPACING=\"5\" PORT=\"HEAD\">"
				+ "<TR>"
				+ "<TD colspan=\"10\" WIDTH=\"110\" BORDER=\"0\" CELLPADDING=\"2\" ALIGN=\"CENTER\" VALIGN=\"MIDDLE\">"
				+ "<FONT COLOR=\"%s\">%s</FONT>"
				+ "</TD>"
				+ "</TR>"
				+ "<TR>"
				+ "<TD colspan=\"10\" BORDER=\"0\" HEIGHT=\"18\" ALIGN=\"CENTER\" VALIGN=\"MIDDLE\"><FONT COLOR=\"red\">(Guard-wise) Reasoning-Recall: %s</FONT></TD>"
				+ "</TR><TR>"
				+ "<TD colspan=\"10\" HEIGHT=\"10\" WIDTH=\"110\" BGCOLOR=\"%s\" style=\"rounded\"  ALIGN=\"LEFT\" BORDER=\"0\" CELLPADDING=\"2\"   CELLSPACING=\"0\"></TD>"
				+ "</TR><TR>"
				+ "<TD colspan=\"10\" BORDER=\"0\" HEIGHT=\"18\" ALIGN=\"CENTER\"><FONT COLOR=\"red\">(Guard-wise) Reasoning-Precision: %s</FONT></TD>"
				+ "</TR><TR>"
				+ "<TD colspan=\"10\" HEIGHT=\"10\" WIDTH=\"110\" BGCOLOR=\"%s\" style=\"rounded\"  ALIGN=\"LEFT\" BORDER=\"0\" CELLPADDING=\"2\"   CELLSPACING=\"0\"></TD>"
				+ "</TR>"
				+ "<TR><TD COLSPAN=\"10\" style=\"rounded\" BGCOLOR=\"WHITE\"><TABLE BORDER=\"0\"><TR><TD BORDER=\"0\"><FONT COLOR=\"red\">Guard:</FONT></TD></TR>";
		
		String end = ""
				+ "</TABLE></TD></TR></TABLE>>";
		
		String precisionMeasure;
		String precisionBarColoured = "green;%.2f:gray";
		String precisionBarUnColoured = "gray";
		String precisionBar = "";
		if (precision > 0.01) {
			precisionBar = String.format(precisionBarColoured, precision);
			precisionMeasure = String.format("%.2f%%", precision * 100.0);
		} else {
			precisionBar = precisionBarUnColoured;
			precisionMeasure = "";
		}
		
		String recallMeasure;
		String recallBarColoured = "gold;%.2f:gray";
		String recallBarUnColoured = "gray";
		String recallBar = "";
		if (recall > 0.01) {
			recallBar = String.format(recallBarColoured, recall);
			recallMeasure = String.format("%.2f%%", recall * 100.0);
		} else {
			recallBar = recallBarUnColoured;
			recallMeasure = "";
		}
		
		String formattedlabel = String.format(labelFortmat, 
				istau ? "BLACK" : "WHITE",
				istau ? "WHITE" : "BLACK",
				istau ? "&tau;"+label : label,
				recallMeasure,
				recallBar,
				precisionMeasure,
				precisionBar
		);
		List<String> exprList = new ArrayList<String>();
		
		String anyExprBar = "<TR><TD WIDTH=\"15\" ROWSPAN=\"3\" BORDER=\"1\"  STYLE=\"ROUNDED\" ALIGN=\"CENTER\" VALIGN=\"MIDDLE\">A<BR ALIGN=\"LEFT\"/>N (||)<BR ALIGN=\"LEFT\"/>Y<BR ALIGN=\"LEFT\"/></TD></TR>";
		String allExprBar = "<TR><TD WIDTH=\"15\" ROWSPAN=\"%d\" BORDER=\"1\"  STYLE=\"ROUNDED\" ALIGN=\"CENTER\" VALIGN=\"MIDDLE\">A<BR ALIGN=\"LEFT\"/>L (&amp;&amp;)<BR ALIGN=\"LEFT\"/>L<BR ALIGN=\"LEFT\"/></TD></TR>";
		try {
			String expr = g.toString();
			SimpleNode root  = new ExpressionParser(g.toString()).parse();
			int curr_left = 1;
			int curr_right = 0;
			System.out.println("number of root nodes :: "+ root.jjtGetNumChildren());
			for(int i=0;i < root.jjtGetNumChildren(); i++) {
				SimpleNode node = (SimpleNode) root.jjtGetChild(i);
				System.out.println("number of children :: "+ node.jjtGetNumChildren());
				if (node.jjtGetFirstToken().kind == 16) {
					exprList.add(anyExprBar);
//					If the first conjuction is a OR, make rows
					curr_right = node.jjtGetFirstToken().beginColumn-1;
					exprList.add(formatExpression(expr.substring(curr_left, curr_right),exprList.size(), swapper));
					curr_left = node.jjtGetFirstToken().endColumn;
					exprList.add(formatExpression(expr.substring(curr_left, expr.length()-1),exprList.size(), swapper));
				} else if (node.jjtGetFirstToken().kind == 15) {
//					If the first conjuction is a AND, make a table
					List<String> tmp = new ArrayList<String>();
					List<String> nextRun = new ArrayList<String>();
//					flatten all && into a single group 
					List<SimpleNode> possibleJoins = new ArrayList<>();
					System.out.println("Given :: "+expr.substring(root.jjtGetFirstToken().beginColumn, root.jjtGetLastToken().endColumn));
					for(int j=0; j < node.jjtGetNumChildren(); j++) {
						SimpleNode child = (SimpleNode)node.jjtGetChild(j);
						String subexpr = expr.substring(curr_left, child.jjtGetLastToken().endColumn);
						System.out.println("starting with :: " + subexpr);
						possibleJoins.add(child);
						curr_left = child.jjtGetLastToken().endColumn;
					}
					do {
						System.out.println("starting flatenning");
						tmp.clear();
						tmp.addAll(nextRun);
						nextRun.clear();						
						List<SimpleNode> nextJoins = new ArrayList<>();
						for (SimpleNode proot : possibleJoins) {
							SimpleNode child = (SimpleNode) proot.jjtGetChild(0);
							System.out.println("possible child :: "+child.jjtGetNumChildren());
							if (proot.jjtGetFirstToken().kind == 15) {
								nextJoins.add((SimpleNode) proot.jjtGetChild(0));
								nextJoins.add((SimpleNode) proot.jjtGetChild(1));
							}
							else {
								nextJoins.add(proot);
							}
						}
						for (SimpleNode children: nextJoins) {
							String subexpr;
							if (children.jjtGetFirstToken().kind != 15) {
								String left = ((SimpleNode)children.jjtGetChild(0)).jjtGetValue().toString(); 
								String mid = children.jjtGetFirstToken().image; 
								String right = ((SimpleNode)children.jjtGetChild(1)).jjtGetValue().toString(); ;
								subexpr = left + mid +right;
							} else {
								subexpr = "still building";
							}
							System.out.println("Adding :: "+subexpr);
							nextRun.add(subexpr);
						}
						possibleJoins.clear();
						possibleJoins.addAll(nextJoins);
					} while ( tmp.size() != nextRun.size());
					
					nextRun.clear();
					for (String child : tmp) {
						nextRun.add(formatExpression(child,nextRun.size()+exprList.size(),swapper));
					}
					exprList.add(String.format(allExprBar, nextRun.size()+1));
					for (String subexpr : nextRun) {
						exprList.add(subexpr);
					}
//					old table formatting
//					curr_right = node.jjtGetFirstToken().beginColumn-1;
//					tmp.add(expr.substring(curr_left, curr_right));
//					curr_left = node.jjtGetFirstToken().endColumn;
//					tmp.add(expr.substring(curr_left, expr.length()-1));
//					exprList.add(createTableRow(tmp, exprList.size(), swapper));
					
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
		String ruleFormat = "<TR><TD BGCOLOR=\"WHITE\" CELLPADDING=\"5\" ALIGN=\"LEFT\" BORDER=\"0\" style=\"rounded\"><FONT COLOR=\"RED\" POINT-SIZE=\"9\">[%s%d]</FONT><FONT POINT-SIZE=\"8\" COLOR=\"BLACK\"> %s </FONT> </TD></TR>";
		for (Entry<String, String> val : swapper.entrySet()) {
			expr = expr.replace(val.getValue(), val.getKey());
		}
//		escape html entities
		expr =  StringEscapeUtils.escapeHtml(expr);
		expr =  String.format(ruleFormat, prefix, key+1, expr);
		return expr;
	}
	
	private static String formatExpression(String expr, int key, Map<String,String> swapper) {
		String ruleFormat = "<TR><TD BGCOLOR=\"WHITE\" ALIGN=\"LEFT\" STYLE=\"rounded\" CELLPADDING=\"5\" BORDER=\"1\"><FONT COLOR=\"RED\" POINT-SIZE=\"9\">[R%d]</FONT><FONT POINT-SIZE=\"8\" COLOR=\"BLACK\"> %s </FONT> </TD></TR>";
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
		String tableFormat = "<TR><TD style=\"rounded\"  CELLPADDING=\"1\" BORDER=\"1\"><TABLE BORDER=\"0\"><TR><TD CELLPADDING=\"5\" ALIGN=\"LEFT\" BORDER=\"0\"><FONT COLOR=\"RED\" POINT-SIZE=\"9\">[R%d]</FONT>"
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
