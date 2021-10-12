package org.processmining.qut.exogenousaware.gui.workers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.ui.widgets.ProMScrollablePanel;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceList;
import org.processmining.qut.exogenousaware.gui.ExogenousTraceView;
import org.processmining.qut.exogenousaware.gui.ExogenousTraceView.exoTraceBuilder;
import org.processmining.qut.exogenousaware.gui.listeners.TraceBreakdownEventListener;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;

@Builder
public class TraceVisTraceBreakdownCharts extends SwingWorker<JPanel, String> {

	@NonNull ExogenousTraceView source;
	@NonNull XTrace endo;
	@NonNull JPanel panel;
	@NonNull JProgressBar progress;
	
	@Default GridBagConstraints c = new GridBagConstraints();
	
	
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
		this.source.setTraceBreakdownView(new JScrollPane());
		JPanel view = new ProMScrollablePanel();
		this.source.stylePanel(view);
		this.source.getTraceBreakdownView().setViewportView(view);
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
		Set<String> evKeySet = this.source.getSource().getLinkedSubseries().keySet();
		evKeySet = evKeySet.stream()
				.filter(s -> endo.stream().map(ev -> this.source.getSource().getEventExogenousLinkId(ev)).collect(Collectors.toList()).contains(s))
				.collect(Collectors.toSet());
		evKeySet = evKeySet.stream()
				.filter(s -> this.source.getSource().getLinkedSubseries().get(s).keySet().size() > 0)
				.collect(Collectors.toSet());
		ProMTraceList<XTrace> traceController = new ProMTraceList<XTrace>(
				new exoTraceBuilder(evKeySet)
		);
//		add selected trace and setup listener on wedge click
		traceController.add(endo);
		traceController.addTraceClickListener(TraceBreakdownEventListener.builder().source(this.source).build());
//		set size of controller
		traceController.setMaximumSize(new Dimension(2000,115));
		traceController.setMinimumSize(new Dimension(800,115));
		traceController.setPreferredSize(new Dimension(1200,115));
		bottomLeft.add(traceController, c);
		c.gridy++;
		c.weighty = 1.0;
		c.weightx = 1.0;
		this.panel.add(this.source.getTraceBreakdownView(), c);
		this.panel.validate();
		this.source.getRightTopBottom().validate();
		this.progress.setValue(this.progress.getValue()+1);
//		add event breakdowns in scroll
		for(XEvent ev: endo) {
			JPanel clickable = new JPanel();
			clickable.setLayout(new GridBagLayout());
			GridBagConstraints cc = new GridBagConstraints();
			cc.weightx = 1.0;
			cc.weighty = 0;
			cc.gridx = 0;
			cc.gridy = 0;
			cc.anchor = GridBagConstraints.WEST;
			cc.fill = GridBagConstraints.HORIZONTAL;
			cc.insets = new Insets(25,25,25,25);
//			create graphs for this endogenous event
			TraceVisEventChart chartbuilder = TraceVisEventChart.builder()
					.log(this.source.getSource())
					.endogenous(ev)
					.build();
			chartbuilder.setup();
			this.source.stylePanel(chartbuilder.getView());
			clickable.add(chartbuilder.getView(), cc);
//			style clickable
			this.source.styleChart(clickable);
			clickable.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2, true));
			view.add(clickable);
			view.validate();
			this.panel.validate();
			this.source.getRightTopBottom().validate();
			this.progress.setValue(this.progress.getValue()+1);
		}
		this.source.stylePanel(view);
		view.validate();
		this.panel.validate();
		this.source.getRightTopBottom().validate();
		return bottomLeft;
	}
	
	@Override
    protected void done() 
    {
        // this method is called when the background 
        // thread finishes execution
//		style the panel and send it back to split panes
		this.source.stylePanel(this.panel,false);
		this.panel.validate();
		this.source.getRightTopBottom().setBottomComponent(this.panel);
		this.source.getRightTopBottom().validate();
        
      
    }
}
