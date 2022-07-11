package org.processmining.qut.exogenousaware.gui.panels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jfree.chart.JFreeChart;
import org.processmining.framework.util.ui.widgets.ProMScrollPane;
import org.processmining.qut.exogenousaware.gui.styles.PanelStyler;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

public class ExogenousTraceViewJChartFilterPanel {

	// charts
	private List<ChartHolder> charts = new ArrayList<ChartHolder>();
	
	// filters
	private List<ChartFilter> filters = new ArrayList<ChartFilter>();
	
	// visibles
	private JPanel view;
	private JScrollPane scroller;
	
	
	public ExogenousTraceViewJChartFilterPanel() {
		setup();
	}
	
	public void setup() {
		view = new JPanel();
		PanelStyler.StylePanel(view, false);
		view.setLayout(new BoxLayout(view, BoxLayout.Y_AXIS));
		scroller = new ProMScrollPane(view);
	}
	
	public JScrollPane getScroller() {
		return scroller;
	}
	
	public void add() {
		
	}
	
	public boolean addChart(ChartHolder charter) {
		try {
			charts.add(charter);
			this.view.add(charter.getPanel());
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public boolean filter(ChartFilter filter) {
		try {
			this.filters.add(filter);
			for(ChartHolder chart: this.charts) {
				boolean visible = chart.getPanel().isVisible();
				visible = filter.shouldFilter(chart) && visible;
				chart.getPanel().setVisible(visible);
			}
			return true;
		} catch (Exception e) {
			
			return false;
		}
	}
	
	public boolean removeFilter(ChartFilter filter) {
		
		this.filters.remove(filter);
		
		for(ChartHolder chart: this.charts) {
			boolean visible = true;
			for(ChartFilter cfilter: this.filters) {
				visible = cfilter.shouldFilter(chart) && visible;
			}
			chart.getPanel().setVisible(visible);
		}
		return true;
	}
	
	public void clearFilters() {
		this.filters.clear();
		for(ChartHolder chart: this.charts) {
			chart.getPanel().setVisible(true);
			int series = chart.getChart().getXYPlot().getRendererCount();
			for(int i=0;i < series; i++) {
				chart.getChart().getXYPlot().getRenderer().setSeriesVisible(i, true);
			}
		}
	}
	
	public boolean clear() {
		try {
			this.view = new JPanel();
			PanelStyler.StylePanel(view, false);
			this.view.setLayout(new BoxLayout(view, BoxLayout.Y_AXIS));
			this.charts.clear();
			this.filters.clear();
			this.scroller.setViewportView(this.view);
			return true;
		} catch (Exception e) {
			
			return false;
		}
	}
	
	@Builder
	public static class ChartHolder {
		
		@Getter int eventIndex;
		@Getter String exoPanel;
		@Getter Map<String, Integer> slicers;
		@Getter JFreeChart chart;
		@Getter JPanel panel;
	}
	
	abstract public interface ChartFilter {
		
		public boolean filter(ChartHolder chart);
		
		public boolean shouldFilter(ChartHolder chart);
	}
	
	@Builder
	public static class EventFilter implements ChartFilter {
		
		@Default int eventIndex = -1;

		public boolean filter(ChartHolder chart) {
			System.out.println("checking for :: "+this.eventIndex +" :: "+chart.getEventIndex());
			if (chart.getEventIndex() != this.eventIndex) {
				chart.getPanel().setVisible(false);
			} else {
				chart.getPanel().setVisible(true);
			}
			return true;
		}

		public boolean shouldFilter(ChartHolder chart) {
			return chart.getEventIndex() == this.eventIndex;
		}
		
		@Override
		public boolean equals(Object o) {
			
//			if it is the same object then return
			if (o == this) {
				return true;
			}
			
//			check for same instance type
			if (!(o instanceof EventFilter)) {
				return false;
			}
			
			EventFilter ef = (EventFilter) o;
			
			return Integer.compare(this.eventIndex, ef.eventIndex) == 0;
		}
	}
	
	@Builder
	public static class PanelFilter implements ChartFilter {
		
		@NonNull String exoPanel;
		
		public boolean filter(ChartHolder chart) {
			if (!chart.getExoPanel().equals(this.exoPanel)) {
				chart.getPanel().setVisible(false);
			} else {
				chart.getPanel().setVisible(true);
			}
			return true;
		}

		public boolean shouldFilter(ChartHolder chart) {
			return chart.getExoPanel().equals(this.exoPanel);
		}
		
		@Override
		public boolean equals(Object o) {
			
//			if it is the same object then return
			if (o == this) {
				return true;
			}
			
//			check for same instance type
			if (!(o instanceof PanelFilter)) {
				return false;
			}
			
			PanelFilter ef = (PanelFilter) o;
			
			return this.exoPanel.equals(ef.exoPanel);
		}
		
	}
	

}
