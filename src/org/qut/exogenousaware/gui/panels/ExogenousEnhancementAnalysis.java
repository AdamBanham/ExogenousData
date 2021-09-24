package org.qut.exogenousaware.gui.panels;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeriesCollection;
import org.qut.exogenousaware.data.ExogenousAnnotatedLog;
import org.qut.exogenousaware.gui.ExogenousEnhancementTracablity;
import org.qut.exogenousaware.gui.panels.ExogenousEnhancementDotPanel.ExoDotNode;
import org.qut.exogenousaware.gui.workers.ExogenousObservedUniverse;
import org.qut.exogenousaware.ml.clustering.HierarchicalClustering.DistanceType;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NonNull;

@Builder
@Data
public class ExogenousEnhancementAnalysis {
	
	@NonNull private ExogenousEnhancementTracablity source;
	

	@Default private JPanel main = new JPanel();
	@Default private JScrollPane scroll = new JScrollPane();
	@Default private JLabel focusedTrans = new JLabel();
	@Default private JLabel guard = new JLabel();
	@Default private JProgressBar progress = new JProgressBar();
	@Default private JLabel progressLabel = new JLabel();
	@Default private Map<String,ChartPanel> exoCharts = new HashMap<String, ChartPanel>();
	@Default private ExoDotNode focus = null;
	@Default private ExogenousObservedUniverse task = null;
	@Default private GridBagConstraints c = new GridBagConstraints();
	@Default private Map<ExoDotNode, Map<String,XYSeriesCollection>> cacheUniverse = new HashMap<ExoDotNode, Map<String,XYSeriesCollection>>();
	@Default private Map<ExoDotNode, Map<String, List<Map<String,Object>>>> cacheStates = new HashMap<ExoDotNode, Map<String, List<Map<String,Object>>>>();
	@Default private Map<ExoDotNode, Map<String, List<Map<String,Object>>>> cacheSeriesStates = new HashMap<ExoDotNode, Map<String, List<Map<String,Object>>>>();
	
	@Default private Color expressionFailedColor = new Color(128,0,0,25);
	@Default private Color expressionPassedColor = new Color(0,102,51,25);
	@Default private Color expressionNullColor = new Color(0,0,0,25);
	
	public ExogenousEnhancementAnalysis setup() {
//		link main and scroll
		this.scroll.setViewportView(this.main);
		this.scroll.setBorder(BorderFactory.createEmptyBorder());
//		style main
		this.main.setLayout(new GridBagLayout());
		this.c.gridheight = 1;
		this.c.gridwidth = 2;
		this.c.weightx = 0;
		this.c.weighty = 0;
		this.c.gridx = 0;
		this.c.gridy = 1;
		this.c.anchor = GridBagConstraints.LINE_START;
		this.c.fill = GridBagConstraints.NONE;
		this.c.insets = new Insets(10,25,10,25);
		this.main.setBackground(Color.DARK_GRAY);
//		add label
		this.focusedTrans.setText("click a transition in the graph above");
		this.focusedTrans.setForeground(Color.WHITE);
		this.main.add(this.focusedTrans, c);
//		add label for guard
		this.c.gridy++;
		this.guard.setForeground(Color.WHITE);
		this.main.add(this.guard, c);
//		add progress
		this.c.gridy++;
		this.c.gridwidth = 1;
		this.progressLabel.setForeground(Color.WHITE);
		this.c.anchor = GridBagConstraints.LINE_START;
		this.main.add(this.progressLabel, c);
		this.progressLabel.setVisible(false);
		this.c.gridx++;
		this.c.anchor = GridBagConstraints.LINE_START;
		this.main.add(this.progress, c);
		this.progress.setVisible(false);
//		setup constraints for any graphs to be added after
		this.c.gridwidth = 2;
		this.c.anchor = GridBagConstraints.CENTER;
		this.c.weightx = 1;
		this.c.weighty = 0.1;
		this.c.ipadx = 600;
		return this;
	}
	
	public void reset() {
//		clear cache
		this.cacheUniverse = new HashMap<ExoDotNode, Map<String,XYSeriesCollection>>();
		this.cacheStates = new HashMap<ExoDotNode, Map<String, List<Map<String,Object>>>>();
		this.updateAnalysis(null);
		this.task = null;
		this.hideCharts();		
	}
	
	public String formatTransLabel(String trans) {
		String formater = "Enhancing on \"%s\" :";
		return String.format(formater, trans);
	}
	
	public void updateAnalysis(ExoDotNode node) {
		if(node != null) {
			this.focus = node;
			this.focusedTrans.setText(this.formatTransLabel(this.focus.getTransLabel()));
			if (node.getGuardExpression() != null) {
				this.guard.setVisible(true);
				this.guard.setText(node.getGuardExpression().getRepresentation());
//				test out evaluation method on guard
				if(focus.getGuardExpression().hasExpression()) {
					Map<String, Object> update = new HashMap<String, Object>();
					for(String var: node.getGuardExpression().getGuardExpression().getNormalVariables()) {
						if (var.toLowerCase().contains("arterialline")) {
							update.put(var, 4000.0);
						} else {
							update.put(var, 0.0);
						}
						
					}
					for(String var: node.getGuardExpression().getGuardExpression().getPrimeVariables()) {
						if (var.toLowerCase().contains("arterialline")) {
							update.put(var, 4000.0);
						} else {
							update.put(var, 0.0);
						}
					}
					System.out.println("variables state used :: " +update.toString());
					System.out.println("evaluating expressions with empty update :: " +node.getGuardExpression().getGuardExpression().evaluate(update));
				}
			}
			this.buildObservedUniverse();
			this.showProgress(true);
		} else {
			this.focus = null;
			this.hideCharts();
			this.focusedTrans.setText("click a transition in the graph above");
			this.guard.setVisible(false);
			this.showProgress(false);
		}
		
	}
	
	public void hideCharts() {
//		loop through previous charts and hide all charts
		for(Entry<String, ChartPanel> entry : this.exoCharts.entrySet()) {
			entry.getValue().setVisible(false);
		}
	}
	
	public void showProgress(Boolean vis) {
		this.progress.setVisible(vis);
		this.progressLabel.setVisible(vis);
	}
	
	
	public void buildObservedUniverse() {
		this.hideCharts();
//		check for cached results
		if(this.cacheUniverse.containsKey(this.focus)) {
			this.hideCharts();
			this.handleObservedUniverse(true);
		} else {
//		create new worker and build a universe
			ExogenousAnnotatedLog log = this.source.getSource().getSource();
			this.showProgress(true);
			this.progressLabel.setText("Building observed exogenous universe :");
			this.task = ExogenousObservedUniverse.builder()
				.log(log)
				.alignment(this.source.getAlignment())
				.focus(this.focus)
				.progress(this.progress)
				.label(this.progressLabel)
				.build()
				.setup();
			this.task.addPropertyChangeListener(new ObservedListener(this));
			this.task.execute();
		}
	}
	
	public void handleObservedUniverse(Boolean cached) {
		Map<String, XYSeriesCollection> universe = null;
		Map<String, List<Map<String,Object>>> states = null;
		Map<String, List<Map<String,Object>>> seriesStates = null;
		this.progress.setVisible(true);
		this.progress.setValue(0);
		this.progressLabel.setText("Building Graphs :");
		try {
			universe = cached ? this.cacheUniverse.get(this.focus) : this.task.get();
			states = cached ? this.cacheStates.get(this.focus) : this.task.getDatasetStates();
			seriesStates = cached ? this.cacheSeriesStates.get(this.focus) : this.task.getSeriesStates();
			this.progress.setMaximum(universe.entrySet().size() * 3);
			for(Entry<String,XYSeriesCollection> entry : universe.entrySet()) {
				if (cached) {
					showCachedGraph(entry.getKey() + " - Subseries");
					showCachedGraph(entry.getKey() + " - Median By Group");
					showCachedGraph(entry.getKey() + " - Cluster Graph");
				} else {
				System.out.println("Building graphs");
				
				EnhancementAllGraph allGraphBuilder = EnhancementAllGraph.builder()
						.title(entry.getKey() + " - Subseries")
						.xlabel("time:timestamp (hours)")
						.ylabel("value")
						.dataState(seriesStates.get(entry.getKey()))
						.hasExpression(this.focus.getGuardExpression().hasExpression())
						.expression(this.focus.getGuardExpression())
						.graphData(entry.getValue())
						.build();
				allGraphBuilder.make();
				JFreeChart chart = allGraphBuilder.graph.getChart();
				cacheGraph(allGraphBuilder.getTitle(), chart);
				
				EnhancementMedianGraph medianGraphBuilder = EnhancementMedianGraph.builder()
						.title(entry.getKey() + " - Median By Group")
						.xlabel("time:timestamp (hours)")
						.ylabel("value")
						.dataState(seriesStates.get(entry.getKey()))
						.hasExpression(this.focus.getGuardExpression().hasExpression())
						.expression(this.focus.getGuardExpression())
						.graphData(entry.getValue())
						.build();
				medianGraphBuilder.make();
				chart = medianGraphBuilder.graph.getChart();
				cacheGraph(medianGraphBuilder.getTitle(), chart);
				
				EnhancementClusterGraph clusterGraphBuilder = EnhancementClusterGraph.builder()
						.title(entry.getKey() + " - (model) Cluster Graph")
						.xlabel("time:timestamp (hours)")
						.ylabel("value")
						.dataState(seriesStates.get(entry.getKey()))
						.hasExpression(this.focus.getGuardExpression().hasExpression())
						.expression(this.focus.getGuardExpression())
						.graphData(entry.getValue())
						.build();
				clusterGraphBuilder.make();
				chart = clusterGraphBuilder.graph.getChart();
				cacheGraph(clusterGraphBuilder.getTitle(), chart);
				
				clusterGraphBuilder = EnhancementClusterGraph.builder()
						.title(entry.getKey() + " - (DTW) Cluster Graph")
						.xlabel("time:timestamp (hours)")
						.ylabel("value")
						.distance(DistanceType.DTW)
						.dataState(seriesStates.get(entry.getKey()))
						.hasExpression(this.focus.getGuardExpression().hasExpression())
						.expression(this.focus.getGuardExpression())
						.graphData(entry.getValue())
						.build();
				clusterGraphBuilder.make();
				chart = clusterGraphBuilder.graph.getChart();
				cacheGraph(clusterGraphBuilder.getTitle(), chart);
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.showProgress(false);
		this.main.validate();
		this.scroll.validate();
		if (!cached && universe != null) {
			this.cacheUniverse.put(this.focus, universe);
			this.cacheStates.put(this.focus, states);
			this.cacheSeriesStates.put(this.focus, seriesStates);
		}
	}
	
	public void cacheGraph(String title,JFreeChart chart) {
		if (this.exoCharts.containsKey(title)) {
			this.exoCharts.get(title).setChart(chart);
			this.exoCharts.get(title).setVisible(true);
		} else {
			ChartPanel graph = new ChartPanel(
					chart
			);
			this.exoCharts.put(title, graph);
			this.c.gridx = 0;
			this.c.gridy++;
			this.main.add(graph, c);
		}
		System.out.println("added graph");
		this.progress.setValue(this.progress.getValue()+1);
	}
	
	public void showCachedGraph(String title) {
		if (this.exoCharts.containsKey(title)) {
			ChartPanel graph = this.exoCharts.get(title);
			graph.setVisible(true);
		}
	}
	
	
	public class ObservedListener implements PropertyChangeListener {
		
		ExogenousEnhancementAnalysis source;
		
		public ObservedListener(ExogenousEnhancementAnalysis source) {
			this.source = source;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			// TODO Auto-generated method stub
//			System.out.println("property trigger");
//			System.out.println(evt.getPropertyName());
//			System.out.println(evt.getNewValue());
			if (evt.getPropertyName().toString().equals("state")) {
//				System.out.println("state change");
				if(evt.getNewValue().equals(SwingWorker.StateValue.DONE)) {
//					System.out.println("work is done");
					this.source.handleObservedUniverse(false);
				}
			}
			
		}
		
	}


	
}
