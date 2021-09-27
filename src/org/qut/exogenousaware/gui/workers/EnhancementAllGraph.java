package org.qut.exogenousaware.gui.workers;

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
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeriesCollection;
import org.qut.exogenousaware.gui.panels.ExogenousEnhancementDotPanel.GuardExpressionHandler;

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
	
	@Default GuardExpressionHandler expression = null;
	@Default Color passColour = new Color(0,102,51,75); 
	@Default Color failColour = new Color(128,0,0,75);
	@Default Color nullColour = new Color(255,255,255,75);
	@Default ChartPanel graph = null;
	@Default @Getter JPanel main = new JPanel();
	@Default JProgressBar progress = new JProgressBar();
	
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
		panel.add(new ChartPanel( graph.getChart()), BorderLayout.CENTER);
		return panel;
	}
	
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
		plot.setBackgroundPaint(Color.BLACK);
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(false);
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
			this.progress.setValue(this.progress.getValue()+1);
		}
		plot.setRenderer(exoRender);
		chart.removeLegend();
//		remake the graph 
		this.graph = new ChartPanel(
				chart
		);
		this.graph.setMaximumSize(new Dimension(400,600));
		return this.graph;
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
