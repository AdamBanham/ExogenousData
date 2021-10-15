package org.processmining.qut.exogenousaware.gui.workers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.processmining.qut.exogenousaware.data.dot.GuardExpressionHandler;
import org.processmining.qut.exogenousaware.gui.panels.Colours;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class EnhancementAllGraph extends SwingWorker<JPanel, String>{

	@NonNull XYSeriesCollection graphData;
	@NonNull List<Map<String,Object>> dataState;
	@NonNull Boolean hasExpression;
	@NonNull @Getter String title;
	@NonNull String xlabel;
	@NonNull String ylabel;
	
	@Default boolean useGroups = false;
	@Default List<Integer> groups = null;
	@Default GuardExpressionHandler expression = null;
	@Default Color passColour = Colours.getGraphPaletteColour(1); 
	@Default Color failColour = Colours.getGraphPaletteColour(7);
	@Default Color nullColour = Colours.getGraphPaletteColour(4);
	@Default ChartPanel graph = null;
	@Default @Getter JPanel main = new JPanel();
	@Default JProgressBar progress = new JProgressBar();
	@Default double lowerDomainBound = Double.MAX_VALUE;
	@Default double upperDomainBound = Double.MIN_VALUE;
	@Default double lowerRangeBound = Double.MAX_VALUE;
	@Default double upperRangeBound = Double.MIN_VALUE;
	
	public EnhancementAllGraph setup() {
		this.main.setLayout(new BorderLayout(50,50));
		this.main.add(progress, BorderLayout.CENTER);
		this.main.setMaximumSize(new Dimension(400,600));
		this.progress.setVisible(true);
		this.progress.setValue(0);
		this.progress.setMaximum(this.graphData.getSeriesCount());
		return this;
	}
	
	public JPanel getNewChart() {
		JPanel panel = new JPanel();
		panel.setMaximumSize(new Dimension(400,600));
		panel.setLayout(new BorderLayout(50,50));
		if (this.isDone()) {
			panel.add(new ChartPanel( graph.getChart()), BorderLayout.CENTER);
		}
		return panel;
	}
	
	public ChartPanel make() {
//		check graph data to find bounds
		try {
		for(Object item: this.graphData.getSeries()) {
			if (item.getClass().equals(XYSeries.class)) {
				XYSeries series = (XYSeries) item;
				double lowx = series.getMinX();
				double highx = series.getMaxX();
				double lowy = series.getMinY();
				double highy = series.getMaxY();
//				check x bounds
				this.lowerDomainBound = lowx < this.lowerDomainBound ? lowx : this.lowerDomainBound;
				this.upperDomainBound = highx >= this.upperDomainBound ? highx : this.upperDomainBound;
//				check y bounds
				this.lowerRangeBound = lowy < this.lowerRangeBound ? lowy : this.lowerRangeBound;
				this.upperRangeBound = highy >= this.upperRangeBound ? highy : this.upperRangeBound;
			}
		}
		
//		make dummy chart
		JFreeChart chart = ChartFactory.createXYLineChart(
				this.title, 
				this.xlabel, 
				this.ylabel, 
				this.graphData
		);
//		setup renderers for each series
		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Colours.CHART_BACKGROUND);
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(false);
		XYLineAndShapeRenderer exoRender = new XYLineAndShapeRenderer(true, false);
		exoRender.setUseFillPaint(true);
		for(int i=0; i < this.graphData.getSeriesCount(); i++) {
			Color paintColor = this.findSeriesColor(i);
			exoRender.setSeriesPaint(i, paintColor);
			exoRender.setSeriesShape(i, new Ellipse2D.Double(-2,-2,4,4));
			exoRender.setSeriesShapesVisible(i, true);
			exoRender.setSeriesFillPaint(i, paintColor);
			exoRender.setSeriesVisible(i, true);
			exoRender.setSeriesVisibleInLegend(i, false);
			exoRender.setSeriesLinesVisible(i, true);
			this.progress.setValue(this.progress.getValue()+1);
		}
		plot.setRenderer(exoRender);
		ValueAxis axis = plot.getDomainAxis();
		axis.setUpperBound(this.upperDomainBound);
		axis.setLowerBound(this.lowerDomainBound);
		axis = plot.getRangeAxis();
		axis.setUpperBound(205.0); // should be this.upperRangeBound
		axis.setLowerBound(45.0); // should be this.lowerRangeBound
		chart.removeLegend();
//		remake the graph 
		this.graph = new ChartPanel(
				chart
		);
		this.graph.setMaximumSize(new Dimension(400,600));
		} 
		catch (Exception e) {
			System.out.println("Something went wrong with bound checking :: " + e.getCause());
			System.out.println(e.getMessage());
		}
		return this.graph;
	}
	
	public Color findSeriesColor(int series) {
		if (this.useGroups && this.groups != null) {
			int group = this.groups.get(series);
			if (group == 0) {
				return this.failColour;
			} else if (group == 1) {
				return this.passColour;
			} else {
				return this.nullColour;
			}
		} else {
			return this.hasExpression ?
					(	this.expression.evaluate(this.dataState.get(series)) ? 
						this.passColour : 
						this.failColour
					) : this.nullColour ;
		}
	}

	protected JPanel doInBackground() throws Exception {
		return make();
	}
	
	@Override
    protected void done() {
		this.progress.setVisible(false);
		this.main.add(this.graph, BorderLayout.CENTER);
	}
	
}
