package org.processmining.qut.exogenousaware.gui.workers;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.apache.commons.math3.stat.StatUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.processmining.qut.exogenousaware.data.dot.GuardExpressionHandler;
import org.processmining.qut.exogenousaware.ds.linear.BestFittingLine;
import org.processmining.qut.exogenousaware.ds.timeseries.sample.TimeSeriesSampling;
import org.processmining.qut.exogenousaware.gui.panels.Colours;
import org.processmining.qut.exogenousaware.gui.workers.helpers.ExogenousObserverGrouper;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class EnhancementMedianGraph extends SwingWorker<JPanel, String> {
	
	
	//params for builder
	@NonNull XYSeriesCollection graphData;
	@NonNull List<Map<String,Object>> dataState;
	@NonNull Boolean hasExpression;
	@NonNull @Getter String title;
	@NonNull String xlabel;
	@NonNull String ylabel;
	
	@Default ShadingType shadingType = ShadingType.STD;
	
	// internal variables
	@Default boolean useGroups = false;
	@Default List<Integer> groups = null;
	@Default ExogenousObserverGrouper grouper = null;
	@Default GuardExpressionHandler expression = null;
	@Default Color passColour = Colours.getGraphPaletteColour(1);
	@Default Color passColourBg = Colours.getGraphPaletteColour(2);
	@Default Color failColour = Colours.getGraphPaletteColour(7);
	@Default Color failColourBg = Colours.getGraphPaletteColour(6);
	@Default Color nullColour = Colours.getGraphPaletteColour(4);
	@Default @Getter ChartPanel graph = null;
	@Default double segmentInterval = 0.05;
	@Default double segmentWindow = 0.2;
	@Default @Getter JPanel main = new JPanel();
	@Default JProgressBar progress = new JProgressBar();
	@Default double lowerDomainBound = Double.MAX_VALUE;
	@Default double upperDomainBound = Double.MIN_VALUE;
	@Default double lowerRangeBound = Double.MAX_VALUE;
	@Default double upperRangeBound = Double.MIN_VALUE;
	@Default @Getter YIntervalSeries trueMedianDataset = null;
	@Default @Getter YIntervalSeries falseMedianDataset = null;
	@Default @Getter YIntervalSeries nullMedianDataset = null;
	
	public EnhancementMedianGraph setup() {
		this.main.setLayout(new BorderLayout(50,50));
		this.main.add(progress, BorderLayout.CENTER);
		this.main.setMaximumSize(new Dimension(400,600));
		this.progress.setVisible(true);
		this.progress.setValue(0);
		this.progress.setMaximum(this.graphData.getSeriesCount());
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public ChartPanel make() {
//		for each time point (rounded to .25 of an hour) find median
		Map<Double, List<Double>> trueMedians = new HashMap<Double, List<Double>>();
		Map<Double, List<Double>> falseMedians = new HashMap<Double, List<Double>>();
		Map<Double, List<Double>> nullMedians = new HashMap<Double, List<Double>>();
		int seriescount = 0;
		List<Double> timeline = TimeSeriesSampling.findTimeline(graphData,250);
		for(XYSeries series: (List<XYSeries>) this.graphData.getSeries()) {
			Map<Double, List<Double>> medians = chooseMap(seriescount, trueMedians, falseMedians, nullMedians);
			TimeSeriesSampling.resampleSeries(series, medians, timeline);
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
		int groupsize = groups.stream()
				.filter(g -> g == 1)
				.collect(Collectors.toList())
				.size();
		YIntervalSeries seriesInt = new YIntervalSeries(
				(this.grouper != null ?
				this.grouper.getGroupName(1) : 
				"true") + 
				" ("+groupsize+")"
		);
		createIntervalSeries(seriesInt, trueMedians, dataset.getSeries(0));
		this.trueMedianDataset = seriesInt;
		intervalDataset.addSeries(seriesInt);
		groupsize = groups.stream()
				.filter(g -> g == 0)
				.collect(Collectors.toList())
				.size();
		seriesInt = new YIntervalSeries(
				(this.grouper != null ?
				this.grouper.getGroupName(0) : 
				"false") + 
				" ("+groupsize+")"
		);
		createIntervalSeries(seriesInt, falseMedians, dataset.getSeries(1));
		this.falseMedianDataset = seriesInt;
		intervalDataset.addSeries(seriesInt);
		seriesInt = new YIntervalSeries("null");
		createIntervalSeries(seriesInt, nullMedians, dataset.getSeries(2));
		this.nullMedianDataset = seriesInt;
		intervalDataset.addSeries(seriesInt);
//		make dummy chart
		JFreeChart chart = ChartFactory.createXYLineChart(
				this.title, 
				this.xlabel, 
				this.ylabel, 
				intervalDataset,
				PlotOrientation.VERTICAL, true,
				true,
				false
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
        renderer.setSeriesFillPaint(0, passColourBg);
        renderer.setSeriesPaint(0, passColour);
        renderer.setSeriesFillPaint(1, failColourBg);
        renderer.setSeriesPaint(1, failColour);
        renderer.setSeriesFillPaint(2, nullColour);
        renderer.setSeriesPaint(2, nullColour);
        plot.setRenderer(renderer);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setBackgroundPaint(Colours.CHART_BACKGROUND);
//      setup a smart value and domain range of interest  
        ValueAxis axis = plot.getDomainAxis();
        axis.setLowerBound(this.lowerDomainBound);
        axis.setUpperBound(this.upperDomainBound);
        axis = plot.getRangeAxis();
        axis.setLowerBound(this.lowerRangeBound); // should be this.lowerRangeBound
        axis.setUpperBound(this.upperRangeBound); // should be this.upperRangeBound
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
	
	
	public void createMedianSeries(XYSeries series, Map<Double, List<Double>> data) {
		for(Entry<Double, List<Double>> entry : data.entrySet()) {
			double median = StatUtils.percentile(entry.getValue().stream().mapToDouble(d -> d).toArray(), 50);
			series.add((double) entry.getKey(), median);
		}
	}
	
	@SuppressWarnings({ "cast", "unchecked" })
	public double findSample(XYSeries series, Double sampleStart, Double sampleEnd) throws ArithmeticException,CloneNotSupportedException {
//		find a pair such that LHS is the lostmost of the sample point (mid) and RHS is the rightmost of the sample point
//		return a value for the mid point of the sample
		double sampleMiddle = sampleStart + ((sampleEnd - sampleStart) / 2.0);
		XYDataItem pairLeft = null;
		XYDataItem pairRight = null;
		List<XYDataItem> items = (List<XYDataItem>) series.createCopy(0, series.getItemCount()-1).getItems();
//		find data points in sample window
		List<XYDataItem> windowedItems = items.stream()
				.filter(it -> it.getX().doubleValue() >= sampleStart)
				.filter(it -> it.getX().doubleValue() <= sampleEnd)
				.collect(Collectors.toList());
//		case (1) no window items
		if (windowedItems.size() > 0) {
//			case (1)(a) we have some datapoints in sample window
//			case (2) we only have data points to one side of the middle
			List<XYDataItem> leftmostItems = windowedItems.stream()
					.filter(it -> it.getX().doubleValue() < sampleMiddle)
					.collect(Collectors.toList());
			List<XYDataItem> rightmostItems = windowedItems.stream()
					.filter(it -> it.getX().doubleValue() > sampleMiddle)
					.collect(Collectors.toList());
			if (leftmostItems.size()==0 || rightmostItems.size()==0) {
//				case(2)(a) need to find either a leftmost or a rightmost to complete pair
				if (leftmostItems.size() == 0) {
//					case(2)(a)(i) find a possible left else throw
					List<XYDataItem> possibleLeft = items.stream()
							.filter(it -> it.getX().doubleValue() < sampleMiddle)
							.collect(Collectors.toList());
					if (possibleLeft.size() > 0) {
						leftmostItems.add(possibleLeft.get(possibleLeft.size()));
					} else {
						
						throw new ArithmeticException("Unable to find a leftmost element for this sample window.");
					}
				} else {
//					case(2)(a)(ii) find a possible right else throw
					List<XYDataItem> possibleRight = items.stream()
							.filter(it -> it.getX().doubleValue() > sampleMiddle)
							.collect(Collectors.toList());
					if (possibleRight.size() > 0) {
						rightmostItems.add(possibleRight.get(0));
					} else {
						throw new ArithmeticException("Unable to find a rightmost element for this sample window.");
					}
				}
			}
//			case(3) we can make a pair
			pairLeft = leftmostItems.get(leftmostItems.size()-1);
			pairRight = rightmostItems.get(0);
		} else {
//			case (1)(b) no data points within window, need to right leftmost and rightmost
			List<XYDataItem> possibleLeft = items.stream()
					.filter(it -> it.getX().doubleValue() < sampleMiddle)
					.collect(Collectors.toList());
			List<XYDataItem> possibleRight = items.stream()
					.filter(it -> it.getX().doubleValue() > sampleMiddle)
					.collect(Collectors.toList());
//			case (4) only items to one side of middle
			if (possibleLeft.size() == 0 || possibleRight.size() == 0) {
				throw new ArithmeticException("Unable to find suitable pair witin sample window");
			} else {
				pairLeft = possibleLeft.get(possibleLeft.size()-1);
				pairRight = possibleRight.get(0);
			}
		}
//		calculate the best fitting line and find y at middle of sample window
		BestFittingLine sampler = BestFittingLine.builder()
				.X(pairLeft.getXValue())
				.X(pairRight.getXValue())
				.Y(pairLeft.getYValue())
				.Y(pairRight.getYValue())
				.build()
				.findSlope()
				.findIntercept();
		return sampler.findY(sampleMiddle);
	}
	
	public void createIntervalSeries(YIntervalSeries series, Map<Double, List<Double>> data, XYSeries medians) {
		for(Entry<Double, List<Double>> entry : data.entrySet()) {
			double stdValue =0;
			double x =entry.getKey();
			double median = StatUtils.percentile(entry.getValue().stream().mapToDouble(d -> d).toArray(), 50);
			double lowy = 0;
			double highy = 200;
			// workflow for std shading
			if (shadingType.equals(ShadingType.STD)) {
			//// find std
				double mean = entry.getValue().stream()
						.reduce(0.0, (c,n) -> c+n );
				mean = mean/entry.getValue().size();
				final double fmean = mean;
				stdValue = entry.getValue().stream()
						.reduce(0.0, (c,u) -> { return c+Math.pow((u-fmean),2);});
				stdValue = stdValue / entry.getValue().size();
				stdValue = Math.sqrt(stdValue);
	//			set series values
				series.add( x, median, median-stdValue, median+stdValue);
//				set bounding values
				lowy = median-(stdValue * 1.05);
				highy = median+(stdValue * 1.05);
			} else if (shadingType.equals(ShadingType.Quartile)) {
				// workflow for q1 (25%) and q3 (75%)
				List<Double> values = entry.getValue();
				if (values.size() > 4) {
					double q1 = StatUtils.percentile(entry.getValue().stream().mapToDouble(d -> d).toArray(), 25);
					double q3 = StatUtils.percentile(entry.getValue().stream().mapToDouble(d -> d).toArray(), 75);
	//				set series values
					series.add( x, median, q1, q3);
	//				set bounding values
					lowy = (q1 * 0.95);
					highy = (q3 * 1.05);
				} else {
//					set series values
					series.add( x, median, median, median);
					lowy = median;
					highy = median;
				}
			} else {
				throw new UnsupportedOperationException("Shading type not implemented :: "+shadingType.getType());
			}

//			check bounds for given point
			try {
			this.lowerDomainBound = x < this.lowerDomainBound ? x : this.lowerDomainBound;
			this.upperDomainBound = x > this.upperDomainBound ? x : this.upperDomainBound;
			this.lowerRangeBound = lowy < this.lowerRangeBound ? lowy : this.lowerRangeBound;
			this.upperRangeBound = highy > this.upperRangeBound ? highy : this.upperRangeBound;
			} catch (Exception e) {
				System.out.println("["+title+"] Error in bound comparision :: "+e.getMessage());
			}
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
		if (this.isDone()) {
			panel.add(new ChartPanel( graph.getChart()), BorderLayout.CENTER);
		}
		return panel;
	}
	
	@Override
    protected void done() {
		this.progress.setVisible(false);
		if (this.graph == null) {
			System.out.println("[EnhancementMedianGraph] ERROR :: '"+title+"' :: Unable to make graph.");
			return;
		}
		if (this.main == null) {
			System.out.println("[EnhancementMedianGraph] ERROR :: '"+title+"' :: Parent panel dereferenced??? cannot add chart panel.");
			return;
		}
		this.main.add(this.graph, BorderLayout.CENTER);
	}

	protected JPanel doInBackground() throws Exception {
		JPanel work = null;
		try {
			work = this.make();
		} catch (Exception e) {
			System.out.println("["+title+"] failed to do work :: "+e.getCause().getMessage());
			e.getCause().printStackTrace();
			e.printStackTrace();
		}
		return work;
	}
	
	public enum ShadingType {
		STD("+- 1 standard deviation"),
		Quartile("q1 vs q3");
		
		private String type;
		
		private ShadingType(String type) {
			this.type = type;
		}
		
		public String getType() {
			return this.type;
		}
	}
}
