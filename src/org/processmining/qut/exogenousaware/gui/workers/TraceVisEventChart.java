package org.processmining.qut.exogenousaware.gui.workers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
import org.processmining.qut.exogenousaware.data.ExogenousDatasetType;
import org.processmining.qut.exogenousaware.gui.ExogenousTraceView;
import org.processmining.qut.exogenousaware.gui.colours.ColourScheme;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousTraceViewJChartFilterPanel.ChartHolder;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Data
public class TraceVisEventChart {

	@NonNull ExogenousAnnotatedLog log;
	@NonNull XEvent endogenous; 
	@NonNull @Default int eventIndex = -1;
	
	@Default Color seriesColourBase = ColourScheme.green;
	
	@Default JScrollPane chartPanel = new JScrollPane();
	@Default JPanel view = new JPanel();
	@Default @Getter Map<String, Map<String, List<ChartPanel>>> chartDict = new HashMap<String, Map<String, List<ChartPanel>>>();
	@Default @Getter Map<String, Map<String, List<ChartSeriesController>>> seriesControllers = new HashMap<String, Map<String, List<ChartSeriesController>>>();
	@Default @Getter List<ChartHolder> charts= new ArrayList();
	
	@Default @Getter int graphs = 0;
	
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
	JLabel label = makeTitle();
	this.view.add(label, c);
//	reset layout settings
	c.weightx = 0;
	c.weighty = 0;
	c.ipady = 0;
	c.gridy += 1;
	c.insets = new Insets(25,0,25,0);
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
		String datasetkey = entry.getKey();
		XYSeriesCollection dataset = new XYSeriesCollection();
		
		double high = 0.0;
		double low = 0.0;
		int exoSeries= 0;
		boolean started = false;
		String exoSet = entry.getKey();
		String slicekey = "foo";
		List<String> slicers = new ArrayList();
		List<SubSeries> vals = new ArrayList<SubSeries>(entry.getValue());
		Comparator<SubSeries> lengthSort = (a,b) -> Integer.compare(a.size(),b.size());
		vals.sort(lengthSort);
		Color seriesColourBase = this.seriesColourBase;
		if (vals.size() > 0) {
			seriesColourBase = vals.get(0).getComesFrom().getColourBase();
		}
//		cycle through subseries and plot each
		Map<String, Integer> sliceCombos = new HashMap();
		for(SubSeries subtimeseries: vals) {
			if (subtimeseries.getDatatype().equals(ExogenousDatasetType.DISCRETE)) {
				continue;
			}
//			setup chart dict if needed
			slicekey = subtimeseries.getAbvSlicingName();
			slicers.add(slicekey);
			if (this.chartDict.containsKey(datasetkey)) {
				Map<String, List<ChartPanel>> dbmap = this.chartDict.get(datasetkey);
				if (!dbmap.containsKey(slicekey)) {
					dbmap.put(slicekey, new ArrayList<ChartPanel>());
				}
			}
			else {
				this.chartDict.put(datasetkey, new HashMap<String, List<ChartPanel>>());
				Map<String, List<ChartPanel>> dbmap = this.chartDict.get(datasetkey);
				dbmap.put(slicekey, new ArrayList<ChartPanel>());
			}
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
			sliceCombos.put(slicekey, exoSeries);
			exoSeries++;
			// prepare exo series controller dict
			if (this.seriesControllers.containsKey(datasetkey)) {
				Map<String, List<ChartSeriesController>> dbmap = this.seriesControllers.get(datasetkey);
				if (!dbmap.containsKey(slicekey)) {
					dbmap.put(slicekey, new ArrayList<ChartSeriesController>());
				}
			}
			else {
				this.seriesControllers.put(datasetkey, new HashMap<String, List<ChartSeriesController>>());
				Map<String, List<ChartSeriesController>> dbmap = this.seriesControllers.get(datasetkey);
				dbmap.put(slicekey, new ArrayList<ChartSeriesController>());
			}
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
		List<Color> colours = ColourScheme.getDarkerSampleSpace(seriesColourBase, exoSeries);
		for(int xseries=0;xseries < exoSeries; xseries++) {
			exoRender.setSeriesShape(xseries, new Ellipse2D.Double(-5,-5,10,10));
			exoRender.setSeriesStroke(xseries, new BasicStroke(2.0f));
			exoRender.setSeriesShapesVisible(xseries, true);
			exoRender.setSeriesFillPaint(xseries, Color.white);
			exoRender.setSeriesPaint(xseries, colours.get(xseries));
			this.seriesControllers.get(datasetkey)
				.get(slicers.get(xseries))
				.add(new ChartSeriesController(datasetkey, slicers.get(xseries), xseries, exoRender));
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
		JPanel mainView = new JPanel();
		mainView.setLayout(new BoxLayout(mainView, BoxLayout.Y_AXIS));
//		add a title for the main view
		mainView.add(makeTitle());
//		add a graph panel to main view
		chart = new JFreeChart(plot);
		chart.setTitle("Subseries for "+ exoSet);
		ChartPanel graph = new ChartPanel(
				chart
		);
		chart.setBackgroundPaint(Color.LIGHT_GRAY);
		chart.getLegend().setBackgroundPaint(Color.LIGHT_GRAY);
		graph.setPreferredSize(new Dimension(800,400));
		graph.setMinimumSize(new Dimension(800,400));
		graph.setMaximumSize(new Dimension(800,400));
		mainView.add(graph);
//		do i still need this?
		for(String sliceopt : new HashSet<String>(slicers)) {
			this.chartDict.get(datasetkey).get(sliceopt).add(graph);
		}
//		create holder for chart info
		ChartHolder holder = ChartHolder.builder()
				.eventIndex(this.eventIndex)
				.exoPanel(datasetkey)
				.slicers(sliceCombos)
				.chart(chart)
				.panel(mainView)
				.build();
		charts.add(holder);
		
	}
//	tidy up panel and validate sub-compontents
	this.view.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2, true));
	this.view.validate();
	}


	public JLabel makeTitle() {
		JLabel label;
		String eventString = "<html><p>Event Breakdown of ''%s'' @ %s</p></html>";
		eventString = String.format(eventString,
			endogenous.getAttributes().get("concept:name").toString(),
			endogenous.getAttributes().get("time:timestamp").toString()
		);
		label = ExogenousTraceView.createLeftAlignedLabel(eventString, true, 18);
		return label;
	}
	
	
	public double getEventTimestampMillis(XEvent ev) {
		double time = (double) ((XAttributeTimestamp) ev.getAttributes().get("time:timestamp")).getValueMillis();
		return time / (1000.0 * 60.0 * 60.0);
	}
	
	class ChartSeriesController {
		
		String panel;
		String slice;
		int renderItem;
		XYLineAndShapeRenderer renderer;
		
		public ChartSeriesController(String panel, String slice, Integer renderItem, XYLineAndShapeRenderer render) {
			this.panel = panel;
			this.slice = slice;
			this.renderItem = renderItem;
			this.renderer = render;
		}
		
		
	}
}
