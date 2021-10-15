package org.processmining.qut.exogenousaware.gui.panels;

import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JPanel;

import org.apache.commons.lang.NotImplementedException;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithData;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.Dot.GraphDirection;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.qut.exogenousaware.gui.ExogenousEnhancementTracablity;
import org.processmining.qut.exogenousaware.gui.dot.DotGraphVisualisation;
import org.processmining.qut.exogenousaware.gui.dot.ExoDotTransition;

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
	
	@Default @Setter @Getter private Map<String, GuardExpression> rules = null;
	@Default @Setter @Getter private Map<String,String> swapMap = null;
	@Default @Setter @Getter private PetriNetWithData updatedGraph = null;
	@Default private ExoDotTransition selectedNode = null;
	
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
		DotGraphVisualisation handler = 
				DotGraphVisualisation.builder()
				.graph(this.graph)
				.updatedGraph(this.updatedGraph)
				.swapMap(this.swapMap)
				.transMapping(transMapping)
				.rules(this.rules)
				.build()
				.make();
		Dot update = handler
				.getVisualisation();
//		add mouse listeners to tranistions
		for(ExoDotTransition node: handler.getTransitions()) {
			node.addMouseListener(new EnhancementListener(this, this.vis, node));
		}
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
	
	public Dot convertGraphToDot(DotPanel panel) {
//		build new dot graph with rules 
		Dot update = DotGraphVisualisation.builder()
				.graph(this.graph)
				.swapMap(this.swapMap)
				.rules(this.rules)
				.build()
				.make()
				.getVisualisation();
		return update;
	}
	
	public void updateSelectedNode(ExoDotTransition node) {
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
	
	public class EnhancementListener implements MouseListener {

		private ExoDotTransition element;
		private DotPanel panel; 
		private Boolean active = false;
		private ExogenousEnhancementDotPanel source;
		
		public EnhancementListener(ExogenousEnhancementDotPanel source, DotPanel panel, ExoDotTransition element) {
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
