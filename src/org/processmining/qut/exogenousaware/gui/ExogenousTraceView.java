package org.processmining.qut.exogenousaware.gui;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.SwingConstants;

import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.jfree.chart.ChartPanel;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.util.ui.widgets.ProMSplitPane;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceList;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceList.ClickListener;
import org.processmining.qut.exogenousaware.data.ExogenousAnnotatedLog;
import org.processmining.qut.exogenousaware.gui.listeners.EndoTraceListener;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousTraceViewJChartFilterPanel;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousTraceViewJChartFilterPanel.EventFilter;
import org.processmining.qut.exogenousaware.gui.promlist.ProMListComponents.ExoTraceBuilder;
import org.processmining.qut.exogenousaware.gui.promlist.WedgeBuilderFactory;
import org.processmining.qut.exogenousaware.gui.workers.TraceVisEventChart;
import org.processmining.qut.exogenousaware.gui.workers.TraceVisOverviewChart;
import org.processmining.qut.exogenousaware.gui.workers.TraceVisTraceBreakdownCharts;

import com.fluxicon.slickerbox.ui.SlickerScrollBarUI;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Plugin(
		name="Exogenous Trace Explorer",
		level=PluginLevel.NightlyBuild,
		returnLabels= {"Exogenous Annotated Explorer UI"},
		returnTypes = { JComponent.class },
		userAccessible = true, 
		parameterLabels = { "" }
)
@Visualizer
@EqualsAndHashCode(callSuper=false)
public class ExogenousTraceView extends JPanel {
	
	private static final long serialVersionUID = 7656143847405225511L;
	private XTrace selectedEndogenous;
	private EndoTraceListener listenerEndogenous;
	
	@NonNull @Getter ExogenousAnnotatedLog source;
	@NonNull @Getter UIPluginContext context;
	
	
	@Builder.Default ProMSplitPane leftRight = new ProMSplitPane(ProMSplitPane.HORIZONTAL_SPLIT);
	@Builder.Default ProMSplitPane rightTopBottom = new ProMSplitPane(ProMSplitPane.VERTICAL_SPLIT);
	
	@Builder.Default @Setter @Getter ChartPanel traceOverviewChart = null; 
	@Builder.Default private int lastEventSliceTouched = 0;
	@Builder.Default private boolean lastEventSliceHiglighted = false;
	
	@Builder.Default @Getter private ExogenousTraceViewJChartFilterPanel traceBreakdownView = new ExogenousTraceViewJChartFilterPanel();
	private EventFilter eventfilter;
	
	
	public ExogenousTraceView setup() {
		this.setAlignmentX(JPanel.CENTER_ALIGNMENT);
		this.setAlignmentY(JPanel.CENTER_ALIGNMENT);
		this.setBackground(Color.black);
		this.setVisible(true);
		this.setMinimumSize(new Dimension(1280, 1024));
		context.getProgress().setIndeterminate(true);
//		layout managers
		this.rightTopBottom.setLayout(new GridLayout(0,1));
		this.rightTopBottom.validate();
		this.leftRight.setLayout(new GridLayout(1,2));
		this.leftRight.setResizeWeight(0);
		this.leftRight.validate();
		this.setLayout(new GridLayout(1,1));
//		new setup, trace list on left, spliting to
//		new spliter (vertically) with trace vis on top and breakdown bellow
		this.leftRight.setLeftComponent(this.buildTraceList());
		this.leftRight.setRightComponent(this.rightTopBottom);
		this.rightTopBottom.setTopComponent(this.buildTraceVis());
		this.rightTopBottom.setBottomComponent(this.buildTraceBreakdown());
		add(this.leftRight);
		this.validate();
		return this;
	}
	
	
	public void stylePanel(JComponent pane) {
		pane.setBackground(Color.LIGHT_GRAY);
		pane.setBorder(BorderFactory.createEmptyBorder());
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		pane.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		pane.setAlignmentY(JPanel.CENTER_ALIGNMENT);
	}
	
	public void stylePanel(JComponent pane, boolean layout) {
		pane.setBackground(Color.LIGHT_GRAY);
		pane.setBorder(BorderFactory.createEmptyBorder());
		if (layout) {
			pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		}
		pane.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		pane.setAlignmentY(JPanel.CENTER_ALIGNMENT);
	}
	
	public void styleChart(JComponent chart) {
		chart.setBackground(Color.LIGHT_GRAY);
		chart.setBorder(BorderFactory.createEmptyBorder());
		chart.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		chart.setAlignmentY(JPanel.CENTER_ALIGNMENT);
	}
	
	
	public JComponent buildTraceVis() {
		JPanel topPanel = new JPanel();
		JLabel label = new JLabel("Select a trace on the left.");
		label.setVisible(true);
		topPanel.add(label);
		this.stylePanel(topPanel);
		topPanel.setLayout(new GridLayout(0,1));
		topPanel.validate();
		topPanel.setMinimumSize(new Dimension(800,400));
		return topPanel;
	}
	
	public JComponent buildTraceVis(XTrace endo) {
//		panels to show
		JPanel topPanel = new JPanel();
		JLabel progress = new JLabel();
		progress.setVisible(true);
//		setup graph builder in background
		TraceVisOverviewChart.builder().source(this).target(topPanel).endo(endo).progress(progress).build().execute();
		
//		style panels
		this.stylePanel(topPanel);
		topPanel.setLayout(new GridLayout(0,1));
		topPanel.validate();
		topPanel.setMinimumSize(new Dimension(800,400));
		return topPanel;
	}
	
	public void updateTraceVis() {
		this.rightTopBottom.setTopComponent(this.buildTraceVis());
		this.rightTopBottom.validate();
		this.validate();
	}
	
	public void updateTraceVis(XTrace endo) {
		this.rightTopBottom.setTopComponent(this.buildTraceVis(endo));
		this.rightTopBottom.validate();
		this.validate();
	}
	
	public JComponent buildTraceBreakdown() {
		JPanel bottomLeft = new JPanel();
		JLabel label = new JLabel("Select a Trace from the left.");
		bottomLeft.add(label);
		this.stylePanel(bottomLeft);
		bottomLeft.setLayout(new GridLayout(0,1));
		bottomLeft.validate();
		return bottomLeft;
	}
	
	public void buildTraceBreakdown(XTrace endo) {
//		setup progresser while worker does it job
		JPanel progressPane = new JPanel();
		progressPane.add(new JLabel("Building Breakdown :: "));
		JProgressBar progress = new JProgressBar();
		progressPane.add(progress);
		this.stylePanel(progressPane,false);
		this.rightTopBottom.setBottomComponent(progressPane);
//		start worker to build full breakdown
		JPanel panel =  new JPanel();
		TraceVisTraceBreakdownCharts.builder()
			.source(this)
			.endo(endo)
			.panel(panel)
			.progress(progress)
			.build()
			.execute();
	}
	
	public JComponent buildSpacer(int width) {
		JPanel spacer = new JPanel();
		this.stylePanel(spacer);
		spacer.setMinimumSize(new Dimension(width,25));
		spacer.setMaximumSize(new Dimension(width,25));
		return spacer;
	}
	
	public void updateTraceBreakdown() {
		this.rightTopBottom.setBottomComponent(this.buildTraceBreakdown());
		this.rightTopBottom.validate();
		this.validate();
	}
	
	public void updateTraceBreakdown(XTrace endo) {
		this.buildTraceBreakdown(endo);
	}
	
	public JComponent buildTraceList () {
		JPanel bottomRight = new JPanel();
//		create stylish trace selector
		ExoTraceBuilder builder = new ExoTraceBuilder();
		ProMTraceList<XTrace> traceView = new ProMTraceList<XTrace>(
				builder
		);
		traceView.addAll(this.source.getEndogenousLog());
		traceView.addTraceClickListener(
				new endoClickListener(this, traceView)
		);
//		style trace list
		JScrollBar bar = traceView.getScrollPane().getVerticalScrollBar();
		bar.setUI(new SlickerScrollBarUI(bar, Color.LIGHT_GRAY, Color.GRAY,Color.DARK_GRAY, 4, 12));
		bar = traceView.getScrollPane().getHorizontalScrollBar();
		bar.setUI(new SlickerScrollBarUI(bar, Color.LIGHT_GRAY, Color.GRAY,Color.DARK_GRAY, 4, 12));
		traceView.getScrollPane().validate();
		traceView.validate();
//		ordering of elements and styling
		bottomRight.add(
				new JLabel("Avaliable Endogenous Traces:") 
				{{ 
					setAlignmentY(LEFT_ALIGNMENT);
					setAlignmentX(LEFT_ALIGNMENT);
				}}
		);
//		build and style panel
		bottomRight.add(traceView);
		this.stylePanel(bottomRight);
		bottomRight.setMinimumSize(new Dimension(300,800));
		bottomRight.validate();
		return bottomRight;
	}
	
	public JComponent getComponent() {
		return this;
	}
	
	public void highlightEventSlice(XTrace trace, int eventIndex) {
		if (this.traceOverviewChart != null) {
			if (!this.lastEventSliceHiglighted) {
				this.traceOverviewChart.getChart().getXYPlot().getRenderer(0).setSeriesPaint(eventIndex, Color.orange);
				this.traceOverviewChart.getChart().getXYPlot().getRenderer(0).setSeriesStroke(eventIndex, new BasicStroke(5));
				this.traceOverviewChart.validate();
				this.lastEventSliceHiglighted = true;
				this.lastEventSliceTouched = eventIndex;
			} else {
				this.traceOverviewChart.getChart().getXYPlot().getRenderer(0).setSeriesPaint(this.lastEventSliceTouched, new Color(0F, 0F, 0F, 0.33F));
				this.traceOverviewChart.getChart().getXYPlot().getRenderer(0).setSeriesStroke(this.lastEventSliceTouched, new BasicStroke(1));
				if (eventIndex != this.lastEventSliceTouched) {
					this.traceOverviewChart.getChart().getXYPlot().getRenderer(0).setSeriesPaint(eventIndex, Color.orange);
					this.traceOverviewChart.getChart().getXYPlot().getRenderer(0).setSeriesStroke(eventIndex, new BasicStroke(5));
					this.lastEventSliceHiglighted = true;
				} else {
					this.lastEventSliceHiglighted = false;
				}
				this.traceOverviewChart.validate();
				this.lastEventSliceTouched = eventIndex;
			}
		}
	}
	
	public boolean setSelectEndogenous(XTrace select, EndoTraceListener listener) {
		if (this.selectedEndogenous == null) {
			this.selectedEndogenous = select;
			this.listenerEndogenous = listener;
			updateTraceBreakdown(this.selectedEndogenous);
			updateTraceVis(this.selectedEndogenous);
			return true;
		} else if ( this.selectedEndogenous.getAttributes().get("concept:name") == select.getAttributes().get("concept:name")){
			this.selectedEndogenous = null;
			this.listenerEndogenous.resetClicked();
			updateTraceBreakdown();
			updateTraceVis();
			this.listenerEndogenous = null;
			return false;
		} else {
			this.selectedEndogenous = select;
			this.listenerEndogenous.resetClicked();
			this.listenerEndogenous = listener;
			updateTraceBreakdown(this.selectedEndogenous);
			updateTraceVis(this.selectedEndogenous);
			return true;
		}
	}
	
	public boolean setSelectEndogenous(XTrace select) {
		if (this.selectedEndogenous == null) {
			this.selectedEndogenous = select;
			updateTraceBreakdown(this.selectedEndogenous);
			updateTraceVis(this.selectedEndogenous);
			return true;
		} else if ( this.selectedEndogenous.getAttributes().get("concept:name") == select.getAttributes().get("concept:name")){
			this.selectedEndogenous = null;
			updateTraceBreakdown();
			updateTraceVis();
			return false;
		} else {
			this.selectedEndogenous = select;
			updateTraceBreakdown(this.selectedEndogenous);
			updateTraceVis(this.selectedEndogenous);
			return true;
		}
	}
	
	public double getEventTimestampMillis(XEvent ev) {
		return ((XAttributeTimestamp) ev.getAttributes().get("time:timestamp")).getValueMillis();
	}
	
	public double getExoEventValue(XEvent ev) {
		return Double.parseDouble(ev.getAttributes().get("exogenous:value").toString());
	}
	
	public void updateTraceBreakdownEvent(XEvent ev, int eventIndex) {
		
		if (ev != null) {
			if (this.eventfilter != null) {
				this.traceBreakdownView.removeFilter(this.eventfilter);
			}
			this.eventfilter = EventFilter
					.builder()
					.eventIndex(eventIndex)
					.build();
			
			this.traceBreakdownView.filter( 
					this.eventfilter
			);
		} else {
			this.traceBreakdownView.removeFilter(this.eventfilter);
			this.eventfilter = null;
		}
		
		this.rightTopBottom.validate();
	}
	
	public JPanel buildIndividualEventBreakdown(XEvent ev) {
//		create panel for breakdown
		TraceVisEventChart chartbuilder = TraceVisEventChart.builder()
				.log(this.source)
				.endogenous(ev)
				.build();
		chartbuilder.setup();
		stylePanel(chartbuilder.getView());
		return chartbuilder.getView();
	}
	
 	public static JLabel createLeftAlignedLabel(String text, Boolean bold, int fontSize) {
 		JLabel label = new JLabel(text, SwingConstants.LEADING);
		label.setFont(new Font("Times new Roman", bold ? Font.BOLD : null, fontSize));
		label.setBackground(Color.DARK_GRAY);
		label.setHorizontalAlignment(JLabel.LEFT);
		label.setAlignmentX(JPanel.RIGHT_ALIGNMENT);
		return label;
 	}
 	
 	
// 	interfaces
 	public static class endoClickListener implements ClickListener<XTrace> {
		
		private ExogenousTraceView source;
		private ProMTraceList<XTrace> controller;
		private ExoTraceBuilder builder;
		
		public endoClickListener(ExogenousTraceView source, ProMTraceList<XTrace> controller) {
			this.source = source;
			this.controller = controller;
			this.builder = (ExoTraceBuilder) controller.getTraceBuilder();
		}

		@Override
		public void traceMouseDoubleClicked(XTrace trace, int traceIndex, int eventIndex, MouseEvent e) {
			boolean changed = this.source.setSelectEndogenous(trace);
			int target = changed ? traceIndex: -1;
			this.controller.setWedgeBuilder(WedgeBuilderFactory.createTraceHighlight(target));
			this.builder.selection.clear();
			this.source.getTraceBreakdownView().clearFilters();
			if (changed) {
				this.builder.selection.add(target);
			}
			this.controller.updateUI();
			this.controller.validate();
		}

		@Override
		public void traceMouseClicked(XTrace trace, int traceIndex, int eventIndex, MouseEvent e) {
			boolean changed = this.source.setSelectEndogenous(trace);
			int target = changed ? traceIndex: -1;
			this.controller.setWedgeBuilder(WedgeBuilderFactory.createTraceHighlight(target));
			this.builder.selection.clear();
			this.source.getTraceBreakdownView().clearFilters();
			if (changed) {
				this.builder.selection.add(target);
			} 
			this.controller.updateUI();
			this.controller.validate();
		}
		
	}

	
}
