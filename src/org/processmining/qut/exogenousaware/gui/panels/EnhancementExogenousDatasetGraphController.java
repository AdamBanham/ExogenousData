package org.processmining.qut.exogenousaware.gui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.data.xy.XYSeriesCollection;
import org.processmining.qut.exogenousaware.data.dot.GuardExpressionHandler;
import org.processmining.qut.exogenousaware.gui.workers.EnhancementAllGraph;
import org.processmining.qut.exogenousaware.gui.workers.EnhancementMedianGraph;
import org.processmining.qut.exogenousaware.gui.workers.EnhancementMedianGraph.ShadingType;
import org.processmining.qut.exogenousaware.gui.workers.helpers.ExogenousObserverGrouper;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class EnhancementExogenousDatasetGraphController extends JPanel {

	@NonNull XYSeriesCollection universe;
	@NonNull List<Map<String,Object>> states;
	@NonNull List<Map<String,Object>> seriesStates;
	@NonNull @Getter String datasetName;
	@NonNull Boolean hasExpression;
	@NonNull @Getter String transName;
	
	@Default @Getter boolean useGroups = false;
	@Default @Getter List<Integer> groups = null;
	@Default @Getter ExogenousObserverGrouper grouper = null;
	@Default GuardExpressionHandler guardExpression = null;
	@Default GridBagLayout layout = new GridBagLayout();
	@Default GridBagConstraints layoutcc = new GridBagConstraints();
	@Default JTabbedPane graphPane = new JTabbedPane();
	@Default JButton pop = new JButton("popout");
	@Default JButton min = new JButton("maximise");
	@Default @Getter Map<String, SwingWorker<JPanel, String>> cachedGraphs = new HashMap<String, SwingWorker<JPanel, String>>();
	@Default Map<Integer, String> workerToTab = new HashMap<Integer, String>();
	
	
	public EnhancementExogenousDatasetGraphController setup() {
//		style panel
		styleMainPanel(this);
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
		JLabel name = new JLabel(this.datasetName.toUpperCase());
		name.setForeground(Color.white);
		name.setFont(new Font("Times New Roman", 24, 24));
		add(name, layoutcc);
		layoutcc.gridx++;
		layoutcc.anchor = GridBagConstraints.EAST;
		layoutcc.weightx = 0.0;
		layoutcc.insets = new Insets(25,0,10,5);
		min.addMouseListener(MinimiseMouseListener.builder().button(min).source(this).build());
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
		JPanel fill = new JPanel();
		fill.setBackground(Color.DARK_GRAY);
		add(fill,layoutcc);
		layoutcc.gridy++;
//		add tabbed panned for all graph panels
		layoutcc.fill = GridBagConstraints.BOTH;
		layoutcc.weighty = 1.0;
//		start graph workers for tabs
//		TODO make graph workers start on tab click for the first time
		addTabs();
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
		this.graphPane.setVisible(false);
		this.validate();
		return this;
	}
	
	public EnhancementExogenousDatasetGraphController maximise() {
		this.graphPane.setVisible(true);
		return this;
	}
	
	public void styleMainPanel(JPanel panel) {
		panel.setBackground(Color.DARK_GRAY);
		panel.setBorder(BorderFactory.createLineBorder(Color.gray, 2));
	}
	
	public void addTabs() {
		int tabCount = 0;
//		add median plot
		if (this.cachedGraphs.containsKey("Median-S")) {
			this.graphPane.addTab("Median-S", ((EnhancementMedianGraph) this.cachedGraphs.get("Median")).getNewChart());
			this.workerToTab.put(tabCount, "Median-S");
			tabCount++;
		} else {
			EnhancementMedianGraph worker = EnhancementMedianGraph.builder()
					.title(this.datasetName + " - Median (STD)")
					.xlabel("time:timestamp (hours)")
					.ylabel("value")
					.dataState(seriesStates)
					.useGroups(this.useGroups && this.groups != null)
					.groups(this.groups)
					.grouper(this.grouper)
					.hasExpression(hasExpression)
					.expression(guardExpression)
					.graphData(universe)
					.build()
					.setup();
			worker.execute();
			this.graphPane.addTab("Median-S", worker.getMain());
			this.cachedGraphs.put("Median-S", worker);
			this.workerToTab.put(tabCount, "Median-S");
			tabCount++;
		}
		if (this.cachedGraphs.containsKey("Median")) {
			this.graphPane.addTab("Median", ((EnhancementMedianGraph) this.cachedGraphs.get("Median")).getNewChart());
			this.workerToTab.put(tabCount, "Median");
			tabCount++;
		} else {
			EnhancementMedianGraph worker = EnhancementMedianGraph.builder()
					.title(this.datasetName + " - Median (Q1-3)")
					.xlabel("time:timestamp (hours)")
					.ylabel("value")
					.shadingType(ShadingType.Quartile)
					.dataState(seriesStates)
					.useGroups(this.useGroups && this.groups != null)
					.groups(this.groups)
					.grouper(this.grouper)
					.hasExpression(hasExpression)
					.expression(guardExpression)
					.graphData(universe)
					.build()
					.setup();
			this.graphPane.addTab("Median", worker.getMain());
			this.cachedGraphs.put("Median", worker);
			this.workerToTab.put(tabCount, "Median");
			tabCount++;
		}
//		add normal line-series plot
		if (this.cachedGraphs.containsKey("Line")) {
			this.graphPane.addTab("Line", ((EnhancementAllGraph) this.cachedGraphs.get("Line")).getNewChart());
			this.workerToTab.put(tabCount, "Line");
			tabCount++;
		} else {
			EnhancementAllGraph worker = EnhancementAllGraph.builder()
					.title(this.datasetName + " - Line")
					.xlabel("time:timestamp (hours)")
					.ylabel("value")
					.dataState(seriesStates)
					.hasExpression(hasExpression)
					.expression(guardExpression)
					.graphData(universe)
					.useGroups(this.useGroups && this.groups != null)
					.groups(this.groups)
					.build()
					.setup();
			this.graphPane.addTab("Line", worker.getMain());
			this.cachedGraphs.put("Line", worker);
			this.workerToTab.put(tabCount, "Line");
			tabCount++;
		}
//		add smudge plot
//		if (this.cachedGraphs.containsKey("Smudge")) {
//			this.graphPane.addTab("Smudge", ((EnhancementSmudgeGraph) this.cachedGraphs.get("Smudge")).getNewChart());
//			this.workerToTab.put(tabCount, "Smudge");
//			tabCount++;
//		} else {
//			EnhancementSmudgeGraph worker = EnhancementSmudgeGraph.builder()
//					.title(this.datasetName + " - Smudge")
//					.xlabel("time:timestamp (hours)")
//					.ylabel("value")
//					.dataState(seriesStates)
//					.hasExpression(hasExpression)
//					.expression(guardExpression)
//					.useGroups(this.useGroups && this.groups != null)
//					.groups(this.groups)
//					.graphData(universe)
//					.build()
//					.setup();
//			graphPane.addTab("Smudge", worker.getMain());
//			this.cachedGraphs.put("Smudge", worker);
//			this.workerToTab.put(tabCount, "Smudge");
//			tabCount++;
//		}		
//		add Cluster (Model Based Approach) plot
//		if (this.cachedGraphs.containsKey("Cluster Model")) {
//			this.graphPane.addTab("Cluster", null, ((EnhancementClusterGraph) this.cachedGraphs.get("Cluster Model")).getNewChart(), "Model Based");
//			this.workerToTab.put(tabCount, "Cluster Model");
//			tabCount++;
//		} else {
//			EnhancementClusterGraph worker = EnhancementClusterGraph.builder()
//					.title(this.datasetName + " - Clustered Sequences (Model)")
//					.xlabel("time:timestamp (hours)")
//					.ylabel("value")
//					.dataState(seriesStates)
//					.hasExpression(hasExpression)
//					.expression(guardExpression)
//					.graphData(universe)
//					.useGroups(this.useGroups && this.groups != null)
//					.groups(this.groups)
//					.build()
//					.setup();
//			this.graphPane.addTab("Cluster", null, worker.getMain() , "Model Based");
//			this.cachedGraphs.put("Cluster Model", worker);
//			this.workerToTab.put(tabCount, "Cluster Model");
//			tabCount++;
//		}
////		add Cluster (Shape Based Approach) plot
//		if (this.cachedGraphs.containsKey("Cluster Shape")) {
//			this.graphPane.addTab("Cluster", null, ((EnhancementClusterGraph) this.cachedGraphs.get("Cluster Shape")).getNewChart(), "Shape Based");
//			this.workerToTab.put(tabCount, "Cluster Shape");
//			tabCount++;
//		} else {
//			EnhancementClusterGraph worker = EnhancementClusterGraph.builder()
//					.title(this.datasetName + " - Clustered Sequences (Shape)")
//					.xlabel("time:timestamp (hours)")
//					.ylabel("value")
//					.graphType(ClusterGraphType.shape)
//					.dataState(seriesStates)
//					.hasExpression(hasExpression)
//					.expression(guardExpression)
//					.graphData(universe)
//					.useGroups(this.useGroups && this.groups != null)
//					.groups(this.groups)
//					.build()
//					.setup();
//			graphPane.addTab("Cluster", null, worker.getMain() , "Shape Based");
//			this.cachedGraphs.put("Cluster Shape", worker);
//			this.workerToTab.put(tabCount, "Cluster Shape");
//			tabCount++;
//		}
		graphPane.addChangeListener(new ChangeListener() {
			
			public void stateChanged(ChangeEvent e) {
				SwingWorker worker = cachedGraphs.get(workerToTab.get(graphPane.getSelectedIndex()));
				if (!worker.isDone()) {
					worker.execute();
				}
			}
		});
	}
	
	@Builder
	public static class PopoutActionListener implements ActionListener {

		@NonNull private EnhancementExogenousDatasetGraphController source;
		
		@Default private boolean poped = false;
		
		public void actionPerformed(ActionEvent e) {
			if (!this.poped) {
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
						.transName(this.source.transName)
						.useGroups(true)
						.groups(this.source.groups)
						.grouper(this.source.grouper)
						.build()
						.setup()
						.maximise();
				resource.pop.setEnabled(false);
				resource.min.setEnabled(false);
				frame.getContentPane().add(resource, BorderLayout.CENTER);
				frame.setPreferredSize(new Dimension(800,600));
				frame.pack();
				this.source.pop.setEnabled(false);
				frame.setVisible(true);
				frame.addWindowListener(PopoutCloseListener.builder().source(this).build());
				this.poped = true;
			}
		}
		
		public void reset() {
			this.poped = false;
			this.source.pop.setEnabled(true);
		}
		
	}
	
	public EnhancementExogenousDatasetGraphController createCopy() {
		return EnhancementExogenousDatasetGraphController.builder()
				.datasetName(this.datasetName)
				.universe(this.universe)
				.seriesStates(this.seriesStates)
				.states(this.states)
				.hasExpression(this.hasExpression)
				.guardExpression(this.guardExpression)
				.transName(this.transName)
				.useGroups(true)
				.groups(this.groups)
				.grouper(this.grouper)
				.build()
				.setup()
				.maximise();
	}

	@Builder
	public static class PopoutCloseListener implements WindowListener {
		
		@NonNull private PopoutActionListener source;
		
		public void windowClosed(WindowEvent e) {
			this.source.reset();
			
		}
		
		public void windowClosing(WindowEvent e) {
			// TODO Auto-generated method stub
		}
		
		public void windowOpened(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void windowIconified(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void windowDeiconified(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void windowActivated(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void windowDeactivated(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	@Builder
	public static class MinimiseMouseListener implements MouseListener {
		
		@NonNull private JButton button;
		@NonNull private EnhancementExogenousDatasetGraphController source;
		
		@Default private boolean clicked = true;
		
		public void mouseClicked(MouseEvent e) {
			if (this.button.isEnabled()) {
				if (this.clicked) {
					this.clicked = !this.clicked;
					this.button.setText("minimise");
					this.source.graphPane.setVisible(true);
				}
				else {
					this.button.setText("maximise");
					this.source.graphPane.setVisible(false);
					this.clicked = !this.clicked;
				}
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
