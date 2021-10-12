package org.processmining.qut.exogenousaware.gui.workers;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.processmining.qut.exogenousaware.data.ExogenousAnnotatedLog;
import org.processmining.qut.exogenousaware.gui.ExogenousTraceView;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NonNull;

@Builder
@Data
public class TraceVisEventChart {

	@NonNull ExogenousAnnotatedLog log;
	@NonNull XEvent endogenous; 
	
	@Default JScrollPane chartPanel = new JScrollPane();
	@Default JPanel view = new JPanel();
	@Default Map<String, ChartPanel> charts = new HashMap<String, ChartPanel>();
	
	
	
	public void setup() {
//	setup defaults for layout manager
	this.chartPanel.setViewportView(this.view);
	this.view.setLayout( new GridBagLayout());
	GridBagConstraints c = new GridBagConstraints();
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 1;
	c.weighty = 1;
	c.ipady= 50;
	c.insets = new Insets(10,0,10,0);
	c.anchor = GridBagConstraints.WEST;
//	add label to higlight what event was clicked
	JLabel label;
	String eventString = "<html><p>Event Breakdown of ''%s'' @ %s</p></html>";
	eventString = String.format(eventString,
		endogenous.getAttributes().get("concept:name").toString(),
		endogenous.getAttributes().get("time:timestamp").toString()
	);
	label = ExogenousTraceView.createLeftAlignedLabel(eventString, true, 18);
	this.view.add(label, c);
//	reset layout settings
	c.weightx = 0;
	c.weighty = 0;
	c.ipady = 0;
	c.gridy += 1;
	c.insets = new Insets(25,0,25,0);
	int graphs = 0;
	// find all transformed attributes for this event
	List<TransformedAttribute> xattrs = new ArrayList<TransformedAttribute>();
	for(Entry<String, XAttribute> entry : endogenous.getAttributes().entrySet()) {
		if (entry.getValue().getClass().equals(TransformedAttribute.class)) {
			xattrs.add( (TransformedAttribute) entry.getValue());
		}
	}
//	for each attribute find the subseries
//	then group into datasets
	Map<String, Set<SubSeries>> linkedDatasets = new HashMap<String, Set<SubSeries>>();
	for(TransformedAttribute xattr: xattrs) {
		String dataset = xattr.getSource().getDataset();
		if ( !linkedDatasets.containsKey(dataset)) {
			Set<SubSeries> collect = new HashSet<SubSeries>();
			collect.add(xattr.getSource());
			linkedDatasets.put(dataset, collect);
		} else {
			linkedDatasets.get(dataset).add(xattr.getSource());
		}
	}
//	create a graph for each linked dataset, 
//	keep reference for each graph
	for( Entry<String, Set<SubSeries>> entry : linkedDatasets.entrySet()) {
		XYSeriesCollection dataset = new XYSeriesCollection();
		double high = 0.0;
		double low = 0.0;
		int exoSeries= 0;
		boolean started = false;
		String exoSet = entry.getKey();
//		cycle through subseries and plot each
		for(SubSeries subtimeseries: entry.getValue()) {
			// check for non-empty sub-sequences
			List<Long> xseries = subtimeseries.getXSeries(true);
			List<Double> yseries = subtimeseries.getYSeries();
			if (xseries.size()> 0) {
				graphs += 1;
			} else {
				continue;
			}
			// create series and keep track of high and low
			XYSeries series = new XYSeries(
					"linked ("+subtimeseries.getAbvSlicingName()+")"
					,true);
			for(int i=0; i < xseries.size();i++) {
				series.add(
						xseries.get(i) / (1000.0 * 60.0 * 60.0)
						,
						yseries.get(i)
						
				);
				if (!started) {
					low = yseries.get(i);
					high = yseries.get(i);
					started = true;
				} else {
					double val = yseries.get(i);
					low = low > val ? val : low;
					high = high < val ? val : high;
				}
			}
			// add series and move on
			dataset.addSeries(series);
			exoSeries++;
		}
//		add when the event occured
		XYSeries evSeries = new XYSeries("event occurance");
		evSeries.add(
			0,
			low - 5
		);
		evSeries.add (
			 0,
			 high + 5
		);
		dataset.addSeries(evSeries);
//		create plot
		JFreeChart chart = ChartFactory.createXYLineChart(
				"Subseries for "+ exoSet, 
				"time:timestamp (hours)", 
				"value", 
				dataset
		);
		XYPlot plot = (XYPlot) chart.getPlot();
//		handle render settings for each series type
		XYLineAndShapeRenderer exoRender = new XYLineAndShapeRenderer();
		exoRender.setUseFillPaint(true);
		for(int xseries=0;xseries < exoSeries; xseries++) {
			exoRender.setSeriesShape(xseries, new Ellipse2D.Double(-2.5,-2.5,5,5));
			exoRender.setSeriesShapesVisible(xseries, true);
			exoRender.setSeriesFillPaint(xseries, Color.white);
		}
		exoRender.setSeriesShape(exoSeries, new Ellipse2D.Double(-5,-5,10,10));
		exoRender.setSeriesShapesVisible(exoSeries, true);
		exoRender.setSeriesFillPaint(exoSeries, Color.black);
		exoRender.setSeriesPaint(exoSeries, Color.black);
		plot.setRenderer(0, exoRender);
//		tidy up y range and x range
		ValueAxis domain = plot.getRangeAxis();
		domain.setRange(low - 15, high+15);
		domain = plot.getDomainAxis();
		domain.setRange(domain.getLowerBound()-0.5,domain.getUpperBound()+0.5);
//		recreate chart panel with new plot
		chart = new JFreeChart(plot);
		chart.setTitle("Subseries for "+ exoSet);
		ChartPanel graph = new ChartPanel(
				chart
		);
		chart.setBackgroundPaint(Color.LIGHT_GRAY);
		// add graph to viewport
		c.gridy += 1;
		c.weightx = 0.8;
		c.weighty =0;
		this.view.add(graph, c);
		c.weightx = 0.0;
		c.weighty =1;
//		keep reference to chartpanel
		this.charts.put(entry.getKey(), graph);
	}
//	tidy up panel and validate sub-compontents
	this.view.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2, true));
	this.view.validate();
	}
	
	
	public double getEventTimestampMillis(XEvent ev) {
		double time = (double) ((XAttributeTimestamp) ev.getAttributes().get("time:timestamp")).getValueMillis();
		return time / (1000.0 * 60.0 * 60.0);
	}
}
