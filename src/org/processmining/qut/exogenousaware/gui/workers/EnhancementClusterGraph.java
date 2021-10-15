package org.processmining.qut.exogenousaware.gui.workers;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.apache.commons.lang.NotImplementedException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.IntervalXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.processmining.qut.exogenousaware.data.dot.GuardExpressionHandler;
import org.processmining.qut.exogenousaware.ds.timeseries.sample.TimeSeriesSampling;
import org.processmining.qut.exogenousaware.gui.panels.Colours;
import org.processmining.qut.exogenousaware.ml.clustering.HierarchicalClustering;
import org.processmining.qut.exogenousaware.ml.clustering.HierarchicalClustering.DistanceType;
import org.processmining.qut.exogenousaware.ml.data.FeatureVector;
import org.processmining.qut.exogenousaware.ml.data.FeatureVectorImpl;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class EnhancementClusterGraph extends SwingWorker<JPanel, String> {
	@NonNull XYSeriesCollection graphData;
	@NonNull List<Map<String,Object>> dataState;
	@NonNull Boolean hasExpression;
	@NonNull @Getter String title;
	@NonNull String xlabel;
	@NonNull String ylabel;
	
	@Default boolean useGroups = false;
	@Default List<Integer> groups = null;
	@Default ClusterGraphType graphType = ClusterGraphType.model;
	@Default GuardExpressionHandler expression = null;
	@Default Color passColour = Colours.getGraphPaletteColour(1); 
	@Default Color failColour = Colours.getGraphPaletteColour(7);
	@Default Color nullColour = Colours.getGraphPaletteColour(4);
	@Default ChartPanel graph = null;
	@Default double segmentInterval = 0.05;
	@Default double segmentWindow = 0.2;
	@Default @Getter JPanel main = new JPanel();
	@Default JProgressBar progress = new JProgressBar();
	@Default double lowerRangeBound = Double.MAX_VALUE;
	@Default double upperRangeBound = 0.0;
	@Default double upperDomainBound = 0.0;
	@Default double lowerDomainBound = 0.0;
	
	public EnhancementClusterGraph setup() {
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
//		find groups for series, so that clustering occurs once for each outcome option
		List<Integer> satifised = new ArrayList<Integer>();
		List<Integer> unsatifised = new ArrayList<Integer>();
		List<Integer> noeval = new ArrayList<Integer>();
		for( int i=0; i < this.graphData.getSeriesCount(); i++) {
			XYSeries series = this.graphData.getSeries(i);
			List<Integer> group = null;
			if (this.useGroups && this.groups != null) {
				int grouper = this.groups.get(i);
				if (grouper == 0) {
					group = unsatifised;
				} else if (grouper == 1) {
					group = satifised;
				} else {
					group = noeval;
				}
			} else {
					group = this.hasExpression ?
					(	this.expression.evaluate(this.dataState.get(i)) ? 
							satifised : 
								unsatifised
					) : noeval ;
			}
			group.add(i);
		}
//		perform clustering and find groups
//		first collect each cluster into groups of members
//		then collect all members references to their identifiers
		List<List<Integer>> satClusterGroups = findClusterGroups(satifised);
		List<List<Integer>> unsatClusterGroups = findClusterGroups(unsatifised);
		List<List<Integer>> noClusterGroups = findClusterGroups(noeval);
//		create container for all series collections
		YIntervalSeriesCollection intervalDataset = new YIntervalSeriesCollection();
		DeviationRenderer renderer = new DeviationRenderer(true, false);
		int renderCount = 0;
//		for each cluster set, create a median interval line graph using members of clusters
		int group = 1;
		int totalMemberSize = satClusterGroups.stream().map(g -> g.size()).reduce(0, (c,n) -> c+n);
		int MaximMemberSize = satClusterGroups.stream().map(g -> g.size()).reduce(1,(c,n) -> c < n ? n : c);
		for(List<Integer> seriesIdentifiers: satClusterGroups) {
		 	createMedianTrend("- true", createMedianMap(seriesIdentifiers), group, intervalDataset, renderer, renderCount, passColour, seriesIdentifiers.size(), MaximMemberSize);
			group++;
			renderCount++;
		}
		group = 1;
		totalMemberSize = unsatClusterGroups.stream().map(g -> g.size()).reduce(0, (c,n) -> c+n);
		MaximMemberSize = unsatClusterGroups.stream().map(g -> g.size()).reduce(1,(c,n) -> c < n ? n : c);
		for(List<Integer> seriesIdentifiers: unsatClusterGroups) {
		 	createMedianTrend("- false", createMedianMap(seriesIdentifiers), group, intervalDataset, renderer, renderCount, failColour, seriesIdentifiers.size(), MaximMemberSize);
			group++;
			renderCount++;
		}
		group = 1;
		totalMemberSize = noClusterGroups.stream().map(g -> g.size()).reduce(0, (c,n) -> c+n);
		MaximMemberSize = noClusterGroups.stream().map(g -> g.size()).reduce(1,(c,n) -> c < n ? n : c);
		for(List<Integer> seriesIdentifiers: noClusterGroups) {
		 	createMedianTrend("- NE", createMedianMap(seriesIdentifiers), group, intervalDataset, renderer, renderCount, nullColour, seriesIdentifiers.size(), MaximMemberSize);
			group++;
			renderCount++;
		}
		
		
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
		XYPlot plot = chart.getXYPlot();
		renderer.setDefaultToolTipGenerator(new IntervalXYToolTipGenerator());
		plot.setBackgroundPaint(Colours.CHART_BACKGROUND);
		plot.setRangeGridlinesVisible(false);
		plot.setDomainGridlinesVisible(false);
		ValueAxis axis = plot.getRangeAxis();
		axis.setLowerBound(45.0); // should be this.lowerRangeBound
		axis.setUpperBound(205.0); // should be this.upperRangeBound
		axis = plot.getDomainAxis();
		axis.setUpperBound(this.upperDomainBound); 
		axis.setLowerBound(this.lowerDomainBound);
//		renderer.setDefaultToolTipGenerator(new StandardXYToolTipGenerator());
		plot.setRenderer(renderer);
//		remake the graph 
		this.graph = new ChartPanel(
				chart
		);
		return this.graph;
	}
	
	
	public void createMedianTrend(String suffix, Map<Double,List<Double>> medians, Integer group,YIntervalSeriesCollection intervalDataset,DeviationRenderer renderer, Integer renderCount ,Color colour, int memberCount, int totalMembers) {
		YIntervalSeries seriesInt = new YIntervalSeries("C"+ group + suffix + "(" + memberCount +")");
		createIntervalSeries(seriesInt, medians);
		intervalDataset.addSeries(seriesInt);
		setupDeviationRender(renderCount, renderer, colour, memberCount, totalMembers);
		renderer.setSeriesToolTipGenerator(renderCount, clusterSeriesInfoToolTip.builder().memberCount(memberCount).group("C"+ group + suffix).build());
	}
	
	public List<List<Integer>> findClusterGroups(List<Integer> seriesIdentifiers) {
		if (ClusterGraphType.model.equals(this.graphType)) {
			return this.modelBasedClustering(seriesIdentifiers);
		} else if (ClusterGraphType.shape.equals(this.graphType)) {
			return this.shapeBasedClustering(seriesIdentifiers);
		} else {
			throw new NotImplementedException("Unknown cluster graph type of "+ this.graphType.toString());
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<List<Integer>> shapeBasedClustering(List<Integer> seriesIdentifiers) {
//		create feature vectors to be used in clustering
		List<FeatureVector> obs = new ArrayList<FeatureVector>();
		for( int i : seriesIdentifiers) {
			XYSeries series = this.graphData.getSeries(i);
			obs.add( 
					FeatureVectorImpl.builder()
					.values(
							(Collection<? extends Double>)
							(series.getItems()
									.stream()
									.map( (it) -> {
											XYDataItem item = (XYDataItem)it;
											return item.getY().doubleValue();
										}
									)
									.collect(Collectors.toList()))
							)
					.identifier(i)
					.build()
			);
		}
		if (obs.size() < 1) {
			return new ArrayList<List<Integer>>();
		}
//		perform clustering
		System.out.println("Starting clustering on "+ title +" using "+ obs.size());
		HierarchicalClustering clusterer = HierarchicalClustering.builder()
				.clusterNum(5)
				.distance(this.graphType.distance)
				.linkage(HierarchicalClustering.LinkageType.WARD)
				.observations(obs)
				.build();
//		fit cluster groups to data
		clusterer.fit();
//		create median and std intervals for each cluster
//		first collect each cluster into groups of members
//		then collect all members references to their identifiers
		List<List<Integer>> clusterGroups = new ArrayList<List<Integer>>();
		for(int group=0;group < clusterer.getClusters().size();group++) {
			System.out.println("Collecting cluster group "+ (group+1) +"...");
			clusterGroups.add(new ArrayList<Integer>());
			List<Integer> groupCollector = clusterGroups.get(group);
			for(FeatureVector member :clusterer.getClusters().get(group).getMembers()) {
				int identifier = member.getIdentifier();
				groupCollector.add(identifier);
			}
			System.out.println("cluster group "+ (group+1) +" contains " + groupCollector.size() + " observations...");
			this.progress.setValue(this.progress.getValue() + groupCollector.size());
		}
		return clusterGroups;
	}
	
	public List<List<Integer>> modelBasedClustering(List<Integer> seriesIdentifiers){
//		create feature vectors to be used in clustering
		List<FeatureVector> obs = new ArrayList<FeatureVector>();
		for( int i : seriesIdentifiers) {
			XYSeries series = this.graphData.getSeries(i);
			obs.add( 
					FeatureVectorImpl.builder()
					.value(series.getMaxY())
					.column("maxy")
					.value(series.getMinY())
					.column("miny")
					.value((double)series.getItemCount())
					.column("count")
					.identifier(i)
					.build()
			);
		}
		if (obs.size() < 1) {
			return new ArrayList<List<Integer>>();
		}
//		perform clustering
		System.out.println("Starting clustering on "+ title +" using "+ obs.size());
		HierarchicalClustering clusterer = HierarchicalClustering.builder()
				.clusterNum(5)
				.distance(this.graphType.distance)
				.linkage(HierarchicalClustering.LinkageType.WARD)
				.observations(obs)
				.build();
//		fit cluster groups to data
		clusterer.fit();
//		create median and std intervals for each cluster
//		first collect each cluster into groups of members
//		then collect all members references to their identifiers
		List<List<Integer>> clusterGroups = new ArrayList<List<Integer>>();
		for(int group=0;group < clusterer.getClusters().size();group++) {
			System.out.println("Collecting cluster group "+ (group+1) +"...");
			clusterGroups.add(new ArrayList<Integer>());
			List<Integer> groupCollector = clusterGroups.get(group);
			for(FeatureVector member :clusterer.getClusters().get(group).getMembers()) {
				int identifier = member.getIdentifier();
				groupCollector.add(identifier);
			}
			System.out.println("cluster group "+ (group+1) +" contains " + groupCollector.size() + " observations...");
			this.progress.setValue(this.progress.getValue() + groupCollector.size());
		}
		return clusterGroups;
	}
	
	public Map<Double,List<Double>> createMedianMap(List<Integer> seriesIdentifiers){
		Map<Double, List<Double>> medians = new HashMap<Double, List<Double>>();
		for(int identifier: seriesIdentifiers) {
			XYSeries series = this.graphData.getSeries(identifier);
//			for(int i=0;i < series.getItemCount();i++) {
//				double x = (double) series.getX(i);
//				x = x - (x % segmentInterval);
//				double y = (double) series.getY(i);				
//				if (!medians.containsKey(x)) {
//					medians.put(x, new ArrayList<Double>());
//				}
//				medians.get(x).add(y);
//			}
			TimeSeriesSampling.resampleSeries(series, medians, this.segmentInterval, this.segmentWindow);
		}
		return medians;
	}
	
	public List<Object> createMedianMaps(List<Integer> seriesIdentifiers){
//		for each time point (rounded to .25 of an hour) find median
		Map<Double, List<Double>> trueMedians = new HashMap<Double, List<Double>>();
		Map<Double, List<Double>> falseMedians = new HashMap<Double, List<Double>>();
		Map<Double, List<Double>> nullMedians = new HashMap<Double, List<Double>>();
		for(int identifier: seriesIdentifiers) {
			XYSeries series = this.graphData.getSeries(identifier);
			Map<Double, List<Double>> medians = 
					this.hasExpression ?
					(	this.expression.evaluate(this.dataState.get(identifier)) ? 
							trueMedians : 
							falseMedians
					) : nullMedians ;
			
			TimeSeriesSampling.resampleSeries(series, medians, this.segmentInterval, this.segmentWindow);
		}
//		return back maps
		List<Object> out = new ArrayList<Object>();
		out.add(trueMedians);
		out.add(falseMedians);
		out.add(nullMedians);
		return out;
	}
	
	public void createIntervalSeries(YIntervalSeries series, Map<Double, List<Double>> data) {
		for(Entry<Double, List<Double>> entry : data.entrySet()) {
			double mean = entry.getValue().stream()
					.reduce(0.0, (c,n) -> c+n );
			mean = mean/entry.getValue().size();
			final double fmean = mean;
			double stdValue = entry.getValue().stream()
					.reduce(0.0, (c,u) -> { return c+Math.pow((u-fmean),2);});
			stdValue = stdValue / entry.getValue().size();
			stdValue = Math.sqrt(stdValue);
			double median = findMedianValue(entry.getValue().toArray(new Double[entry.getValue().size()]));
			series.add( entry.getKey(), median, median-stdValue, median+stdValue);
//			update lower and upper bounds
			if ((median-stdValue) < this.lowerRangeBound) {
				this.lowerRangeBound = median - stdValue;
			}
			if ((median+stdValue) > this.upperRangeBound) {
				this.upperRangeBound = median + stdValue;
			}
			if (entry.getKey() < this.lowerDomainBound) {
				this.lowerDomainBound = entry.getKey();
			}
		}
	}
	
	public double findMedianValue(Double[] values) {
		int middle = values.length / 2;
		Arrays.sort(values);
		double median =0;
		if (middle%2 == 1) {
			median = values[middle];
		} else if (middle == 0) {
			median = values[middle];
		} else { 
			median = (values[middle-1] + values[middle]) / 2.0;
		}
		return median;
	}
	
	public void setupDeviationRender(int renderCount, DeviationRenderer renderer, Color seriesColor, int memberCount, int totalMembers) {
		renderer.setSeriesStroke(renderCount, new BasicStroke(3.0f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		int alpha = (int) (255 * (memberCount/(totalMembers * 1.0)));
		Color fillPaint = new Color(seriesColor.getRed(), seriesColor.getGreen(), seriesColor.getBlue(), alpha);
		alpha = (int) (seriesColor.getAlpha() * (memberCount/(totalMembers * 1.0)));
		Color newSeriesPaint = new Color(seriesColor.getRed(), seriesColor.getGreen(), seriesColor.getBlue(), alpha);
		renderer.setSeriesFillPaint(renderCount, fillPaint);
		renderer.setSeriesPaint(renderCount, newSeriesPaint);
		
	}
	
	public static enum ClusterGraphType {
		model("Intercepted Time Series Clustering", DistanceType.EUCLID),
		shape("DTW Series Clustering", DistanceType.DTW);
		
		public String label;
		public DistanceType distance;
		
		private ClusterGraphType(String label, DistanceType distance) {
			this.label = label;
			this.distance = distance;
		}
		
		@Override
		public String toString() {
			return "(" + this.label + ", " + this.distance.label +")"; 
		}
	}

	protected JPanel doInBackground() throws Exception {
		return this.make();
	}
	
	@Override
    protected void done() {
		this.progress.setVisible(false);
		if (this.graph == null) {
			System.out.println("[EnhancementClusterGraph::"+this.graphType.toString()+"] Error :: graph panel is null");
		} else {
			this.main.add(this.graph, BorderLayout.CENTER);
		}
		
	}
	
	@Builder
	public static class clusterSeriesInfoToolTip implements XYToolTipGenerator {

		private int memberCount;
		private String group;
		
		public String generateToolTip(XYDataset dataset, int series, int item) {
			String tooltip = group+ " Members: "+this.memberCount;
			return tooltip;
		}
		
		public String generateToolTip(YIntervalSeriesCollection dataset, int series, int item) {
			String tooltip = group+ " Members: "+this.memberCount;
			return tooltip;
		}

	}
}
