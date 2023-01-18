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
		if (recallMeasure > 0.00 && precisionMeasure > 0.00) {
			label = createTransitionLabel(istau ? buildTauStatLabel(t, stats) : buildStatLabel(t, stats), istau, g, swapper, recallMeasure, precisionMeasure);
		} else if (recallMeasure > 0.0 && precisionMeasure < 0.01) {
			label = createTransitionLabel(istau ? buildTauStatLabel(t, stats) : buildStatLabel(t, stats), istau, g, swapper, recallMeasure, true);
		} else if (recallMeasure < 0.01 && precisionMeasure > 0.00) {
			label = createTransitionLabel(istau ? buildTauStatLabel(t, stats) : buildStatLabel(t, stats), istau, g, swapper, precisionMeasure, false);
		} else {
			label = createTransitionLabel(istau ? buildTauStatLabel(t, stats) : buildStatLabel(t, stats), istau, g, swapper);
		}
		return new ExoDotTransition(label,t.getId().toString(), t.getLabel(), new GuardExpressionHandler(g, swapper));
	}
	
	private static String createTransitionLabel(String label, boolean istau) {
		String labelFortmat = ""
				+ "<<TABLE "
				+ "SIDES=\"TLR\" WIDTH=\"110\" COLUMNS=\"*\" ROWS=\"4\" BGCOLOR=\"None\""
				+ " BORDER=\"0\" style=\"rounded\" CELLPADDING=\"0\" CELLSPACING=\"0\""
				+ " PORT=\"HEAD\""
				+ ">"
				+ "<TR>"
				+ "<TD "
				+ "colspan=\"10\" WIDTH=\"110\" BORDER=\"1\" STYLE=\"ROUNDED\" "
				+ "CELLPADDING=\"2\" CELLSPACING=\"2\" BGCOLOR=\"%s\" PORT=\"TITLE\""
				+ ">"
				+ "<TABLE "
				+ "STYLE=\"ROUNDED\" BORDER=\"0\" "
				+ "><TR>"
				+ "<TD COLSPAN=\"10\" ALIGN=\"CENTER\" VALIGN=\"MIDDLE\">"
				+ "<FONT POINT-SIZE=\"18\" COLOR=\"%s\"><B>%s</B></FONT>"
				+ "</TD>"
				+ "</TR>"
				+ "<TR>"
				+ "<TD "
				+ "COLSPAN=\"10\" style=\"rounded\" BORDER=\"0\" ALIGN=\"CENTER\""
				+ ">"
				+ "<TABLE BORDER=\"0\">"
				+ "<TR>"
				+ "<TD BORDER=\"0\" ALIGN=\"CENTER\">"
				+ "<FONT COLOR=\"red\">Guard:</FONT>"
				+ "</TD></TR>";
		
		String end = "</TABLE></TD></TR></TABLE></TD></TR></TABLE>>";
		
		String formattedlabel = String.format(labelFortmat, 
				istau ? "BLACK" : "WHITE",
				istau ? "WHITE" : "BLACK",
				istau ? "&tau;"+label : label
		);
		String guard = "<TR><TD "
				+ "BGCOLOR=\"#e0ddcc\" CELLPADDING=\"5\" ALIGN=\"CENTER\" "
				+ "BORDER=\"1\" style=\"rounded\""
				+ ">"
				+ "<FONT POINT-SIZE=\"12\" COLOR=\"BLACK\">"
				+ "TRUE"
				+ "</FONT>"
				+ "</TD></TR>";
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
				+ "<<TABLE "
				+ " WIDTH=\"110\" COLUMNS=\"*\" ROWS=\"4\" BGCOLOR=\"NONE\""
				+ " BORDER=\"0\" style=\"rounded\" CELLPADDING=\"0\" CELLSPACING=\"0\""
				+ " PORT=\"HEAD\""
				+ ">"
				+ "<TR>"
				+ "<TD "
				+ "colspan=\"10\" WIDTH=\"110\" BORDER=\"1\" STYLE=\"ROUNDED\" "
				+ "CELLPADDING=\"2\" CELLSPACING=\"2\" BGCOLOR=\"%s\" PORT=\"TITLE\""
				+ ">"
				+ "<TABLE "
				+ "STYLE=\"ROUNDED\" BORDER=\"0\" "
				+ "><TR>"
				+ "<TD ALIGN=\"CENTER\" VALIGN=\"MIDDLE\" >"
				+ "<FONT POINT-SIZE=\"18\" COLOR=\"%s\"><B>%s</B></FONT>"
				+ "</TD>"
				+ "</TR>"
				+ "<TR>"
				+ "<TD "
				+ "STYLE=\"ROUNDED\" BORDER=\"0\" ALIGN=\"CENTER\" "
				+ ">"
				+ "<TABLE BORDER=\"0\">"
				+ "<TR>"
				+ "<TD BORDER=\"0\" ALIGN=\"CENTER\">"
				+ "<FONT COLOR=\"red\">Guard:</FONT>"
				+ "</TD></TR>";
		
		String conformanceBars= ""
				+ "<TR>"
					+ "<TD "
					+ "CELLPADING=\"0\" CELLSPACING=\"0\" "
					+ "BGCOLOR=\"NONE\" "
					+ "BORDER=\"0\" COLSPAN=\"10\" "
					+ ">"
						+ "<TABLE "
						+ "BORDER=\"0\" BGCOLOR=\"NONE\" "
						+ "CELLPADING=\"0\" CELLSPACING=\"0\" "
						+ ">"
							+ "<TR>"
								+ "<TD "
								+ "BGCOLOR=\"NONE\" COLSPAN=\"2\" BORDER=\"0\" "
								+ "></TD>"
								+ "<TD "
								+ "BORDER=\"0\" ALIGN=\"CENTER\" COLSPAN=\"6\" "
								+ "VALIGN=\"MIDDLE\" BGCOLOR=\"BLACK\" STYLE=\"ROUNDED\" "
								+ ">"
									+ "<TABLE BORDER=\"0\" BGCOLOR=\"NONE\" >"
										+ "<TR>"
											+ "<TD>"
											+ "</TD>"
											+ "<TD>"
												+ "<TABLE COLSPAN=\"6\">"
													+ "<TR>"
														+ "<TD BORDER=\"0\" ALIGN=\"CENTER\">"
															+ "<FONT POINT-SIZE=\"8\" COLOR=\"red\">"
																+ "(Guard-wise) Decision-Recall: %s"
															+ "</FONT>"
														+ "</TD>"
													+ "</TR>"
													+ "<TR>"
														+ "<TD "
														+ "BGCOLOR=\"%s\" "
														+ "style=\"rounded\" BORDER=\"0\" "
														+ "></TD>"
													+ "</TR>"
													+ "<TR>"
														+ "<TD BORDER=\"0\" ALIGN=\"CENTER\">"
															+ "<FONT POINT-SIZE=\"8\" COLOR=\"red\">"
																+ "(Guard-wise) Decision-Precision: %s"
															+ "</FONT>"
														+ "</TD>"
													+ "</TR>"
													+ "<TR>"
														+ "<TD BGCOLOR=\"%s\" "
														+ "style=\"rounded\" BORDER=\"0\">"
														+ "</TD>"
													+ "</TR>"
												+ "</TABLE>"
											+ "</TD>"
											+ "<TD></TD>"
										+ "</TR>"
									+ "</TABLE>"
								+ "</TD>"
								+ "<TD "
								+ "BGCOLOR=\"NONE\" COLSPAN=\"2\" BORDER=\"0\" "
								+ "></TD>"
							+ "</TR>"
						+ "</TABLE>"
					+ "</TD>"
				+ "</TR>"
				+ "</TABLE>>";
		
		String endLabel = "</TABLE></TD></TR></TABLE></TD></TR>";
		
		String precisionMeasure;
		String precisionBarColoured = "green;%.2f:gray";
		String precisionBarFullColour = "green";
		String precisionBarUnColoured = "gray";
		String precisionBar = "";
		if (precision > 0.01 && precision < 0.98) {
			precisionBar = String.format(precisionBarColoured, precision);
			precisionMeasure = String.format("%.2f%%", precision * 100.0);
		} else if (precision >= 0.98) {
			precisionBar = precisionBarFullColour;
			precisionMeasure = String.format("%.2f%%", precision * 100.0);
		} else {
			precisionBar = precisionBarUnColoured;
			precisionMeasure = "&lt; 0.01";
		}
		
		String recallMeasure;
		String recallBarColoured = "gold;%.2f:gray";
		String recallBarFullColour = "gold";
		String recallBarUnColoured = "gray";
		String recallBar = "";
		if (recall > 0.01 && recall < 0.98) {
			recallBar = String.format(recallBarColoured, recall);
			recallMeasure = String.format("%.2f%%", recall * 100.0);
		} else if (recall >= 0.98) {
			recallBar = recallBarFullColour;
			recallMeasure = String.format("%.2f%%", recall * 100.0);
		} else {
			recallBar = recallBarUnColoured;
			recallMeasure = "&lt; 0.01";
		}
		
		String formattedlabel = String.format(labelFortmat, 
				istau ? "BLACK" : "WHITE",
				istau ? "WHITE" : "BLACK",
				istau ? "&tau;"+label : label
		);
		String formattedConformanceLabel = String.format(conformanceBars, 
				recallMeasure,
				recallBar,
				precisionMeasure,
				precisionBar);
		List<String> exprList = new ArrayList<String>();
		
		if (g.isTrue()) {
			String guard = "<TR><TD "
					+ "BGCOLOR=\"#e0ddcc\" CELLPADDING=\"5\" ALIGN=\"CENTER\" "
					+ "BORDER=\"1\" style=\"rounded\""
					+ ">"
					+ "<FONT POINT-SIZE=\"12\" COLOR=\"BLACK\">"
					+ "TRUE"
					+ "</FONT>"
					+ "</TD></TR>";
			exprList.add(guard);
		} else if (g.isFalse()) {
			String guard = "<TR><TD "
					+ "BGCOLOR=\"#e0ddcc\" CELLPADDING=\"5\" ALIGN=\"CENTER\" "
					+ "BORDER=\"1\" style=\"rounded\""
					+ ">"
					+ "<FONT POINT-SIZE=\"12\" COLOR=\"BLACK\">"
					+ "FALSE"
					+ "</FONT>"
					+ "</TD></TR>";
			exprList.add(guard);
		} else {
			try {
				String expr = g.toString();
				SimpleNode root  = new ExpressionParser(g.toString()).parse();
				for(int i=0;i < root.jjtGetNumChildren(); i++) {
					SimpleNode node = (SimpleNode) root.jjtGetChild(i);
					exprList.addAll(createGuardLabel(node,expr,swapper,0));	
				}
			} catch (Exception e) {
				System.out.println("[ExogenousData-DotStyling] error occured "
						+ "while building transition guard :: " + e.getCause());
				e.printStackTrace();
			}
		}
		String expression = "";
		for( String element: exprList) {
			expression = expression + element;
		}
		formattedlabel = formattedlabel + expression;
		formattedlabel = formattedlabel + endLabel + formattedConformanceLabel;
		return formattedlabel;
	}
	
//	variables for guard label expansion 
	private static int ORKIND = 16;
	private static int ANDKIND = 15;
	private static String OREXPRBAR = "<TR>"
			+ "<TD "
			+ "WIDTH=\"15\" ROWSPAN=\"%d\" BORDER=\"1\" STYLE=\"ROUNDED\" "
			+ "ALIGN=\"CENTER\" BGCOLOR=\"#5eafcc\" VALIGN=\"MIDDLE\">"
			+ "A<BR ALIGN=\"LEFT\"/>"
			+ "N (||)<BR ALIGN=\"LEFT\"/>"
			+ "Y<BR ALIGN=\"LEFT\"/>"
			+ "</TD></TR>";
	private static String ANDEXPRBAR = "<TR>"
			+ "<TD "
			+ "WIDTH=\"15\" ROWSPAN=\"%d\" BORDER=\"1\" STYLE=\"ROUNDED\" "
			+ "ALIGN=\"CENTER\" BGCOLOR=\"#cc5e5e\" VALIGN=\"MIDDLE\">"
			+ "A<BR ALIGN=\"LEFT\"/>"
			+ "L (&amp;&amp;)<BR ALIGN=\"LEFT\"/>"
			+ "L<BR ALIGN=\"LEFT\"/>"
			+ "</TD></TR>";
	
	/**
	 * Entry Point for recursively building guard label.
	 * @return a list of formatted table rows for the guard.
	 */
	private static List<String> createGuardLabel(SimpleNode node, String expr, 
				Map<String,String> varSwapper, int ruleNumber){
		List<String> exprList = new ArrayList();
		
		if (node.jjtGetFirstToken().kind == ORKIND) {
			List<String> tempExpr = formatOrKind(node, expr, varSwapper, ruleNumber);
			exprList.add(createRowSpan(tempExpr.size()+1, ORKIND));
			exprList.addAll(tempExpr);
		} else if (node.jjtGetFirstToken().kind == ANDKIND) {
			List<String> tempExpr = formatAndKind(node, expr, varSwapper, ruleNumber);
			exprList.add(createRowSpan(tempExpr.size()+1, ANDKIND));
			exprList.addAll(tempExpr);
		} else {
			exprList.addAll(formatBase(node, expr, varSwapper, ruleNumber));
		}
		
		return exprList;
	}
	
	/**
	 * Recursive point for &&
	 * @return
	 */
	private static List<String> formatAndKind(SimpleNode node, String expr, 
			Map<String,String> varSwapper, int ruleNumber){
		List<String> exprList = new ArrayList();
//		loop through children 
		for(int i=0;i<node.jjtGetNumChildren();i++) {
			SimpleNode child = (SimpleNode) node.jjtGetChild(i);
			if (child.jjtGetFirstToken().kind == ANDKIND) {
				List temp = formatAndKind(child, expr, varSwapper, ruleNumber);
				ruleNumber += temp.size();
				exprList.addAll(temp);
			} else {
				List temp = createGuardLabel(child, expr, varSwapper, ruleNumber);
				ruleNumber += temp.size();
				exprList.addAll(temp);
			}
		}
		return exprList;
	}
	
	/**
	 * Recursive point for ||
	 * @return
	 */
	private static List<String> formatOrKind(SimpleNode node, String expr,
			Map<String,String> varSwapper, int ruleNumber){
		List<String> exprList = new ArrayList();
//		loop through children 
		for(int i=0;i<node.jjtGetNumChildren();i++) {
			SimpleNode child = (SimpleNode) node.jjtGetChild(i);
			if (child.jjtGetFirstToken().kind == ORKIND) {
				List temp = formatOrKind(child, expr, varSwapper, ruleNumber);
				ruleNumber += temp.size();
				exprList.addAll(temp);
			} else {
				List temp = createGuardLabel(child, expr, varSwapper, ruleNumber);
				ruleNumber += temp.size();
				exprList.addAll(temp);
			}
		}
		return exprList;
	}
	
	/**
	 * Base case for recursion
	 * @return
	 */
	private static List<String> formatBase(SimpleNode node, String expr, 
			Map<String,String> varSwapper, int ruleNumber){
		List<String> exprList = new ArrayList(); 
		SimpleNode firstNode = (SimpleNode) node.jjtGetChild(0);
		SimpleNode lastNode = (SimpleNode) node.jjtGetChild(node.jjtGetNumChildren()-1);
		String portion = expr.substring(
				firstNode.jjtGetFirstToken().beginColumn-1,
				lastNode.jjtGetLastToken().endColumn
		);
		exprList.add(formatExpression(portion, ruleNumber, varSwapper));
		return exprList;
	}
	
	/**
	 * Creates a column span for and's and or's.
	 * @param span
	 * @param kind
	 * @return
	 */
	private static String createRowSpan(int span, int kind) {
		if (kind == ORKIND) {
			return String.format(OREXPRBAR, span);
		} else if (kind == ANDKIND) {
			return String.format(ANDEXPRBAR, span);
		} else {
			throw new IllegalArgumentException("Unsupported kind :: "+kind);
		}
	}
	
	/**
	 * Given a portion of a transition guard, converts portion into a table row 
	 * for graphviz visualisation.
	 * @param expr : content for label
	 * @param key : rule number
	 * @param swapper : mapping from html safe variables to display names.
	 * @return
	 */
	private static String formatExpression(String expr, int key, 
			Map<String,String> swapper) {
		String ruleFormat = "<TR>"
				+ "<TD "
				+ "BGCOLOR=\"#e0ddcc\" ALIGN=\"LEFT\" STYLE=\"rounded\" "
				+ "CELLPADDING=\"5\" BORDER=\"1\">"
				+ "<FONT COLOR=\"RED\" POINT-SIZE=\"9\">"
				+ "[R%d]"
				+ "</FONT>"
				+ "<FONT POINT-SIZE=\"8\" COLOR=\"BLACK\">"
				+ " %s "
				+ "</FONT> "
				+ "</TD></TR>";
		for (Entry<String, String> val : swapper.entrySet()) {
			expr = expr.replace(val.getValue(), val.getKey());
		}
//		escape html entities
		expr =  StringEscapeUtils.escapeHtml(expr);
		expr =  String.format(ruleFormat, key+1, expr);
		return expr;
	}

}
