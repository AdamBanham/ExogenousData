package org.processmining.qut.exogenousaware.gui.workers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import org.processmining.qut.exogenousaware.gui.colours.ColourScheme;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class TraceVisOverviewChart extends SwingWorker<JPanel, String> {
	
	@NonNull ExogenousTraceView source;
	@NonNull JPanel target;
	@NonNull JLabel progress;
	@NonNull XTrace endo;
	
	@Default boolean normalize = false;
	@Default boolean ppa = false;
	@Default boolean sax = false;
	@Default ChartPanel chart = null;
	@Default double innerSpacing = 0.05;
	
//	SAX Gaussian equiprobable regions (10)
	@Default List<Double> SAX_BOUNDARIES = new ArrayList<Double>() {{
		add(-1.28);
		add(-0.84);
		add(-0.52);
		add(-0.25);
		add(0.0);
		add(0.25);
		add(0.52);
		add(0.84);
		add(1.28);
	}};
	@Default List<String> SAX_LETTERS = new ArrayList<String>() {{
		add("a");
		add("b");
		add("c");
		add("d");
		add("e");
		add("f");
		add("g");
		add("h");
		add("i");
		add("j");
	}};
	
//	gui elements
	private static String FlipToStandardise = "Normalise exo-series?";
	private static String FlipToRaw = "Show raw exo-series?";
	private static String FlipToRealDim = "Show real dimensions?";
	private static String FlipToReduceDim = "Show reduced dimensions?";
	private static String FlipToShowSax = "Show SAX boundaries?";
	private static String FlipToWOSAX = "Stop showing SAX?";
	
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
			//	check if we have set up bounds before
			double mean = 0.0;
			double std = 0.0;
			// perform normalisation to show time series differences?
			if (normalize) {
				Normalisation norm = normalise(y, x);
				if (norm != null) {
					System.out.println("Normalisation completed");
					y = norm.getValues();
					mean = norm.getMean();
					std = norm.getStd();
				} else {
					mean = y.stream().reduce((c,n) -> c + n).get() / y.size();
					final double mmean = mean;
					std = Math.sqrt(y.stream().reduce((c,n) -> c + Math.pow(n - mmean,2)).get() / y.size());
				}
			}
			// prepare suitable min and max view range for plot
			double min;
			double high;
			if (normalize) {
				min = -3;
				high = 3;
			} else {
				min = y.stream().reduce((c,n) -> c > n ? n : c).get();
				high = y.stream().reduce((c,n) -> c < n ? n : c).get();
			}
			// perform dimensionality reduction via PAA?
			if (ppa) {
				y = PiecewiseAggregateAproximation(y);
				x = PiecewiseAggregateAproximation(x);
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
			if (normalize) {
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
	
	private List<Double> PiecewiseAggregateAproximation(List<Double> series) {
		 return PiecewiseAggregateAproximation(series, 100);
	}
	
	private List<Double> PiecewiseAggregateAproximation(List<Double> series, int windows) {
		if (series.size() < windows) {
			System.out.println(String.format(
					"[TraceVisOverviewChart] PPA window reduced from %d to...",
					windows)
			);
			windows = (int)(series.size() * 0.05);
			System.out.println("[TraceVisOverviewChart] ..."+windows);
		}
		List<Double> ret = new ArrayList<Double>(windows);
		// perform aggregation
		// c^_i = w/n * sum(c_j)
		for(int i=1;i <= windows; i++) {
			int start = (series.size()/ windows) * (i - 1) + 1;
			int end = (series.size()/windows) * i;
			List<Double> frame = series.subList(start, end);
			
			if (frame.size() == 1) {
				ret.add(frame.get(0));
			} else if (frame.size() < 1) {
				continue;
			} else {
				double mean = frame.stream().reduce((c, n) -> c + n).get() / frame.size();
				ret.add(mean);
			}
		}
		
		if (ret.size() != windows) {
			System.out.println(String.format(
						"[TraceVisOverviewChart] PPA stream returned different size :: "
						+ "expected %d but saw %d.",
						windows, ret.size())
			);
		}
		
		return ret;
	}

	@Builder
	private static class Normalisation {
		@Getter @NonNull List<Double> values;
		@Getter double mean;
		@Getter double std;
	}
	
	private Normalisation normalise(List<Double> values, List<Double> times) {
		if (values.size() < 2) {
			return null;
		}
		
		List<Double> ret = new ArrayList<Double>(values.size());
		// perform weighted mean, calc using time different between now and last
		double len = times.get(times.size() - 1) - times.get(0);
		double durtemp = 0.0;
		double tmp = 0.0;
		for(int i=1; i < values.size(); i++) {
			double dur = (times.get(i) - times.get(i -1)) / len;
			double cval = values.get(i);
			tmp += cval * dur;
			durtemp += dur;
		}
		final double mean = tmp;
		// find standard deviation
		double std = values.stream()
				.map( v -> v - mean)
				.map( v -> Math.pow(v, 2))
				.reduce((c,n) -> c + n)
				.get();
		std = std / values.size();
		std = Math.sqrt(std);
		// modify values and return new series
		for(int i=0; i < values.size(); i++) {
			double val = values.get(i);
			val = (val - mean) / std;
			ret.add(val);
		}
		return Normalisation.builder()
				.values(ret)
				.mean(mean)
				.std(std)
				.build();
	}
	
	protected List<Object> buildSAXBoundaries(double left, double right){
//		build containers for output, who would want a tuple return ?
//		instead of this hack
		List<Object> seriesAndRender = new ArrayList<Object>();
		XYSeriesCollection saxMoments = new XYSeriesCollection();
		XYLineAndShapeRenderer saxRender = new XYLineAndShapeRenderer();
		seriesAndRender.add(saxMoments);
		seriesAndRender.add(saxRender);
//		build horizontal lines for boundaries
		for(int i=0; i < SAX_BOUNDARIES.size(); i++) {
			XYSeries saxMoment = new XYSeries(
					"SAX_"+ i
			);
			saxMoment.add(
					left, 
					SAX_BOUNDARIES.get(i)
			);
			saxMoment.add(
					right, 
					SAX_BOUNDARIES.get(i)
			);
			saxRender.setSeriesPaint(i, ColourScheme.red);
			saxRender.setSeriesShapesVisible(i , false);
			saxRender.setSeriesVisibleInLegend(i, false, false);
			saxMoments.addSeries(saxMoment);
		}
		
		return seriesAndRender;
	}
	
	@SuppressWarnings("unchecked")
	protected JPanel backgroundBuildPanel() {
//		setup containers
		Map<String,Double> bounds = null;
		XYSeriesCollection endoMoments = null;
		XYLineAndShapeRenderer endoRender = null;
		XYSeriesCollection exoSignals = null;
		XYLineAndShapeRenderer exoRender = null;
		XYSeriesCollection saxSlices = null;
		XYLineAndShapeRenderer saxRender = null;
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
		double spacing = bounds.get("high") - bounds.get("low");
		spacing = spacing * innerSpacing;
		out = buildEventSlices(bounds.get("low") - spacing, bounds.get("high")+spacing);
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
		
//		add sax boundaries if needed
		if (normalize && ppa && sax) {
			double left = -1;
			double right = endoMoments.getDomainUpperBound(false)+1;
			out = buildSAXBoundaries(left, right);
			saxSlices = (XYSeriesCollection) out.get(0);
			saxRender = (XYLineAndShapeRenderer) out.get(1);
			plot.setDataset(2, saxSlices);
			plot.setRenderer(2, saxRender);
		}
		
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
		domain.setRange(bounds.get("low")- (spacing),bounds.get("high")+ (spacing));
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
            if (normalize) {
            	buttonText = FlipToRaw;
            } else {
            	buttonText = FlipToStandardise;
            }
            JButton flipper = new JButton(buttonText);
            flipper.addMouseListener(new NormFlipperListener(this));
            flipper.setMaximumSize(new Dimension(100, 25));
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            c.gridy +=1;
            c.gridwidth =1;
            c.weightx = 0.0;
            c.insets = new Insets(5,50,5,7);
            c.weighty = 0.05;
            this.target.add(flipper, c);
//            add flip dimension button
            if (ppa) {
            	buttonText = FlipToRealDim;
            } else {
            	buttonText = FlipToReduceDim;
            }
            flipper = new JButton(buttonText);
            flipper.addMouseListener(new PPAFlipperListener(this));
            flipper.setMaximumSize(new Dimension(100, 25));
            c.gridx += 1;
            c.insets = new Insets(5,7,5,7);
            this.target.add(flipper, c);
//            add sax boundaries
            if (ppa && normalize) {
            	if (sax) {
            		buttonText = FlipToWOSAX;
            	} else {
            		buttonText = FlipToShowSax;
            	}
            	flipper = new JButton(buttonText);
            	flipper.addMouseListener(new SAXFlipperListener(this));
            	flipper.setMaximumSize(new Dimension(100,25));
            	c.gridx += 1;
            	this.target.add(flipper, c);
            }
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
	
	
	private static class NormFlipperListener extends MouseAdapter {
		
		private TraceVisOverviewChart owner;
		
		public NormFlipperListener(TraceVisOverviewChart owner) {
			this.owner = owner;
		}

		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			owner.source.updateTraceVis(owner.endo, !owner.normalize, owner.ppa, owner.sax);
		}
	}
	
	private static class PPAFlipperListener extends MouseAdapter {
		
		private TraceVisOverviewChart owner;
		
		public PPAFlipperListener(TraceVisOverviewChart owner) {
			this.owner = owner;
		}

		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			owner.source.updateTraceVis(owner.endo, owner.normalize, !owner.ppa, owner.sax);
		}
	}
	
	private static class SAXFlipperListener extends MouseAdapter {
		
		private TraceVisOverviewChart owner;
		
		public SAXFlipperListener(TraceVisOverviewChart owner) {
			this.owner = owner;
		}

		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			owner.source.updateTraceVis(owner.endo, owner.normalize, owner.ppa, !owner.sax);
		}
	}
}
