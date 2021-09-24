package org.qut.exogenousaware.gui.panels;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.qut.exogenousaware.gui.panels.ExogenousEnhancementDotPanel.GuardExpressionHandler;
import org.qut.exogenousaware.ml.clustering.HierarchicalClustering;
import org.qut.exogenousaware.ml.clustering.HierarchicalClustering.DistanceType;
import org.qut.exogenousaware.ml.data.FeatureVector;
import org.qut.exogenousaware.ml.data.FeatureVectorImpl;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class EnhancementClusterGraph {
	@NonNull XYSeriesCollection graphData;
	@NonNull List<Map<String,Object>> dataState;
	@NonNull Boolean hasExpression;
	@NonNull @Getter String title;
	@NonNull String xlabel;
	@NonNull String ylabel;
	
	@Default DistanceType distance = DistanceType.EUCLID;
	@Default GuardExpressionHandler expression = null;
	@Default Color passColour = new Color(0,102,51,175); 
	@Default Color failColour = new Color(128,0,0,175);
	@Default Color nullColour = new Color(0,0,0,175);
	@Default ChartPanel graph = null;
	@Default double segmentInterval = 0.15;
	
	public ChartPanel make() {
//		find groups for series, so that clustering occurs once for each outcome option
		List<Integer> satifised = new ArrayList<Integer>();
		List<Integer> unsatifised = new ArrayList<Integer>();
		List<Integer> noeval = new ArrayList<Integer>();
		for( int i=0; i < this.graphData.getSeriesCount(); i++) {
			XYSeries series = this.graphData.getSeries(i);
			List<Integer> group = 
					this.hasExpression ?
					(	this.expression.evaluate(this.dataState.get(i)) ? 
							satifised : 
								unsatifised
					) : noeval ;
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
		for(List<Integer> seriesIdentifiers: satClusterGroups) {
		 	createMedianTrend("- true", createMedianMap(seriesIdentifiers), group, intervalDataset, renderer, renderCount, passColour);
			group++;
			renderCount++;
		}
		group = 1;
		for(List<Integer> seriesIdentifiers: unsatClusterGroups) {
		 	createMedianTrend("- false", createMedianMap(seriesIdentifiers), group, intervalDataset, renderer, renderCount, failColour);
			group++;
			renderCount++;
		}
		group = 1;
		for(List<Integer> seriesIdentifiers: noClusterGroups) {
		 	createMedianTrend("- NE", createMedianMap(seriesIdentifiers), group, intervalDataset, renderer, renderCount, nullColour);
			group++;
			renderCount++;
		}
		
		
//		make dummy chart
		JFreeChart chart = ChartFactory.createXYLineChart(
				this.title, 
				this.xlabel, 
				this.ylabel, 
				intervalDataset
		);
		XYPlot plot = chart.getXYPlot();
		plot.setRenderer(renderer);
//		remake the graph 
		this.graph = new ChartPanel(
				chart
		);
		return this.graph;
	}
	
	
	public void createMedianTrend(String suffix, Map<Double,List<Double>> medians, Integer group,YIntervalSeriesCollection intervalDataset,DeviationRenderer renderer, Integer renderCount ,Color colour) {
		YIntervalSeries seriesInt = new YIntervalSeries("C"+ group + suffix);
		createIntervalSeries(seriesInt, medians);
		intervalDataset.addSeries(seriesInt);
		setupDeviationRender(renderCount, renderer, colour);
	}
	
	public List<List<Integer>> findClusterGroups(List<Integer> seriesIdentifiers) {
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
				.distance(this.distance)
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
		}
		return clusterGroups;
	}
	
	public Map<Double,List<Double>> createMedianMap(List<Integer> seriesIdentifiers){
		Map<Double, List<Double>> medians = new HashMap<Double, List<Double>>();
		for(int identifier: seriesIdentifiers) {
			XYSeries series = this.graphData.getSeries(identifier);
			for(int i=0;i < series.getItemCount();i++) {
				double x = (double) series.getX(i);
				x = x - (x % segmentInterval);
				double y = (double) series.getY(i);				
				if (!medians.containsKey(x)) {
					medians.put(x, new ArrayList<Double>());
				}
				medians.get(x).add(y);
			}
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
			for(int i=0;i < series.getItemCount();i++) {
				double x = (double) series.getX(i);
				x = x - (x % segmentInterval);
				double y = (double) series.getY(i);				
				if (!medians.containsKey(x)) {
					medians.put(x, new ArrayList<Double>());
				}
				medians.get(x).add(y);
			}
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
	
	public void setupDeviationRender(int renderCount, DeviationRenderer renderer, Color seriesColor ) {
		renderer.setSeriesStroke(renderCount, new BasicStroke(3.0f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		Color fillPaint = new Color(seriesColor.getRed(), seriesColor.getGreen(), seriesColor.getBlue(), 100);
		renderer.setSeriesFillPaint(renderCount, fillPaint);
		renderer.setSeriesPaint(renderCount, seriesColor);
	}
}
