package org.qut.exogenousaware.gui.panels;

import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.JPanel;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringEscapeUtils;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.datapetrinets.expression.syntax.ExprRoot;
import org.processmining.datapetrinets.expression.syntax.ExpressionParser;
import org.processmining.datapetrinets.expression.syntax.SimpleNode;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithData;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.Dot.GraphDirection;
import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.plugins.graphviz.visualisation.DotPanel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExogenousInvestigatorDotPanel  {

	@Getter private JPanel main;
	@Getter private DotPanel vis;
	@Getter @NonNull private PetriNetWithData graph;
	
	@Default @Setter private Map<String, GuardExpression> rules = null;
	@Default @Setter private Map<String,String> swapMap = null;
	@Default @Setter private PetriNetWithData updatedGraph = null;
	
	public ExogenousInvestigatorDotPanel setup() {
		this.main = new JPanel();
		this.main.setLayout(new GridLayout());
		this.vis = new DotPanel(new Dot());
		this.vis.changeDot(this.convertGraphToDot(this.vis), false);
		this.vis.setDirection(GraphDirection.leftRight);
		this.main.add(this.vis);
		return this;
	}
	
	public Dot convertGraphToDot(DotPanel panel) {
		Dot test = new Dot();
		test.setOption("bgcolor", "none");
		test.setOption("ordering", "out");
		test.setOption("rank", "min");
		List<Place> initial = this.graph.getInitialMarking().toList();
		List<Place> end = this.graph.getFinalMarkings()[0].toList();
		List<Place> places = new ArrayList<Place>();
		Map<String, DotNode> nodes = new HashMap<String, DotNode>();
//		add initial places first
		for (Place place : initial) {
			nodes.put(place.getId().toString(), buildPlaceNode(place.getLabel()));
			DotNode ePlace = nodes.get(place.getId().toString());
			ePlace.setOption("fillcolor", "green");
			ePlace.setOption("style", "filled");
			ePlace.setOption("xlabel","START");
			test.addNode(ePlace);
			places.add(place);
			List<? extends PetrinetNode> trans = this.graph.getEdges().stream()
				.filter(ed -> ed.getSource().getId().equals(place.getId()))
				.map(ed -> ed.getTarget())
				.collect(Collectors.toList());
			System.out.println("Place= "+place.getLabel()+" has "+trans.size()+" out coming transitions");
		}
//		add end place last
		for (Place place : end) {
			nodes.put(place.getId().toString(), buildPlaceNode(place.getLabel()));
			DotNode ePlace = nodes.get(place.getId().toString());
			ePlace.setOption("fillcolor", "red");
			ePlace.setOption("style", "filled");
			ePlace.setOption("xlabel","END");
			test.addNode(ePlace);
			places.add(place);
		}
		for( Transition trans: this.graph.getTransitions()) {
			nodes.put(trans.getId().toString(), buildTransitionNode(trans.getLabel(),panel));
			test.addNode( nodes.get(trans.getId().toString()) );
		}
		for ( Place place : this.graph.getPlaces()) {
			if (!initial.contains(place) & !end.contains(place)) {
				nodes.put(place.getId().toString(), buildPlaceNode(place.getLabel()));
				test.addNode(nodes.get(place.getId().toString()));
			}
		}

		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> arc : this.graph.getEdges()) {
			test.addEdge(
					new ExoDotEdge(nodes.get(arc.getSource().getId().toString()),
					nodes.get(arc.getTarget().getId().toString()) )
			);
		}
		return test;
	}
	
	public void update() {
		if (this.rules == null || this.swapMap == null || this.updatedGraph == null) {
			throw new NotImplementedException("[ExogenousInvestigatorDotPanel] Cannot update without any rules, swap map for variable names or new graph");
		}
//		build new dot graph with rules 
		Dot update = new Dot();
		update.setOption("bgcolor", "none");
		update.setOption("ordering", "out");
		update.setOption("rank", "min");
		List<Place> initial = new ArrayList<>();
		List<Place> end = new ArrayList<>();
		Map<String, DotNode> nodes = new HashMap<String, DotNode>();
//		add initial places first
		for (Place place : graph.getInitialMarking().stream().collect(Collectors.toList())) {
			for(Place newPlace : this.updatedGraph.getPlaces()) {
				if (newPlace.getLabel().equals(place.getLabel())) {
					nodes.put(newPlace.getId().toString(), buildPlaceNode(newPlace.getLabel()));
					DotNode ePlace = nodes.get(newPlace.getId().toString());
					ePlace.setOption("fillcolor", "green");
					ePlace.setOption("style", "filled");
					ePlace.setOption("xlabel","START");
					update.addNode(nodes.get(newPlace.getId().toString()));
					initial.add(newPlace);
					break;
				}
			}
		}
		for (Place place : graph.getFinalMarkings()[0].stream().collect(Collectors.toList())) {
			for(Place newPlace : this.updatedGraph.getPlaces()) {
				if (newPlace.getLabel().equals(place.getLabel())) {
					end.add(newPlace);
				}
			}
		}
//		build transitions to show rules found under variable bars
		for( Transition trans: this.updatedGraph.getTransitions()) {
			ExoDotNode node;
			if (this.rules.containsKey(trans.getId().toString())) {
				node = buildTransitionNode(trans.getLabel(),this.vis, this.rules.get(trans.getId().toString()));
			} else {
				node = buildTransitionNode(trans.getLabel(),this.vis);
			}
			nodes.put(trans.getId().toString(), node);
			update.addNode( nodes.get(trans.getId().toString()) );
		}
//		places and edges are built the same 
//		#TODO highligh arcs with blue green as in the original paper
		for ( Place place : this.updatedGraph.getPlaces()) {
			if (!initial.contains(place) & !end.contains(place)) {
				nodes.put(place.getId().toString(), buildPlaceNode(place.getLabel()));
				update.addNode(nodes.get(place.getId().toString()));
			}
		}
//		add end place last
		for (Place place : end) {
			for(Place newPlace : this.updatedGraph.getPlaces()) {
				if (newPlace.getLabel().equals(place.getLabel())) {
					nodes.put(newPlace.getId().toString(), buildPlaceNode(newPlace.getLabel()));
					DotNode ePlace = nodes.get(newPlace.getId().toString());
					ePlace.setOption("fillcolor", "red");
					ePlace.setOption("style", "filled");
					ePlace.setOption("xlabel","END");
					update.addNode(nodes.get(newPlace.getId().toString()));
					break;
				}
			}
		}
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> arc : this.updatedGraph.getEdges()) {
			update.addEdge(
					new ExoDotEdge(nodes.get(arc.getSource().getId().toString()),
					nodes.get(arc.getTarget().getId().toString()) )
			);
		}
//		regraph dot
		this.vis.changeDot(update, true);
	}
	
	private ExoDotNode buildTransitionNode(String label, DotPanel panel, GuardExpression guardExpression) {
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
		String ruleFormat = "<TR>"
				+ "<TD BGCOLOR=\"WHITE\" CELLPADDING=\"5\"> <FONT COLOR=\"BLACK\"> %s </FONT> </TD>"
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
			String expr = guardExpression.toString();
			ExprRoot root  = new ExpressionParser(guardExpression.toString()).parse();
			int curr_left = 1;
			int curr_right = 0;
			for(int i=0;i < root.jjtGetNumChildren(); i++) {
				SimpleNode node = (SimpleNode) root.jjtGetChild(i);
				if (node.jjtGetFirstToken().kind == 16) {
//					If the first conjuction is a OR, make rows
					curr_right = node.jjtGetFirstToken().beginColumn-1;
					exprList.add(this.formatExpression(expr.substring(curr_left, curr_right),exprList.size()));
					curr_left = node.jjtGetFirstToken().endColumn;
					exprList.add(this.formatExpression(expr.substring(curr_left, expr.length()-1),exprList.size()));
				} else if (node.jjtGetFirstToken().kind == 15) {
//					If the first conjuction is a AND, make a table
					List<String> tmp = new ArrayList<String>();
					curr_right = node.jjtGetFirstToken().beginColumn-1;
					tmp.add(expr.substring(curr_left, curr_right));
					curr_left = node.jjtGetFirstToken().endColumn;
					tmp.add(expr.substring(curr_left, expr.length()-1));
					exprList.add(this.createTableRow(tmp, exprList.size()));
					
				} else {
//					for anything else just make a row
					exprList.add(this.formatExpression(expr, exprList.size()));
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
		return new ExoDotNode(formattedlabel, panel, label);
	}
	
	public String createTableRow(List<String> exprs, int key) {
		List<String> rows = new ArrayList<String>();
		String tableFormat = "<TR><TD><TABLE BORDER=\"1\"><TR><TD CELLPADDING=\"5\" ALIGN=\"LEFT\" BORDER=\"0\"><FONT COLOR=\"RED\" POINT-SIZE=\"9\">[R%d]</FONT>"
				+ "<FONT POINT-SIZE=\"9\" COLOR=\"BLACK\"> - Requires the following to be true: </FONT>"
				+ "</TD></TR>";
		tableFormat = String.format(tableFormat, key+1);
		for(String expr: exprs) {
			rows.add(this.formatExpression(expr,rows.size(), "R"+(key+1)+"-"));
		}
		for(String expr: rows) {
			tableFormat = tableFormat + expr;
		}
		return tableFormat + "</TABLE></TD></TR>";
	}
	
	public String formatExpression(String expr, int key, String prefix) {
		String ruleFormat = "<TR>"
				+ "<TD BGCOLOR=\"WHITE\" CELLPADDING=\"5\" ALIGN=\"LEFT\" BORDER=\"0\"><FONT COLOR=\"RED\" POINT-SIZE=\"9\">[%s%d]</FONT><FONT POINT-SIZE=\"8\" COLOR=\"BLACK\"> %s </FONT> </TD>"
				+ "</TR>";
		for (Entry<String, String> val : this.swapMap.entrySet()) {
			expr = expr.replace(val.getValue(), val.getKey());
		}
//		escape html entities
		expr =  StringEscapeUtils.escapeHtml(expr);
		expr =  String.format(ruleFormat, prefix, key+1, expr);
		return expr;
	}
	 
	public String formatExpression(String expr, int key) {
		String ruleFormat = "<TR>"
				+ "<TD BGCOLOR=\"WHITE\" CELLPADDING=\"5\" BORDER=\"1\"><FONT COLOR=\"RED\" POINT-SIZE=\"9\">[R%d]</FONT><FONT POINT-SIZE=\"8\" COLOR=\"BLACK\"> %s </FONT> </TD>"
				+ "</TR>";
		for (Entry<String, String> val : this.swapMap.entrySet()) {
			expr = expr.replace(val.getValue(), val.getKey());
		}
//		escape html entities
		expr =  StringEscapeUtils.escapeHtml(expr);
		expr =  String.format(ruleFormat, key+1, expr);
		return expr;
	}
	
	public ExoDotNode buildTransitionNode(String label, DotPanel panel ) {
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
		return new ExoDotNode(formattedlabel, panel, label);
	}
	
	public ExoDotPlace buildPlaceNode(String label) {
		return new ExoDotPlace(label);
	}
	
	
//	Dot elements to be visualised 
//	transition
	public class ExoDotNode extends DotNode {
		
		private String label;

		public ExoDotNode(String label, DotPanel panel, String transLabel) {
			super(label, 
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
			this.label = transLabel;
			//addMouseListener(new ExoTransitionListener(this, panel, this.label));
		}
	}
	
//	arcs
	public class ExoDotEdge extends DotEdge {

		public ExoDotEdge(DotNode source, DotNode target) {
			super(source, target);
			if (source.getClass().equals(ExoDotNode.class)) {
				setOption("tailport", "HEAD");
			}
			if (target.getClass().equals(ExoDotNode.class)) {
				setOption("headport", "HEAD");
			}
		}
		
	}
	
//	places
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
	
//	listener for transition
	public class ExoTransitionListener implements MouseListener {
		
		private DotNode node;
		private DotPanel panel;
		private String transName;
		
		public ExoTransitionListener(ExoDotNode node, DotPanel panel, String label) {
			this.node = node;
			this.panel = panel;
			this.transName = label;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
	
		@Override
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
	
		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
	
		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
//			this.node.setOption("fillcolor", "blue;0.25:black");
			System.out.println("Hovering on "+ this.transName);
			this.panel.changeDot(this.panel.getDot(), false);
		}
	
		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
//			this.node.setOption("fillcolor", "blue;0.05:black");
			System.out.println("Hover ended on "+ this.transName);
			this.panel.changeDot(this.panel.getDot(), false);
		}
		
	}
	
}
