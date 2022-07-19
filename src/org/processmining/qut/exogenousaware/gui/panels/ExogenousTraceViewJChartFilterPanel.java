package org.processmining.qut.exogenousaware.gui.panels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
	private List<ChartFilter> sfilters = new ArrayList<ChartFilter>();
	
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
			if (filter instanceof SlicerFilter) {
				this.sfilters.add(filter);
			} else {
				this.filters.add(filter);
			}
				
			runFilterCheck();
			
			return true;
		} catch (Exception e) {
			
			return false;
		}
	}
	
	public boolean removeFilter(ChartFilter filter) {
		
		if (filter instanceof SlicerFilter) {
			this.sfilters.remove(filter);
		} else {
			this.filters.remove(filter);
		}
		
		runFilterCheck();
		
		return true;
	}
	
	public void runFilterCheck() {
//		check event filters separately 
		List<ChartFilter> evFilters = this.filters.stream().filter(e -> e instanceof EventFilter).collect(Collectors.toList());
		List<ChartFilter> otFilters = this.filters.stream().filter(e -> !(e instanceof EventFilter)).collect(Collectors.toList());
		
//		check all filters that must be true
		for(ChartHolder chart: this.charts) {
			if (evFilters.size() < 1) {
				chart.panel.setVisible(true);
			} else {
				boolean visible = true;
				for (ChartFilter efilter : evFilters) {
					visible = efilter.shouldFilter(chart) && visible;
				}
				chart.panel.setVisible(visible);
			}
			
		}

//		check for at least one filter allowing for chart
		for(ChartHolder chart: this.charts.stream().filter(e -> e.panel.isVisible()).collect(Collectors.toList())) {
			boolean visible;
			if (otFilters.size() < 1) {
				visible = true;
			} else {
				visible = false;
			}
			for(ChartFilter cfilter: otFilters) {
				visible = cfilter.shouldFilter(chart) || visible;
			}
			chart.getPanel().setVisible(visible);
			
			if (visible) {
				if (this.sfilters.size() > 0) {
					Set<String> keys = chart.getSlicers().keySet();
					for(String key : keys) {
						int tkey = chart.getSlicers().get(key);
						chart.getChart().getXYPlot().getRenderer().setSeriesVisible(tkey, false);
					}
					for(ChartFilter cfilter: this.sfilters) {
						cfilter.filter(chart);
					}
				} else {
					Set<String> keys = chart.getSlicers().keySet();
					for(String key : keys) {
						int tkey = chart.getSlicers().get(key);
						chart.getChart().getXYPlot().getRenderer().setSeriesVisible(tkey, true);
					}
				}
			}
		}
	}
	
	public void clearFilters() {
		this.filters.clear();
		this.sfilters.clear();
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
		@Default @Getter Map<String, Integer> slicers = new HashMap();
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
	
	@Builder
	public static class SlicerFilter implements ChartFilter {
		
		@NonNull String slicer;
		
		public boolean filter(ChartHolder chart) {
			Set<String> keys = chart.getSlicers().keySet();
			if (keys.contains(this.slicer)) {
				int tkey = chart.getSlicers().get(this.slicer);
				chart.getChart().getXYPlot().getRenderer(0).setSeriesVisible(tkey, true);
			}
			return true;
		}

		public boolean shouldFilter(ChartHolder chart) {
			return chart.getExoPanel().equals(this.slicer);
		}
		
		
		@Override
		public boolean equals(Object o) {
			
//			if it is the same object then return
			if (o == this) {
				return true;
			}
			
//			check for same instance type
			if (!(o instanceof SlicerFilter)) {
				return false;
			}
			
			SlicerFilter ef = (SlicerFilter) o;
			
			return this.slicer.equals(ef.slicer);
		}
	}
	

}
