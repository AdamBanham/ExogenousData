package org.processmining.qut.exogenousaware.gui.panels;

import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JPanel;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringEscapeUtils;
import org.deckfour.xes.model.XAttributeContinuous;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.datapetrinets.expression.syntax.ExprRoot;
import org.processmining.datapetrinets.expression.syntax.ExpressionParser;
import org.processmining.datapetrinets.expression.syntax.SimpleNode;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PNWDTransition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithData;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.Dot.GraphDirection;
import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.qut.exogenousaware.gui.ExogenousEnhancementTracablity;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;

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
public class ExogenousEnhancementDotPanel {

	
	@Setter @Default private ExogenousEnhancementTracablity source = null;
	@Getter private JPanel main;
	@Getter private DotPanel vis;
	@Getter @NonNull private PetriNetWithData graph;
	
	@Default @Setter private Map<String, GuardExpression> rules = null;
	@Default @Setter private Map<String,String> swapMap = null;
	@Default @Setter private PetriNetWithData updatedGraph = null;
	@Default private ExoDotNode selectedNode = null;
	
	public ExogenousEnhancementDotPanel setup() {
		this.main = new JPanel();
		this.main.setLayout(new GridLayout());
		this.vis = new DotPanel(new Dot());
		this.vis.changeDot(this.convertGraphToDot(this.vis), false);
		this.vis.setDirection(GraphDirection.leftRight);
		this.main.add(this.vis);
		return this;
	}
	
	public void update(ExogenousEnhancementTracablity source) {
//		reset references
		this.source = source;
		this.selectedNode = null;
//		check for needed information
		if (this.rules == null || this.swapMap == null || this.updatedGraph == null) {
			throw new NotImplementedException("[ExogenousInvestigatorDotPanel] Cannot update without any rules, swap map for variable names or new graph");
		}
		Map<Transition,Transition> transMapping = this.getSource().getFocus().getTask().getTransMap();
//		build new dot graph with rules 
		Dot update = new Dot();
		update.setOption("bgcolor", "none");
		update.setOption("rank", "min");
		List<Place> initial = new ArrayList<>();
		List<Place> end = new ArrayList<>();
		List<Object> curr = new ArrayList<>();
		List<PetrinetNode> next = new ArrayList<PetrinetNode>();
		List<Object> nextEdges = new ArrayList<>();
		List<String> seen = new ArrayList<>();
		int group =1;
		Map<String, DotNode> nodes = new HashMap<String, DotNode>();
//		add initial places first
		for (Place place : graph.getInitialMarking().stream().collect(Collectors.toList())) {
			for(Place newPlace : this.updatedGraph.getPlaces()) {
				if (newPlace.getLabel().equals(place.getLabel())) {
					initial.add(newPlace);
					curr.add(newPlace);
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
//		walk through petri net and find backwards edges
		while (curr.size() > 0) {
			next = new ArrayList<PetrinetNode>();
			nextEdges = new ArrayList<>();
			for(Object node : curr) {
//				check that we have not seen this node before
				String id = node.getClass().equals(Arc.class) ? ((Arc)node).getLocalID().toString() : ((PetrinetNode)node).getId().toString();
//				System.out.println("Comparing '"+id+"'");
				if (seen.contains(id)) {
//					System.out.println("Found Match!");
					continue;
				}
				Place p = null;
				Transition t = null;
				PetrinetEdge< ? extends PetrinetNode, ? extends PetrinetNode> arc = null;
//				handle adding element
				if (node.getClass().equals(Place.class)) {
					p = (Place) node;
					nodes.put(p.getId().toString(), buildPlaceNode(p.getLabel()));
					if (initial.contains(p)) {
						ExoDotPlace pp = (ExoDotPlace) nodes.get(p.getId().toString());
						pp.setOption("fillcolor", "green");
						pp.setOption("style", "filled");
						pp.setOption("xlabel","START");
					}
					if (end.contains(p)) {
						ExoDotPlace pp = (ExoDotPlace) nodes.get(p.getId().toString());
						pp.setOption("fillcolor", "red");
						pp.setOption("style", "filled");
						pp.setOption("xlabel","END");
					}
					nodes.get(p.getId().toString()).setOption("group", ""+group);
					update.addNode(nodes.get(p.getId().toString()));
				} else if (node.getClass().equals(PNWDTransition.class)) {
					t = (Transition) node;
					Transition oldTrans = findOldTrans(t,transMapping);
					ExoDotNode newNode;
					if (this.rules.containsKey(t.getId().toString())) {
						newNode = buildTransitionNode(t.getLabel(), oldTrans.getId().toString(),this.vis, this.rules.get(t.getId().toString()));
					} else {
						newNode = buildTransitionNode(t.getLabel(), oldTrans.getId().toString(),this.vis);
					}
					newNode.setOption("group", ""+group);
					nodes.put(t.getId().toString(), newNode);
					newNode.addMouseListener(new EnhancementListener(this, this.vis, newNode));
					update.addNode( nodes.get(t.getId().toString()) );
				} else if (node.getClass().equals(Arc.class)) {
					arc = (Arc) node;
					ExoDotEdge arcDot = new ExoDotEdge(nodes.get(arc.getSource().getId().toString()),
							nodes.get(arc.getTarget().getId().toString()) );
					if (seen.contains(arc.getTarget().getId().toString())) {
//						arcDot.setOption("dir", "back");
					}
					update.addEdge(
							arcDot
					);
				} else {
					throw new IllegalStateException("Unable to find a class type for a petrinet node:: "+ node.getClass());
				}
//				get edges with this element as source
				List<PetrinetEdge< ? extends PetrinetNode, ? extends PetrinetNode>> edges;
				if (p != null) {
					final Place pp = p;
					edges = this.updatedGraph.getEdges().stream()
						.filter(ed -> {return ed.getSource().getId().toString().equals(pp.getId().toString());})
						.collect(Collectors.toList());
//					System.out.println("Place="+pp.getLabel()+" has "+edges.size()+" arcs.");
				} else if (t != null) {
					final Transition tt = t;
					edges = this.updatedGraph.getEdges().stream()
						.filter(ed -> {return ed.getSource().getId().toString().equals(tt.getId().toString());})
						.collect(Collectors.toList());
//					System.out.println("trans="+tt.getLabel()+" has "+edges.size()+" arcs.");
				} else {
					edges = null;
				}
//				get targets for edges
				if (edges != null) {
					for(PetrinetEdge< ? extends PetrinetNode, ? extends PetrinetNode> edge: edges) {
						if (!seen.contains(edge.getTarget().getId().toString()) & !next.contains(edge.getTarget())) {
							next.add(edge.getTarget());
						}
						nextEdges.add(edge);
					}
				}
			}
			group++;
//			System.out.println("Number of new elements:: "+next.size());
//			add elements to seen
			seen.addAll(curr.stream()
					.map(node -> node.getClass().equals(Arc.class) ? ((Arc)node).getLocalID().toString() : ((PetrinetNode)node).getId().toString())
					.collect(Collectors.toList()));
//			System.out.println("Number of seen elements:: "+seen.size());
			curr = new ArrayList<Object>();
			curr.addAll(next.stream().sorted(Comparator.comparing(PetrinetNode::getLabel)).collect(Collectors.toList()));
			curr.addAll(nextEdges);
		}
//		build transitions to show rules found under variable bars
//		for(Transition oldtrans : this.graph.getTransitions()) {
////		for( Transition trans: this.updatedGraph.getTransitions()) {
//			ExoDotNode node;
//			Transition trans = transMapping.get(oldtrans);
//			if (this.rules.containsKey(trans.getId().toString())) {
//				node = buildTransitionNode(trans.getLabel(), oldtrans.getId().toString(),this.vis, this.rules.get(trans.getId().toString()));
//			} else {
//				node = buildTransitionNode(trans.getLabel(), oldtrans.getId().toString(),this.vis);
//			}
//			nodes.put(trans.getId().toString(), node);
//			node.addMouseListener(new EnhancementListener(this, this.vis, node));
//			update.addNode( nodes.get(trans.getId().toString()) );
//		}
//		regraph dot
		this.vis.changeDot(update, true);
	}
	
	public Transition findOldTrans(Transition t, Map<Transition,Transition> mapper) {
		Transition map = null;
		for (Entry<Transition, Transition> entry : mapper.entrySet()) {
			if (entry.getValue().equals(t)) {
				map = entry.getKey();
				break;
			}
		}
		return map;
	}
	
	public ExogenousEnhancementDotPanel addListeners(ExogenousEnhancementTracablity source) {
		this.setSource(source);
		if (this.source != null) {
			for(DotNode element : this.getVis().getDot().getNodes()) {
				element.addMouseListener(new EnhancementListener(this,this.getVis(),(ExoDotNode) element));
			}
			this.vis.changeDot(this.vis.getDot(), true);
		}
		return this;
	}
	
	public Dot convertGraphToDot(DotPanel panel) {
//		build new dot graph with rules 
		Dot update = new Dot();
		update.setOption("bgcolor", "none");
		update.setOption("rank", "min");
		List<Place> initial = new ArrayList<>();
		List<Place> end = new ArrayList<>();
		List<Object> curr = new ArrayList<>();
		List<PetrinetNode> next = new ArrayList<PetrinetNode>();
		List<Object> nextEdges = new ArrayList<>();
		List<String> seen = new ArrayList<>();
		int group =1;
		Map<String, DotNode> nodes = new HashMap<String, DotNode>();
//		add initial places first
		for (Place place : graph.getInitialMarking().stream().collect(Collectors.toList())) {
				initial.add(place);
				curr.add(place);
		}
		for (Place place : graph.getFinalMarkings()[0].stream().collect(Collectors.toList())) {
				end.add(place);
		}
//		walk through petri net and find backwards edges
		while (curr.size() > 0) {
			next = new ArrayList<PetrinetNode>();
			nextEdges = new ArrayList<>();
			for(Object node : curr) {
//				check that we have not seen this node before
				String id = node.getClass().equals(Arc.class) ? ((Arc)node).getLocalID().toString() : ((PetrinetNode)node).getId().toString();
//				System.out.println("Comparing '"+id+"'");
				if (seen.contains(id)) {
//					System.out.println("Found Match!");
					continue;
				}
				Place p = null;
				Transition t = null;
				PetrinetEdge< ? extends PetrinetNode, ? extends PetrinetNode> arc = null;
//				handle adding element
				if (node.getClass().equals(Place.class)) {
					p = (Place) node;
					nodes.put(p.getId().toString(), buildPlaceNode(p.getLabel()));
					if (initial.contains(p)) {
						ExoDotPlace pp = (ExoDotPlace) nodes.get(p.getId().toString());
						pp.setOption("fillcolor", "green");
						pp.setOption("style", "filled");
						pp.setOption("xlabel","START");
					}
					if (end.contains(p)) {
						ExoDotPlace pp = (ExoDotPlace) nodes.get(p.getId().toString());
						pp.setOption("fillcolor", "red");
						pp.setOption("style", "filled");
						pp.setOption("xlabel","END");
					}
					nodes.get(p.getId().toString()).setOption("group", ""+group);
					update.addNode(nodes.get(p.getId().toString()));
				} else if (node.getClass().equals(PNWDTransition.class)) {
					t = (Transition) node;
					ExoDotNode newNode;
					newNode = buildTransitionNode(t.getLabel(), t.getId().toString() ,panel);
					newNode.setOption("group", ""+group);
					nodes.put(t.getId().toString(), newNode);
					update.addNode( nodes.get(t.getId().toString()) );
				} else if (node.getClass().equals(Arc.class)) {
					arc = (Arc) node;
					ExoDotEdge arcDot = new ExoDotEdge(nodes.get(arc.getSource().getId().toString()),
							nodes.get(arc.getTarget().getId().toString()) );
					if (seen.contains(arc.getTarget().getId().toString())) {
//						arcDot.setOption("dir", "back");
					}
					update.addEdge(
							arcDot
					);
				} else {
					throw new IllegalStateException("Unable to find a class type for a petrinet node:: "+ node.getClass());
				}
//				get edges with this element as source
				List<PetrinetEdge< ? extends PetrinetNode, ? extends PetrinetNode>> edges;
				if (p != null) {
					final Place pp = p;
					edges = this.graph.getEdges().stream()
						.filter(ed -> {return ed.getSource().getId().toString().equals(pp.getId().toString());})
						.collect(Collectors.toList());
//					System.out.println("Place="+pp.getLabel()+" has "+edges.size()+" arcs.");
				} else if (t != null) {
					final Transition tt = t;
					edges = this.graph.getEdges().stream()
						.filter(ed -> {return ed.getSource().getId().toString().equals(tt.getId().toString());})
						.collect(Collectors.toList());
//					System.out.println("trans="+tt.getLabel()+" has "+edges.size()+" arcs.");
				} else {
					edges = null;
				}
//				get targets for edges
				if (edges != null) {
					for(PetrinetEdge< ? extends PetrinetNode, ? extends PetrinetNode> edge: edges) {
						if (!seen.contains(edge.getTarget().getId().toString()) & !next.contains(edge.getTarget())) {
							next.add(edge.getTarget());
						}
						nextEdges.add(edge);
					}
				}
			}
			group++;
//			System.out.println("Number of new elements:: "+next.size());
//			add elements to seen
			seen.addAll(curr.stream()
					.map(node -> node.getClass().equals(Arc.class) ? ((Arc)node).getLocalID().toString() : ((PetrinetNode)node).getId().toString())
					.collect(Collectors.toList()));
//			System.out.println("Number of seen elements:: "+seen.size());
			curr = new ArrayList<Object>();
			curr.addAll(next.stream().sorted(Comparator.comparing(PetrinetNode::getLabel)).collect(Collectors.toList()));
			curr.addAll(nextEdges);
		}
		return update;
	}
	
	private ExoDotNode buildTransitionNode(String label,String id, DotPanel panel, GuardExpression guardExpression) {
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
		return new ExoDotNode(formattedlabel,id, panel, label, new GuardExpressionHandler(guardExpression, this.swapMap));
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
	
	public ExoDotNode buildTransitionNode(String label,String id, DotPanel panel ) {
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
		return new ExoDotNode(formattedlabel,id, panel, label, new GuardExpressionHandler(null, this.swapMap));
	}
	
	public ExoDotPlace buildPlaceNode(String label) {
		return new ExoDotPlace(label);
	}
	
	public void updateSelectedNode(ExoDotNode node) {
		if (this.selectedNode == null) {
			this.selectedNode = node;
			this.selectedNode.highlightNode();
		} else if (this.selectedNode.equals(node)) {
			this.selectedNode.revertHighlight();
			this.selectedNode = null;
		} else {
			this.selectedNode.revertHighlight();
			this.selectedNode = node;
			this.selectedNode.highlightNode();
		}
//		send request to update analysis panel
		this.source.updateAnalysisPanel(this.selectedNode);
	}
	
//	Dot elements to be visualised 
//	transition
	public class ExoDotNode extends DotNode {
		
		private String transLabel;
		private String oldLabel;
		private String highlightLabel;
		private String controlFlowId;
		private GuardExpressionHandler guard;

		public ExoDotNode(String label, String id, DotPanel panel, String transLabel, GuardExpressionHandler guard) {
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
			this.oldLabel = label;
			this.transLabel = transLabel;
			this.controlFlowId = id;
			this.highlightLabel = this.makeHighlightLabel(label);
			this.guard = guard;
			//addMouseListener(new ExoTransitionListener(this, panel, this.label));
		}
		
		public GuardExpressionHandler getGuardExpression() {
			return this.guard;
		}
		
		public String getControlFlowId() {
			return this.controlFlowId;
		}
		
		public String getTransLabel() {
			return this.transLabel;
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
	
	@Data
	public class GuardExpressionHandler {
		
		private GuardExpression guardExpression;
		private Map<String,String> convertedNames;
		private double initalValue = -1.0;
		
		public GuardExpressionHandler(GuardExpression guard, Map<String,String> names) {
			this.guardExpression = guard;
			this.convertedNames = names;
		}
		
		public Boolean hasExpression() {
			return this.guardExpression != null;
		}
		
		public String getRepresentation() {
			if (this.guardExpression != null) {
				String rep = this.guardExpression.toCanonicalString();
				for(Entry<String,String> entry : this.convertedNames.entrySet()) {
					rep = rep.replace(entry.getValue(), entry.getKey());
			}
			return "Guard expression : " + rep;
			} else {
				return "Guard expression always evaluates to true";
			}
		}
		
		public Boolean evaluate(Map<String, Object> state) {
//			do swapping with encoded variables
			Map<String, Object> newState = new HashMap<String,Object>();
			Set<String> expressionVariables = this.guardExpression.getNormalVariables();
			for(Entry<String, Object> entry: state.entrySet()) {
//				get this varaible's name
				String stateVariableName = entry.getKey();
//				check if it need to have a special name instead of original
				if(this.convertedNames.containsKey(stateVariableName)) {
					stateVariableName = this.convertedNames.get(stateVariableName);
				}
//				if varaible is used in the expression add the value
				if (expressionVariables.contains(stateVariableName)) {
					double val = this.initalValue;
//					find the right class for this object
					
					if (TransformedAttribute.class.isInstance(entry.getValue())) {
						val = ((TransformedAttribute) entry.getValue()).getRealValue();
					} else if (XAttributeContinuous.class.isInstance(entry.getValue())) {
						val = ((XAttributeContinuous) entry.getValue()).getValue();
					}
//					add variable name's value to new state for evaluation
					newState.put(stateVariableName, val);
				}
			}
//			ensure that all variables are included
			for(String var: expressionVariables) {
				if (!newState.containsKey(var)) {
					newState.put(var, this.initalValue);
				}
			}
//			convert all variables to double
//			System.out.println("Evaluating :: "+ this.guardExpression.toCanonicalString());
//			System.out.println("Prime variables :: "+this.guardExpression.getNormalVariables());
//			System.out.println("Using data state of :: "+newState.toString());
//			System.out.println("Outcome :: " + this.guardExpression.evaluate(newState));
			return (Boolean) this.guardExpression.evaluate(newState);
		}
		
	}
	
	public class EnhancementListener implements MouseListener {

		private ExoDotNode element;
		private DotPanel panel; 
		private Boolean active = false;
		private ExogenousEnhancementDotPanel source;
		
		public EnhancementListener(ExogenousEnhancementDotPanel source, DotPanel panel, ExoDotNode element) {
			this.element = element;
			this.panel = panel;
			this.source = source;
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			this.source.updateSelectedNode(element);
			this.panel.changeDot(this.panel.getDot(), false);
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
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
}
