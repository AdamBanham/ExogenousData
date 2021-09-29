package org.qut.exogenousaware.gui.workers;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.qut.exogenousaware.gui.panels.Colours;
import org.qut.exogenousaware.gui.panels.ExogenousEnhancementDotPanel.GuardExpressionHandler;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class EnhancementMedianGraph extends SwingWorker<JPanel, String> {
	
	@NonNull XYSeriesCollection graphData;
	@NonNull List<Map<String,Object>> dataState;
	@NonNull Boolean hasExpression;
	@NonNull @Getter String title;
	@NonNull String xlabel;
	@NonNull String ylabel;
	
	@Default boolean useGroups = false;
	@Default List<Integer> groups = null;
	@Default GuardExpressionHandler expression = null;
	@Default Color passColour = new Color(0,102,51,255); 
	@Default Color failColour = new Color(128,0,0,255);
	@Default Color nullColour = new Color(255,255,255,255);
	@Default ChartPanel graph = null;
	@Default double segmentInterval = 0.15;
	@Default @Getter JPanel main = new JPanel();
	@Default JProgressBar progress = new JProgressBar();
	
	public EnhancementMedianGraph setup() {
		this.main.setLayout(new BorderLayout(50,50));
		this.main.add(progress, BorderLayout.CENTER);
		this.main.setMaximumSize(new Dimension(400,600));
		this.progress.setVisible(true);
		this.progress.setValue(0);
		this.progress.setMaximum(this.graphData.getSeriesCount());
		return this;
	}
	
	public ChartPanel make() {
//		for each time point (rounded to .25 of an hour) find median
		Map<Double, List<Double>> trueMedians = new HashMap<Double, List<Double>>();
		Map<Double, List<Double>> falseMedians = new HashMap<Double, List<Double>>();
		Map<Double, List<Double>> nullMedians = new HashMap<Double, List<Double>>();
		int seriescount = 0;
		for(XYSeries series: (List<XYSeries>) this.graphData.getSeries()) {
			Map<Double, List<Double>> medians = chooseMap(seriescount, trueMedians, falseMedians, nullMedians);
			for(int i=0;i < series.getItemCount();i++) {
				double x = (double) series.getX(i);
				x = x - (x % segmentInterval);
				double y = (double) series.getY(i);				
				if (!medians.containsKey(x)) {
					medians.put(x, new ArrayList<Double>());
				}
				medians.get(x).add(y);
			}
			seriescount++;
			this.progress.setValue(seriescount);
		}
//		we want three collections each with the following
//		(1) median line
//		(2) interval series
		XYLineAndShapeRenderer renderers = new XYLineAndShapeRenderer(true, false);
		renderers.setUseFillPaint(true);
		XYSeriesCollection dataset = new XYSeriesCollection();
//		create median series
		seriescount = 0;
		XYSeries series = new XYSeries("true",true);
		createMedianSeries(series, trueMedians);
		dataset.addSeries(series);
		createXYLineRender(renderers, passColour, seriescount);
		seriescount++;
		series = new XYSeries("false",true);
		createMedianSeries(series, falseMedians);
		dataset.addSeries(series);
		createXYLineRender(renderers, failColour, seriescount);
		seriescount++;
		series = new XYSeries("non",true);
		createMedianSeries(series, nullMedians);
		dataset.addSeries(series);
		createXYLineRender(renderers, nullColour, seriescount);
		seriescount++;
//		create intervals 
		YIntervalSeriesCollection intervalDataset = new YIntervalSeriesCollection();
		YIntervalSeries seriesInt = new YIntervalSeries("true");
		createIntervalSeries(seriesInt, trueMedians, dataset.getSeries(0));
		intervalDataset.addSeries(seriesInt);
		seriesInt = new YIntervalSeries("false");
		createIntervalSeries(seriesInt, falseMedians, dataset.getSeries(1));
		intervalDataset.addSeries(seriesInt);
		seriesInt = new YIntervalSeries("null");
		createIntervalSeries(seriesInt, nullMedians, dataset.getSeries(2));
		intervalDataset.addSeries(seriesInt);
//		make dummy chart
		JFreeChart chart = ChartFactory.createXYLineChart(
				this.title, 
				this.xlabel, 
				this.ylabel, 
				intervalDataset
		);
//		setup renderers for each series
		XYPlot plot = chart.getXYPlot();
		DeviationRenderer renderer = new DeviationRenderer(true, false);
        renderer.setSeriesStroke(0, new BasicStroke(3.0f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        renderer.setSeriesStroke(1, new BasicStroke(3.0f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        renderer.setSeriesStroke(2, new BasicStroke(3.0f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        renderer.setSeriesFillPaint(0, passColour);
        renderer.setSeriesPaint(0, passColour);
        renderer.setSeriesFillPaint(1, failColour);
        renderer.setSeriesPaint(1, failColour);
        renderer.setSeriesFillPaint(2, nullColour);
        renderer.setSeriesPaint(2, nullColour);
        plot.setRenderer(renderer);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setBackgroundPaint(Colours.CHART_BACKGROUND);
//		chart.removeLegend();
//		remake the graph 
		this.graph = new ChartPanel(
				chart
		);
		return this.graph;
	}
	
	public Map<Double, List<Double>> chooseMap(int series, Map<Double, List<Double>> truth, Map<Double, List<Double>> failure, Map<Double, List<Double>> fall){
		if (this.useGroups && this.groups != null) {
			int group = this.groups.get(series);
			if (group == 0) {
				return failure;
			} else if (group == 1) {
				return truth;
			} else {
				return fall;
			}
		} else {
			return this.hasExpression ?
					(	this.expression.evaluate(this.dataState.get(series)) ? 
						truth : 
						failure
					) : fall ;
		}
	}
	
	
	static public void createMedianSeries(XYSeries series, Map<Double, List<Double>> data) {
		for(Entry<Double, List<Double>> entry : data.entrySet()) {
			int middle =entry.getValue().size() / 2;
			Object[] values = entry.getValue().toArray();
			Arrays.sort(values);
			double median =0;
			if (middle%2 == 1) {
				median = ((double) values[middle]);
			} else if (middle == 0) {
				median = (double) values[middle];
			} else { 
				median = ((double) values[middle-1] + (double) values[middle]) / 2.0;
			}
			series.add((double) entry.getKey(), median);
		}
	}
	
	public void createIntervalSeries(YIntervalSeries series, Map<Double, List<Double>> data, XYSeries medians) {
		StandardDeviation std = new StandardDeviation();
		int medianCounter = 0;
		for(Entry<Double, List<Double>> entry : data.entrySet()) {
			double mean = entry.getValue().stream()
					.reduce(0.0, (c,n) -> c+n );
			mean = mean/entry.getValue().size();
			final double fmean = mean;
			double stdValue = entry.getValue().stream()
					.reduce(0.0, (c,u) -> { return c+Math.pow((u-fmean),2);});
			stdValue = stdValue / entry.getValue().size();
			stdValue = Math.sqrt(stdValue);
			double median = (double) medians.getY(medianCounter);
			series.add( (double) medians.getX(medianCounter), median, median-stdValue, median+stdValue);
			medianCounter++;
		}
	}
	
	public void createXYLineRender(XYLineAndShapeRenderer exoRender, Color colour, int seriescount) {
		exoRender.setSeriesPaint(seriescount, colour);
		exoRender.setSeriesShape(seriescount, new Ellipse2D.Double(-2,-2,4,4));
		exoRender.setSeriesShapesVisible(seriescount, true);
		exoRender.setSeriesFillPaint(seriescount, colour);
		exoRender.setSeriesVisible(seriescount, true);
//		exoRender.setSeriesVisibleInLegend(seriescount, false);
		exoRender.setSeriesLinesVisible(seriescount, true);
	}

	public JPanel getNewChart() {
		JPanel panel = new JPanel();
		panel.setMaximumSize(new Dimension(400,600));
		panel.setLayout(new BorderLayout(50,50));
		panel.add(new ChartPanel( graph.getChart()), BorderLayout.CENTER);
		return panel;
	}
	
	@Override
    protected void done() {
		this.progress.setVisible(false);
		this.main.add(this.graph, BorderLayout.CENTER);
	}

	protected JPanel doInBackground() throws Exception {
		return this.make();
	}
}
