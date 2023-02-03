package org.processmining.qut.exogenousaware.gui.workers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.processmining.qut.exogenousaware.data.ExogenousDatasetType;
import org.processmining.qut.exogenousaware.gui.ExogenousTraceView;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;

@Builder
public class TraceVisOverviewChart extends SwingWorker<JPanel, String> {
	
	@NonNull ExogenousTraceView source;
	@NonNull JPanel target;
	@NonNull JLabel progress;
	@NonNull XTrace endo;
	
	@Default boolean standardize = true;
	@Default ChartPanel chart = null;
	@Default double innerSpacing = 25;
	
//	gui elements
	private static String FlipToStandardise = "Standardise exo-series?";
	private static String FlipToRaw = "Show raw exo-series?";
	
	@Override
	protected JPanel doInBackground() throws Exception {
		 // define what thread will do here
		JPanel graph = backgroundBuildPanel();
		
		return graph;
	}
	
	protected List<Object> buildEventSlices(double low, double high) {
//		build containers for output, who would want a tuple return ?
//		instead of this hack
		List<Object> seriesAndRender = new ArrayList<Object>();
		XYSeriesCollection endoMoments = new XYSeriesCollection();
		XYLineAndShapeRenderer endoRender = new XYLineAndShapeRenderer();
		seriesAndRender.add(endoMoments);
		seriesAndRender.add(endoRender);
//		for each endogenous event in the endogenous trace
//		find a x timestamp and create two points at (x,low) and (x,high)
//		to create a black bar
		int evCount = 0;
		boolean started = false;
		double startingTime = 0.0;
		for(XEvent ev : this.endo) {
//			build a vertical slice for this moment
			if (!started) {
				started = true;
				startingTime = getEventTimestampMillis(ev);
			}
			XYSeries endoMoment = new XYSeries(
					"E"+ evCount +" ("+ev.getAttributes().get("concept:name").toString()+")"
			);
			endoMoment.add(
					getEventTimestampMillis(ev) - startingTime, 
					low
			);
			endoMoment.add(
					getEventTimestampMillis(ev) - startingTime, 
					high
			);
			endoRender.setSeriesPaint(evCount, new Color(0F, 0F, 0F, 0.33F));
			endoRender.setSeriesShapesVisible(evCount , false);
			endoRender.setSeriesVisibleInLegend(evCount, false, false);
			evCount++;
			endoMoments.addSeries(endoMoment);
		}
		
		return seriesAndRender;
	}
	
	protected List<Object> buildExogenousSeries(double starting){
//		build containters for job
		List<Object> seriesRenderBounds = new ArrayList<Object>();
		XYSeriesCollection exoSignals = new XYSeriesCollection();
		XYLineAndShapeRenderer exoRender = new XYLineAndShapeRenderer();
		exoRender.setUseFillPaint(true);
		Map<String, Double> bounds = new HashMap<String, Double>(){{ put("low",0.0); put("high",100.0); }};
		boolean boundSet = false;
		seriesRenderBounds.add(exoSignals);
		seriesRenderBounds.add(exoRender);
		seriesRenderBounds.add(bounds);
//		search through endo trace and find transformed attributes,
//		for each attribute, find subseries
//		then find set of exogenous time series to show
		Set<XTrace> exogenousTimeSeries = new HashSet<XTrace>();
		List<XTrace> exoSeries = new ArrayList();
		List<SubSeries> slices = new ArrayList();
		for(XEvent ev: this.endo) {
			for(Entry<String, XAttribute> entry : ev.getAttributes().entrySet()) {
				if (entry.getValue().getClass().equals(TransformedAttribute.class)) {
					TransformedAttribute xattr = (TransformedAttribute) entry.getValue();
//					can only visualise numerical traces
					if (xattr.getSource().getDatatype().equals(ExogenousDatasetType.NUMERICAL)) {
						boolean added = exogenousTimeSeries.add(xattr.getSource().getSource());
						if (added) {
							slices.add(xattr.getSource());
							exoSeries.add(xattr.getSource().getSource());
						}
					}
				}
			}
		}
//		handle each series, by creating an x and y for each trace
//		x being the timestamp
//		y being the measurement (exogenous:value)
//		keep track of min and max to make event slices
		
		int exoCount = 0;
		for(XTrace exo: exoSeries) {
			List<Double> x = exo.stream().map((ev) -> getEventTimestampMillis(ev)).collect(Collectors.toList());
			List<Double> y = exo.stream().map((ev) -> getExoEventValue(ev)).collect(Collectors.toList());
//			check if we have set up bounds before
			double min = y.stream().reduce((c,n) -> c > n ? n : c).get();
			double high = y.stream().reduce((c,n) -> c < n ? n : c).get();
			double mean = y.stream().reduce((c,n) -> c + n).get() / y.size();
			double std = Math.sqrt(y.stream().reduce((c,n) -> c + Math.pow(n - mean,2)).get()) / y.size();
			if (standardize) {
				y = y.stream().map(ny -> (ny - mean)/std).collect(Collectors.toList());
				min = (min - mean) /std;
				high = (high - mean) /std;
			}
			if (!boundSet) {
				bounds.put("low", min);
				bounds.put("high", high);
				boundSet = true;
			} else {
//				check min and max for series against current bounds
				if (bounds.get("low") > min) {
					bounds.put("low", min);
				}
				if(bounds.get("high") < high) {
					bounds.put("high", high);
				}
			}
//			build series and render
			String title = "%s (Exo-Panel#%d)";
			String name = getExogenousName(exo);
			XYSeries series;
			if (standardize) {
				title = title + " (%.2f/%.2f)";
				series = new XYSeries(String.format(title, name, exoCount+1, mean, std));
			} else {
				series = new XYSeries(String.format(title, name, exoCount+1));
			}
			 
			for(int idx = 0; idx < x.size(); idx++) {
				series.add(x.get(idx) - starting, y.get(idx));
			}
			exoSignals.addSeries(series);
			exoRender.setSeriesShape(exoCount, new Ellipse2D.Double(-1.5,-1.5,3,3));
			exoRender.setSeriesFillPaint(exoCount, Color.white);
			exoRender.setSeriesPaint(exoCount, slices.get(exoCount).getComesFrom().getColourBase());
			exoCount++;
		}
		
		return seriesRenderBounds;
	}
	
	@SuppressWarnings("unchecked")
	protected JPanel backgroundBuildPanel() {
//		setup containers
		Map<String,Double> bounds = null;
		XYSeriesCollection endoMoments = null;
		XYLineAndShapeRenderer endoRender = null;
		XYSeriesCollection exoSignals = null;
		XYLineAndShapeRenderer exoRender = null;
//		get the first event timestamp
		double starting = this.endo.stream()
				.map(ev -> getEventTimestampMillis(ev))
				.reduce((c,n) -> c > n ? n : c)
				.get();
//		build exogenous time series data
		List<Object> out = buildExogenousSeries(starting);
		exoSignals = (XYSeriesCollection) out.get(0);
		exoRender = (XYLineAndShapeRenderer) out.get(1);
		bounds = (Map<String, Double>) out.get(2);
//		build endogenous time data
		out = buildEventSlices(bounds.get("low") - innerSpacing, bounds.get("high")+innerSpacing);
		endoMoments = (XYSeriesCollection) out.get(0);
		endoRender = (XYLineAndShapeRenderer) out.get(1);
//		build dummy plot
		JFreeChart chart = ChartFactory.createXYLineChart(
				"Linked exo-series for trace: "+ endo.getAttributes().get("concept:name").toString(), 
				"time:timestamp (hours)", 
				"measurement value", 
				null
		);
		XYPlot plot = chart.getXYPlot();
		
//		add endoSliceMoments
		plot.setDataset(0, endoMoments);
		plot.setRenderer(0, endoRender);
		
//		add full subseries to chart
		plot.setDataset(1, exoSignals);
		plot.setRenderer(1, exoRender);
		
//		add text annotations for event lines
//		double last_x=0.0;
//		double last_y=0.0;
//		for(int i = 0; i < xticks.size(); i++){
//			if ((i > 0)&((xticks.get(i) - last_x) < 500 )) {
////				do something different so to not overlap labels
//				XYTextAnnotation textAnnotation = new XYTextAnnotation("   " + xtickLabels.get(i) +"   ", xticks.get(i), last_y+5);
//		        textAnnotation.setRotationAngle(-1.5708);
//		        textAnnotation.setBackgroundPaint(Color.white);
//		        plot.addAnnotation(textAnnotation);
//		        last_x = xticks.get(i);
//		        last_y = last_y+5+textAnnotation.getText().length();
//			} else {
//				XYTextAnnotation textAnnotation = new XYTextAnnotation("   " + xtickLabels.get(i) +"   ", xticks.get(i), bounds.get("min")+15);
//		        textAnnotation.setRotationAngle(-1.5708);
//		        textAnnotation.setBackgroundPaint(Color.white);
//		        plot.addAnnotation(textAnnotation);
//		        last_x = xticks.get(i);
//		        last_y = bounds.get("min")+15+textAnnotation.getText().length();
//			}
//			publish(String.format("handling endogenous labels: %d", i));
//	    }
		
//		setup a nice view of gridlines
		ValueAxis domain = plot.getRangeAxis();
		domain.setRange(bounds.get("low")- (innerSpacing-5),bounds.get("high")+ (innerSpacing-5));
		domain = plot.getDomainAxis();
		domain.setRange(-(1),endoMoments.getDomainUpperBound(false)+(1));
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(false);
		
		publish(String.format("graph built"));
//		build chart panel		
		this.chart = new ChartPanel(
				chart
		);
		Color trans = new Color(255,255,255,0);
		this.chart.setBackground(Color.LIGHT_GRAY);
		chart.setBackgroundPaint(trans);
		chart.getLegend().setBackgroundPaint(trans);
		chart.getPlot().setBackgroundAlpha(0.0f);
		return this.chart;
	}
	
	
	@Override
    protected void process(List<String> chunks)
    {
		System.out.println("[TraceVisOverviewChart] Working on : "+chunks.get(chunks.size()-1));
		this.progress.setText("Working on :" +chunks.get(chunks.size()-1));
		this.target.validate();
    }

	@Override
    protected void done() 
    {
        // this method is called when the background 
        // thread finishes execution
        try 
        {
            JPanel graph = get();
            this.source.setTraceOverviewChart(chart);
            this.progress.setVisible(false);
            graph.repaint();
            this.target.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            c.gridwidth = 10;
            c.weightx = 1.0;
            c.weighty = 0.95;
            c.fill = GridBagConstraints.BOTH;
            this.target.add(graph, c);
//            add flip standardisation button
            String buttonText;
            if (standardize) {
            	buttonText = FlipToRaw;
            } else {
            	buttonText = FlipToStandardise;
            }
            JButton flipper = new JButton(buttonText);
            flipper.addMouseListener(new FlipperListener(this));
            flipper.setMaximumSize(new Dimension(100, 25));
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            c.gridy +=1;
            c.gridwidth =1;
            c.weightx = 0.1;
            c.weighty = 0.05;
            this.target.add(flipper, c);
            this.target.repaint();
            this.target.validate();
        } 
        catch (InterruptedException e) 
        {
            e.printStackTrace();
        } 
        catch (ExecutionException e) 
        {
            e.printStackTrace();
        }
    }

	public double getEventTimestampMillis(XEvent ev) {
		double time = (double) ((XAttributeTimestamp) ev.getAttributes().get("time:timestamp")).getValueMillis();
		return time / (1000 * 60 * 60);
	}
	
	public double getExoEventValue(XEvent ev) {
		return Double.parseDouble(ev.getAttributes().get("exogenous:value").toString());
	}
	
	public static String getExogenousName(XTrace trace) {
		return trace.getAttributes().get("exogenous:name").toString();
	}
	
	
	private static class FlipperListener extends MouseAdapter {
		
		private TraceVisOverviewChart owner;
		
		public FlipperListener(TraceVisOverviewChart owner) {
			this.owner = owner;
		}

		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			owner.source.updateTraceVis(owner.endo, !owner.standardize);
		}
		
		
		
	}
}
