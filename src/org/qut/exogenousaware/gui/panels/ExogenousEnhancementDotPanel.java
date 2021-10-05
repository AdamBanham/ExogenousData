package org.qut.exogenousaware.gui.panels;

import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
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
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithData;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.Dot.GraphDirection;
import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.qut.exogenousaware.gui.ExogenousEnhancementTracablity;
import org.qut.exogenousaware.steps.transform.data.TransformedAttribute;

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
		for(Transition oldtrans : this.graph.getTransitions()) {
//		for( Transition trans: this.updatedGraph.getTransitions()) {
			ExoDotNode node;
			Transition trans = transMapping.get(oldtrans);
			if (this.rules.containsKey(trans.getId().toString())) {
				node = buildTransitionNode(trans.getLabel(), oldtrans.getId().toString(),this.vis, this.rules.get(trans.getId().toString()));
			} else {
				node = buildTransitionNode(trans.getLabel(), oldtrans.getId().toString(),this.vis);
			}
			nodes.put(trans.getId().toString(), node);
			node.addMouseListener(new EnhancementListener(this, this.vis, node));
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
			nodes.put(trans.getId().toString(), buildTransitionNode(trans.getLabel(), trans.getId().toString() ,panel));
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
