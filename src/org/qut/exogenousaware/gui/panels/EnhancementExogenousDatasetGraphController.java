package org.qut.exogenousaware.gui.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;

import org.jfree.data.xy.XYSeriesCollection;
import org.qut.exogenousaware.gui.panels.ExogenousEnhancementDotPanel.GuardExpressionHandler;
import org.qut.exogenousaware.gui.workers.EnhancementAllGraph;
import org.qut.exogenousaware.gui.workers.EnhancementSmudgeGraph;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;

@Builder
public class EnhancementExogenousDatasetGraphController extends JPanel {

	@NonNull XYSeriesCollection universe;
	@NonNull List<Map<String,Object>> states;
	@NonNull List<Map<String,Object>> seriesStates;
	@NonNull String datasetName;
	@NonNull Boolean hasExpression;
	@NonNull String transName;
	
	@Default GuardExpressionHandler guardExpression = null;
	@Default GridBagLayout layout = new GridBagLayout();
	@Default GridBagConstraints layoutcc = new GridBagConstraints();
	@Default JTabbedPane graphPane = new JTabbedPane();
	@Default JButton pop = new JButton("popout");
	@Default JButton min = new JButton("minimise");
	@Default Map<String, SwingWorker<JPanel, String>> cachedGraphs = new HashMap<String, SwingWorker<JPanel, String>>();
	
	
	public EnhancementExogenousDatasetGraphController setup() {
//		setup layout manager
		setLayout(this.layout);
		layoutcc.anchor = GridBagConstraints.WEST;
		layoutcc.gridx=0;
		layoutcc.gridy=0;
		layoutcc.gridheight = 1;
		layoutcc.gridwidth = 1;
		layoutcc.fill = GridBagConstraints.HORIZONTAL;
		layoutcc.weightx=1;
		layoutcc.weighty=0;
//		add label (left) then min and popout buttons (right)
		layoutcc.insets = new Insets(25,50,10,0);
		add(new JLabel(this.datasetName), layoutcc);
		layoutcc.gridx++;
		layoutcc.anchor = GridBagConstraints.EAST;
		layoutcc.weightx = 0.0;
		layoutcc.insets = new Insets(25,0,10,5);
		add(min,layoutcc);
		layoutcc.gridx++;
		layoutcc.insets = new Insets(25,0,10,50);
		pop.addActionListener(PopoutActionListener.builder().source(this).build());
		add(pop, layoutcc);
		layoutcc.weightx = 1.0;
		layoutcc.gridx= 0;
		layoutcc.gridy++;
		layoutcc.anchor = GridBagConstraints.WEST;
//		add a slicer option for this dataset 
//		TODO as this is current only getting one slicer into
		layoutcc.gridwidth = 3;
		layoutcc.insets = new Insets(0,50,10,50);
		add(new JPanel(),layoutcc);
		layoutcc.gridy++;
//		add tabbed panned for all graph panels
		layoutcc.fill = GridBagConstraints.BOTH;
		layoutcc.weighty = 1.0;
//		start smudge worker
		addTabs();
		graphPane.addTab("Median", new JPanel());
		graphPane.addTab("Cluster", null, new JPanel(), "model");
		graphPane.addTab("Cluster", null, new JPanel(), "DTW");
		add(graphPane, layoutcc);
		layoutcc.gridy++;
		layoutcc.fill = GridBagConstraints.HORIZONTAL;
		layoutcc.weighty = 0.0;
//		add controller for series selection
//		TODO implement controller to filter series data shown on graphs
		layoutcc.gridwidth = 3;
		layoutcc.insets = new Insets(0,50,10,50);
		add(new JPanel(),layoutcc);
		layoutcc.gridy++;
//		validate panel
		this.validate();
		return this;
	}
	
	public void addTabs() {
//		add normal line-series plot
		if (this.cachedGraphs.containsKey("Line")) {
			this.graphPane.addTab("Line", ((EnhancementAllGraph) this.cachedGraphs.get("Line")).getNewChart());
		} else {
			EnhancementAllGraph worker = EnhancementAllGraph.builder()
					.title(this.datasetName + " - Subseries")
					.xlabel("time:timestamp (hours)")
					.ylabel("value")
					.dataState(seriesStates)
					.hasExpression(hasExpression)
					.expression(guardExpression)
					.graphData(universe)
					.build()
					.setup();
			worker.execute();
			graphPane.addTab("Line", worker.getMain());
			this.cachedGraphs.put("Line", worker);
		}
//		add smudge plot
		if (this.cachedGraphs.containsKey("Smudge")) {
			this.graphPane.addTab("Smudge", ((EnhancementSmudgeGraph) this.cachedGraphs.get("Smudge")).getNewChart());
		} else {
			EnhancementSmudgeGraph worker = EnhancementSmudgeGraph.builder()
					.title(this.datasetName + " - Subseries")
					.xlabel("time:timestamp (hours)")
					.ylabel("value")
					.dataState(seriesStates)
					.hasExpression(hasExpression)
					.expression(guardExpression)
					.graphData(universe)
					.build()
					.setup();
			worker.execute();
			graphPane.addTab("Smudge", worker.getMain());
			this.cachedGraphs.put("Smudge", worker);
		}		
	}
	
	@Builder
	public static class PopoutActionListener implements ActionListener {

		@NonNull private EnhancementExogenousDatasetGraphController source;
		
		public void actionPerformed(ActionEvent e) {
			// create new frame for this controller
			JFrame frame = new JFrame(this.source.transName+ " --- "+ this.source.datasetName);
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			// rebuild source instance
			EnhancementExogenousDatasetGraphController resource = EnhancementExogenousDatasetGraphController.builder()
					.datasetName(this.source.datasetName)
					.universe(this.source.universe)
					.seriesStates(this.source.seriesStates)
					.states(this.source.states)
					.hasExpression(this.source.hasExpression)
					.guardExpression(this.source.guardExpression)
					.cachedGraphs(this.source.cachedGraphs)
					.transName(this.source.transName)
					.build()
					.setup();
			resource.pop.setEnabled(false);
			resource.min.setEnabled(false);
			frame.getContentPane().add(resource, BorderLayout.CENTER);
			frame.setPreferredSize(new Dimension(800,600));
			frame.pack();
			this.source.pop.setEnabled(false);
			frame.setVisible(true);
		}
		
	}
}
