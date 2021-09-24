package org.qut.exogenousaware.gui.panels;

import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeriesCollection;
import org.qut.exogenousaware.gui.panels.ExogenousEnhancementDotPanel.GuardExpressionHandler;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class EnhancementAllGraph {

	@NonNull XYSeriesCollection graphData;
	@NonNull List<Map<String,Object>> dataState;
	@NonNull Boolean hasExpression;
	@NonNull @Getter String title;
	@NonNull String xlabel;
	@NonNull String ylabel;
	
	@Default GuardExpressionHandler expression = null;
	@Default Color passColour = new Color(0,102,51,25); 
	@Default Color failColour = new Color(128,0,0,25);
	@Default Color nullColour = new Color(0,0,0,25);
	@Default ChartPanel graph = null;
	
	public ChartPanel make() {
//		make dummy chart
		JFreeChart chart = ChartFactory.createXYLineChart(
				this.title, 
				this.xlabel, 
				this.ylabel, 
				this.graphData
		);
//		setup renderers for each series
		XYPlot plot = chart.getXYPlot();
		XYLineAndShapeRenderer exoRender = new XYLineAndShapeRenderer(true, false);
		exoRender.setUseFillPaint(true);
		for(int i=0; i < this.graphData.getSeriesCount(); i++) {
			Color paintColor = 
					this.hasExpression ?
					(	this.expression.evaluate(this.dataState.get(i)) ? 
						this.passColour : 
						this.failColour
					) : this.nullColour ;
			exoRender.setSeriesPaint(i, paintColor);
			exoRender.setSeriesShape(i, new Ellipse2D.Double(-2,-2,4,4));
			exoRender.setSeriesShapesVisible(i, true);
			exoRender.setSeriesFillPaint(i, paintColor);
			exoRender.setSeriesVisible(i, true);
			exoRender.setSeriesVisibleInLegend(i, false);
			exoRender.setSeriesLinesVisible(i, true);
		}
		plot.setRenderer(exoRender);
		chart.removeLegend();
//		remake the graph 
		this.graph = new ChartPanel(
				chart
		);
		return this.graph;
	}
	
}
