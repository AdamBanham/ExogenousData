package org.processmining.qut.exogenousaware.gui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.processmining.framework.util.ui.widgets.ProMList;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.Dot.GraphDirection;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.qut.exogenousaware.gui.ExogenousDiscoveryInvestigator;
import org.processmining.qut.exogenousaware.gui.dot.DotGraphVisualisation;
import org.processmining.qut.exogenousaware.gui.dot.ExoDotTransition;

import com.kitfox.svg.SVGElement;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import prefuse.data.query.ListModel;

@Builder
public class ExogenousEnhancementGraphInvestigator {

	@NonNull private ExogenousEnhancementAnalysis source;
	@NonNull private ExogenousDiscoveryInvestigator controller;
	
	@Default @Getter private JPanel main = new JPanel();
	@Default private GridBagConstraints c = new GridBagConstraints();
	@Default private JButton back = new JButton("back");
	@Default private JPanel modelPanel = new JPanel();
	@Default private JPanel graphPanel = new JPanel();
	@Default private JPanel rankingPanel = new JPanel();
	
	@Default private ProMList<RankedListItem> rankList = null;
	@Default private DotPanel vis = new DotPanel(new Dot());
	@Default private EnhancementExogenousDatasetGraphController currentGraphController = null;
	
	public ExogenousEnhancementGraphInvestigator setup() {
		createPanels();
		// setup constraints
		c.anchor = GridBagConstraints.LINE_START;
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight =1;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.4;
		c.insets = new Insets(5, 5, 5, 5);
		c.fill = GridBagConstraints.BOTH;
		// style and set main
		main.setBackground(Color.DARK_GRAY);
		main.setLayout(new GridBagLayout());
		// add panels
		// adding model panel		
		c.gridwidth = 3;
		c.weightx = 1;
		c.ipady =150;
		main.add(modelPanel, c);
		c.ipady = 0;
		c.gridy++;
		// adding graph panel
		c.gridwidth = 2;
		c.weightx = 0.7;
		c.weighty = 0.6;
		main.add(graphPanel, c);
		// adding ranking panel
		c.gridwidth = 1;
		c.weightx = 0.3;
		c.gridx = 2;
		main.add(rankingPanel, c);
		// add back button
		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.0;
		c.weighty = 0.0;
		main.add(back, c);
		main.validate();
		return this;
	}
	
	public void createPanels() {
		// set up ranking panel
		rankingPanel.setLayout( new BorderLayout());
		// create list of possible items
		rankList = new ProMList<RankedListItem>(
				"Exogenous Ranked Charts", 
				new ListModel(
						this.source.getExoCharts().entrySet().stream()
						.map( entry -> { return RankedListItem.builder()
												.controller((EnhancementExogenousDatasetGraphController)entry.getValue())
												.id(entry.getKey())
												.build();})
						.collect(Collectors.toList())
						.toArray()
		));	
		rankList.setSelectionMode(ListModel.SINGLE_SELECTION);
		rankList.setSelectedIndex(0);
		rankList.setBackground(Color.GRAY);
		rankList.setMinimumSize(new Dimension(150,500));
		rankList.setPreferredSize(new Dimension(175,600));
		rankList.setMaximumSize(new Dimension(250,750));
		rankList.addMouseListener(new SelectionListener(this, rankList));
		rankList.validate();
		rankingPanel.add(rankList);
		rankingPanel.setBackground(Color.DARK_GRAY);
		rankingPanel.validate();
		// set up  model panel 
		vis.setDirection(GraphDirection.leftRight);
		vis.changeDot( 
			createDotVis(),
			true);
		modelPanel.setPreferredSize(new Dimension(600,250));
		modelPanel.setLayout(new BorderLayout());
		modelPanel.add(vis);
		modelPanel.validate();
		// set up graph panel
		graphPanel.setLayout(new BorderLayout());
		graphPanel.setPreferredSize(new Dimension(900,600));
		graphPanel.setBackground(Color.BLUE);
		currentGraphController = ((EnhancementExogenousDatasetGraphController) this.source.getExoCharts().get(this.source.getExoCharts().keySet().iterator().next()))
				.createCopy();
		graphPanel.add(
				currentGraphController
		);
		graphPanel.validate();
		// add mouse listener to back button
		back.addMouseListener(new BackListener(back, controller));
	}
	
	private void swapGraphController(RankedListItem cont) {
		this.graphPanel.remove(currentGraphController);
		currentGraphController = cont.getController()
				.createCopy();
		graphPanel.add(
				currentGraphController
		);
		graphPanel.validate();
	}

	private void changeDotFocus(RankedListItem item) {
		this.vis.changeDot(createDotVis(item.getId()), true);
	}
	
	/**
	 * Attempt to center and zoom but seems not possible with public api.
	 * @param item
	 */
	private void tryToCenter(RankedListItem item) {
		ExoDotTransition exDot = findTransition(item.getId(), this.vis.getDot());
		double x=0.0,y=0.0;
		boolean xSet = false,ySet = false;
		System.out.println("-- DEBUG FIND SVG X,Y --");
		System.out.println("Can find element using id? :: "+this.vis.getSVG().getElement(exDot.getId()).toString());
		System.out.println("SVG inline attributes :: "+ this.vis.getSVG().getElement(exDot.getId()).getInlineAttributes().toString());
		System.out.println("SVG presentation attributes :: "+ this.vis.getSVG().getElement(exDot.getId()).getPresentationAttributes().toString());
		System.out.println("SVG children :: "+ this.vis.getSVG().getElement(exDot.getId()).getChildren().size());
		for (SVGElement el : this.vis.getSVG().getElement(exDot.getId()).getChildren()) {
			System.out.println("Child tagname ::"+ el.getTagName());
			if (el.getId() != null) {
				System.out.println("Child Id :: " + el.getId().toString());
			}
			System.out.println("Child inline attributes :: "+ el.getInlineAttributes().toString());
			System.out.println("Child presentation attributes :: "+ el.getPresentationAttributes().toString());
			if (el.getPresentationAttributes().contains("x")) {
				System.out.println("Child has x of :: "+ el.getPresAbsolute("x").getDoubleValue() );
				x = el.getPresAbsolute("x").getDoubleValue();
				xSet = true;
			}
			if (el.getPresentationAttributes().contains("y")) {
				System.out.println("Child has y of :: "+ el.getPresAbsolute("y").getDoubleValue() );
				y = el.getPresAbsolute("y").getDoubleValue();
				ySet =true;
			}
				
			
		}
//		find some way to call, however I need a x,y point on the svg 
//		found some x , y on child of element
//		using thoses and functions on vis to find a navigation point to focus on
		try {
			if (xSet && ySet) {
				Point2D p = this.vis.transformImageToNavigation(new Point2D.Double(x, y));
//				centers but does not account for the viewport size
//				also zoom in is private
				this.vis.centerImageAround(new Point((int)p.getX(),(int)p.getY()));
			}
		} catch (NoninvertibleTransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private Dot createDotVis() {
		ExogenousEnhancementDotPanel dotp = this.source.getSource().getVis();
		return DotGraphVisualisation.builder()
				.graph(dotp.getGraph())
				.updatedGraph(dotp.getUpdatedGraph())
				.swapMap(dotp.getSwapMap())
				.rules(dotp.getRules())
				.transMapping(dotp.getSource().getFocus().getTask().getTransMap())
				.build()
				.make()
				.getVisualisation();
	}
	
	private ExoDotTransition findTransition(String transId, Dot dot) {
		ExoDotTransition exNode = null;
//		find transition being displayed
		for(DotNode node : dot.getNodes()) {
			if (node.getClass().equals(ExoDotTransition.class)) {
				exNode = (ExoDotTransition) node;
				System.out.println("checking node="+exNode.getControlFlowId());
				if (transId.contains(exNode.getControlFlowId())) {
					exNode.highlightNode();
					break;
				}
			}
		}
		return exNode;
	}
	
	private Dot createDotVis(String transId) {
		ExogenousEnhancementDotPanel dotp = this.source.getSource().getVis();
		Dot newDot = DotGraphVisualisation.builder()
				.graph(dotp.getGraph())
				.updatedGraph(dotp.getUpdatedGraph())
				.swapMap(dotp.getSwapMap())
				.rules(dotp.getRules())
				.transMapping(dotp.getSource().getFocus().getTask().getTransMap())
				.build()
				.make()
				.getVisualisation();
//		find transition being displayed
		ExoDotTransition exNode = findTransition(transId, newDot);
		if (exNode != null) {
			exNode.highlightNode();
		}
		return newDot;
	}
	
	@Builder
	private static class RankedListItem {
		
		@Getter private EnhancementExogenousDatasetGraphController controller;
		@Getter private String id;
		
		public String toString() {
			return "Rank=N/A || Transition="+controller.transName+" || Subseries="+controller.datasetName + "|| Distance=N/A";
		}
	}
	
	private class SelectionListener implements MouseListener {
		
		private ExogenousEnhancementGraphInvestigator source; 
		private ProMList<RankedListItem> clicked;
		
		public SelectionListener(ExogenousEnhancementGraphInvestigator source, ProMList<RankedListItem> clicked) {
			this.source = source;
			this.clicked = clicked;
		}

		public void mouseClicked(MouseEvent e) {
			if (this.clicked.getSelectedValuesList().size() > 0) {
				this.source.swapGraphController(this.clicked.getSelectedValuesList().get(0));
				this.source.changeDotFocus(this.clicked.getSelectedValuesList().get(0));
			}
			
		}

		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
		
		
	}
	
	private class BackListener implements MouseListener {
		
		private JButton self;
		private ExogenousDiscoveryInvestigator controller;
		
		public BackListener(JButton self, ExogenousDiscoveryInvestigator controller) {
			this.self = self;
			this.controller = controller;
		}

		public void mouseClicked(MouseEvent e) {
			if (this.self.isEnabled()) {
				this.controller.switchView(this.controller.enhancementTracablityViewKey);
			}
			
		}

		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
	}
}
