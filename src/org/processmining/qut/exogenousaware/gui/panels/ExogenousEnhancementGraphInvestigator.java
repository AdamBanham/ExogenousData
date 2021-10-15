package org.processmining.qut.exogenousaware.gui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.processmining.framework.util.ui.widgets.ProMList;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.Dot.GraphDirection;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.qut.exogenousaware.gui.ExogenousDiscoveryInvestigator;
import org.processmining.qut.exogenousaware.gui.dot.DotGraphVisualisation;

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
	
	@Default private ProMList<String> rankList = null;
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
		c.insets = new Insets(25, 10, 25, 10);
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
		rankList = new ProMList<String>("Exogenous Ranked Charts", new ListModel(this.source.getExoCharts().keySet().toArray()));
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
		ExogenousEnhancementDotPanel dotp = this.source.getSource().getVis();
		vis.setDirection(GraphDirection.leftRight);
		vis.changeDot( 
			DotGraphVisualisation.builder()
				.graph(dotp.getGraph())
				.updatedGraph(dotp.getUpdatedGraph())
				.swapMap(dotp.getSwapMap())
				.rules(dotp.getRules())
				.transMapping(dotp.getSource().getFocus().getTask().getTransMap())
				.build()
				.make()
				.getVisualisation(),
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
	
	private void swapGraphController(String id) {
		this.graphPanel.remove(currentGraphController);
		currentGraphController = ((EnhancementExogenousDatasetGraphController) this.source.getExoCharts().get(id))
				.createCopy();
		graphPanel.add(
				currentGraphController
		);
		graphPanel.validate();
	}
	
	private class SelectionListener implements MouseListener {
		
		private ExogenousEnhancementGraphInvestigator source; 
		private ProMList<String> clicked;
		
		public SelectionListener(ExogenousEnhancementGraphInvestigator source, ProMList<String> clicked) {
			this.source = source;
			this.clicked = clicked;
		}

		public void mouseClicked(MouseEvent e) {
			if (this.clicked.getSelectedValuesList().size() > 0) {
				this.source.swapGraphController(this.clicked.getSelectedValuesList().get(0));
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
