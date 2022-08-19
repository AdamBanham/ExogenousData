package org.processmining.qut.exogenousaware.gui.workers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.jfree.chart.ChartPanel;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceList;
import org.processmining.qut.exogenousaware.gui.ExogenousTraceView;
import org.processmining.qut.exogenousaware.gui.listeners.TraceBreakdownEventListener;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousTraceViewJChartFilterPanel;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousTraceViewJChartFilterPanel.ChartFilter;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousTraceViewJChartFilterPanel.ChartHolder;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousTraceViewJChartFilterPanel.PanelFilter;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousTraceViewJChartFilterPanel.SlicerFilter;
import org.processmining.qut.exogenousaware.gui.promlist.ProMListComponents.ExoTraceBuilder;
import org.processmining.qut.exogenousaware.gui.widgets.TraceViewButton;
import org.processmining.qut.exogenousaware.gui.workers.TraceVisEventChart.ChartSeriesController;

import com.fluxicon.slickerbox.factory.SlickerFactory;
import com.fluxicon.slickerbox.ui.SlickerScrollBarUI;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class TraceVisTraceBreakdownCharts extends SwingWorker<JPanel, String> {

	@NonNull ExogenousTraceView source;
	@NonNull XTrace endo;
	@NonNull JPanel panel;
	@NonNull JProgressBar progress;
	
	@Default GridBagConstraints c = new GridBagConstraints();
	@Default GridBagConstraints vc = new GridBagConstraints();
	
	@Default Map<String, Map<String, List<ChartPanel>>> chartDict = new HashMap<String, Map<String, List<ChartPanel>>>();
	@Default Map<String, Map<String, List<ChartSeriesController>>> seriesControllers = new HashMap<String, Map<String, List<ChartSeriesController>>>();
	@Default @Getter List<ChartHolder> chartHolders = new ArrayList<ChartHolder>();
	
	@Override
	protected JPanel doInBackground() throws Exception {
//		setup for spacer
		JPanel bottomLeft = this.panel;
		this.source.stylePanel(bottomLeft);
		this.panel.setMinimumSize(new Dimension(800,200));
		this.panel.validate();
//		setup progress
		this.progress.setValue(0);
		this.progress.setMaximum(1 + endo.size());
//		create layout manager
		bottomLeft.setLayout(new GridBagLayout());
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.WEST;
		c.gridx =0;
		c.gridy =0;
		c.weightx =1.0;
		c.weighty = 0;
//		create panels
		JPanel view = new JPanel();
		this.source.stylePanel(view, false);
//		build a trace list at top of panel for navigation
		JLabel trace = this.source.createLeftAlignedLabel(
				"<html><p>Event Sequence for : "+endo.getAttributes().get("concept:name")+"</p></html>",
				true,
				18);
		bottomLeft.add( 
				trace,
				c
		);
		c.weightx = 0;
		c.gridy++;
//		setup trace wedge vis with a listener of event clicks
		ExoTraceBuilder builder = new ExoTraceBuilder();
		ProMTraceList<XTrace> traceController = new ProMTraceList<XTrace>(
				builder
		);
//		add selected trace and setup listener on wedge click
		traceController.add(endo);
		traceController.addTraceClickListener(TraceBreakdownEventListener.builder().source(this.source).builder(builder).controller(traceController).build());
//		set size of controller
		traceController.setMaximumSize(new Dimension(2000,85));
		traceController.setMinimumSize(new Dimension(800,85));
		traceController.setPreferredSize(new Dimension(1200,85));
		traceController.getScrollPane().getHorizontalScrollBar().setBackground(Color.LIGHT_GRAY);
		traceController.getScrollPane().getHorizontalScrollBar().setForeground(Color.gray);
		traceController.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		bottomLeft.add(traceController, c);
		c.gridy++;
		c.weighty = 1.0;
		c.weightx = 1.0;
		this.progress.setValue(this.progress.getValue()+1);
		this.handleChartFactories(view);
		this.panel.add(view, c);
		view.validate();
		this.panel.validate();
		this.source.getRightTopBottom().validate();
		return bottomLeft;
	}
	
	public void handleChartFactories(JPanel view) {
//		add layout manager
		view.setLayout(new GridBagLayout());
		vc.gridheight =1;
		vc.gridwidth =3;
		vc.gridy =0;
		vc.weightx = 0.05;
		vc.weighty = 0.05;
		vc.anchor = GridBagConstraints.FIRST_LINE_START;
		vc.fill = GridBagConstraints.HORIZONTAL;
//		Add handler for selecting exo-panel
		JPanel panelHandler = new JPanel();
		panelHandler.setLayout(new BoxLayout(panelHandler,BoxLayout.Y_AXIS));
		this.source.stylePanel(panelHandler, false);
		view.add(panelHandler,vc);
//		Add handler for selecting slicer
		JPanel sliceHandler = new JPanel();
		sliceHandler.setLayout(new BoxLayout(sliceHandler,BoxLayout.Y_AXIS));
		this.source.stylePanel(sliceHandler, false);
		view.add(sliceHandler,vc);
		vc.weightx = 0.9;
		vc.fill = GridBagConstraints.BOTH;
		vc.weighty = 1.0;
		vc.insets = new Insets(5,5,5,5);
//		Add handler for chartDict 
		JPanel graphView = new JPanel();
		graphView.setLayout(new GridBagLayout());
		GridBagConstraints gcc = new GridBagConstraints(); 
		gcc.ipadx =0;
		gcc.ipady =0;
		gcc.gridx =0;
		gcc.weightx =0.9;
		gcc.weighty =0;
		gcc.insets = new Insets(5,5,5,5);
		gcc.fill = GridBagConstraints.NONE;
		this.source.stylePanel(graphView, false);
//		control for the exo-panels seen and slice types
		Set<String> panels = new HashSet<String>();
		Set<String> slices = new HashSet<String>();
//		add event breakdowns in scroll
		this.source.getTraceBreakdownView().clear();
		int eventIndex = -1;
		for(XEvent ev: endo) {
			try {
			eventIndex++;
//			create graphs for this endogenous event
			TraceVisEventChart chartbuilder = TraceVisEventChart.builder()
					.log(this.source.getSource())
					.eventIndex(eventIndex)
					.endogenous(ev)
					.build();
//			add all generated charts to panels
			chartbuilder.setup();
//			add chart 
			for(ChartHolder chart: chartbuilder.getCharts()) {
				this.source.stylePanel(chart.getPanel(), false);
//				this.source.styleChart((JPanel) chart.getChart());
				this.source.getTraceBreakdownView().addChart(chart);
			}
//			keeping just in case
			this.source.getRightTopBottom().validate();
			this.progress.setValue(this.progress.getValue()+1);
			for(String key: chartbuilder.getChartDict().keySet()) {
				panels.add(key);
				for(String okey: chartbuilder.getChartDict().get(key).keySet()) {
					slices.add(okey);
				}
			}
			} catch (Exception e) {
				System.out.println("unable to build TraceVisEventChart");
				e.printStackTrace();
			}
		}
		gcc.insets = new Insets(5,5,5,5);
		gcc.weightx =0.9;
		gcc.fill = GridBagConstraints.HORIZONTAL;
		System.out.println("Seen panels :"+panels.toString());
		System.out.println("Seen slices :"+slices.toString());
//		add buttons for panels
		panelHandler.add(Box.createRigidArea(new Dimension(0,5)));
		JLabel label = SlickerFactory.instance().createLabel("Exo-Panels");
		label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		panelHandler.add(label);
		panelHandler.add(Box.createRigidArea(new Dimension(0,5)));
		for(String panel : panels) {
			TraceViewButton button = new TraceViewButton(panel);
			button.setAlignmentX(JButton.CENTER_ALIGNMENT);
			button.setMargin(new Insets(5, 30, 5, 30));
			button.setMaximumSize(new Dimension(100,25));
			button.addActionListener(new ExoPanelFilterListener(button, this.source.getTraceBreakdownView(), panel));
			panelHandler.add(button);
			panelHandler.add(Box.createRigidArea(new Dimension(0,3)));
		}
//		add buttons for slices
		sliceHandler.add(Box.createRigidArea(new Dimension(0,5)));
		label = SlickerFactory.instance().createLabel("Slicers");
		label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		sliceHandler.add(label);
		sliceHandler.add(Box.createRigidArea(new Dimension(0,5)));
		for(String slice : slices) {
			TraceViewButton button = new TraceViewButton(slice);
			button.setAlignmentX(JButton.CENTER_ALIGNMENT);
			button.setMargin(new Insets(5, 30, 5, 30));
			button.setMaximumSize(new Dimension(100,25));
			button.addActionListener(new SlicerFilterListener(button, this.source.getTraceBreakdownView(), slice));
			sliceHandler.add(button);
			sliceHandler.add(Box.createRigidArea(new Dimension(0,3)));
		}
//		add scroll plane
		JScrollPane graphPane = this.source.getTraceBreakdownView().getScroller();
		JScrollBar vBar = graphPane.getVerticalScrollBar();
		vBar.setUI(new SlickerScrollBarUI(vBar, Color.LIGHT_GRAY, Color.GRAY,Color.DARK_GRAY, 4, 12));
		this.source.stylePanel(graphPane, false);
		view.add(graphPane,vc);	
		graphView.validate();
		graphPane.validate();
		view.validate();
		this.panel.validate();
	}
	
	
	public void reducePanels() {
		
	}
	
	public void reduceSlicers() {
		
	}
	
	@Override
    protected void done() 
    {
        // this method is called when the background 
        // thread finishes execution
//		style the panel and send it back to split panes
		if (!isCancelled()) {
			this.source.stylePanel(this.panel,false);
			this.panel.validate();
			this.source.getRightTopBottom().setBottomComponent(this.panel);
			this.source.getRightTopBottom().validate();
		}
        
      
    }
	
	public static class ExoPanelFilterListener implements ActionListener {

		TraceViewButton button;
		ExogenousTraceViewJChartFilterPanel controller;
		ChartFilter filter;
		boolean added = false;
		
		public ExoPanelFilterListener(TraceViewButton button, ExogenousTraceViewJChartFilterPanel controller, String panel) {
			this.button = button;
			this.controller = controller;
			this.filter = PanelFilter.builder()
					.exoPanel(panel)
					.build();
		}
		
		public void actionPerformed(ActionEvent e) {
			if (!added) {
				controller.filter(filter);
				added = true;
			} else {
				controller.removeFilter(filter);
				added = false;
			}
			button.setActive(added);
		}
		
	}
	
	public static class SlicerFilterListener implements ActionListener {
		
		TraceViewButton button;
		ExogenousTraceViewJChartFilterPanel controller;
		ChartFilter filter;
		boolean added = false;
		
		public SlicerFilterListener(TraceViewButton button, ExogenousTraceViewJChartFilterPanel controller, String slicer) {
			this.button = button;
			this.controller = controller;
			this.filter = SlicerFilter.builder()
					.slicer(slicer)
					.build();
		}

		public void actionPerformed(ActionEvent arg0) {
			if (!added) {
				controller.filter(filter);
				added = true;
			} else {
				controller.removeFilter(filter);
				added = false;
			}
			button.setActive(added);
		}
	}
}
