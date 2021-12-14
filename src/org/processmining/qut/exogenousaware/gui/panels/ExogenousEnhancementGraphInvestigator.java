package org.processmining.qut.exogenousaware.gui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.jfree.data.xy.YIntervalSeries;
import org.processmining.framework.util.ui.widgets.ProMList;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.Dot.GraphDirection;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.qut.exogenousaware.gui.ExogenousDiscoveryInvestigator;
import org.processmining.qut.exogenousaware.gui.dot.DotGraphVisualisation;
import org.processmining.qut.exogenousaware.gui.dot.ExoDotTransition;
import org.processmining.qut.exogenousaware.gui.workers.EnhancementGraphSearchRankedList;
import org.processmining.qut.exogenousaware.gui.workers.EnhancementMedianGraph;
import org.processmining.qut.exogenousaware.gui.workers.EnhancementRankExporter;
import org.processmining.qut.exogenousaware.ml.clustering.distance.DynamicTimeWarpingDistancer;
import org.processmining.qut.exogenousaware.ml.data.FeatureVector;
import org.processmining.qut.exogenousaware.ml.data.FeatureVectorImpl;
import org.processmining.qut.exogenousaware.stats.tests.WilcoxonSignedRankTester;

import com.kitfox.svg.SVGElement;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import prefuse.data.query.ListModel;

@Builder
public class ExogenousEnhancementGraphInvestigator {

	@NonNull private ExogenousEnhancementAnalysis source;
	@NonNull private ExogenousDiscoveryInvestigator controller;
	
	@Default @Getter private JPanel main = new JPanel();
	@Default private GridBagConstraints c = new GridBagConstraints();
	@Default @Getter private JButton back = new JButton("back");
	@Default @Getter private JButton export = new JButton("export");
	@Default private JPanel modelPanel = new JPanel();
	@Default private JPanel graphPanel = new JPanel();
	@Default private JPanel rankingPanel = new JPanel();
	
	@Default private ProMList<RankedListItem> rankList = null;
	@Default private EnhancementGraphSearchRankedList rankerWorker = null;
	@Default private DotPanel vis = new DotPanel(new Dot());
	@Default private EnhancementExogenousDatasetGraphController currentGraphController = null;
	@Default private JPanel progressPanel = new JPanel();
	@Default private JProgressBar progresser = new JProgressBar();
	@Default private JLabel progressLabel = new JLabel("Ranking progress :");
	
	public ExogenousEnhancementGraphInvestigator setup() {
		createPanels();
		// setup constraints
		c.anchor = GridBagConstraints.LINE_START;
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight =1;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.4;
		c.insets = new Insets(5, 5, 5, 5);
		c.fill = GridBagConstraints.BOTH;
		// style and set main
		main.setBackground(Color.DARK_GRAY);
		main.setLayout(new GridBagLayout());
		// add panels
		// adding model panel		
		c.gridwidth = 3;
		c.weightx = 1;
		c.ipady =150;
		main.add(modelPanel, c);
		c.ipady = 0;
		c.gridy++;
		// adding graph panel
		c.gridwidth = 2;
		c.weightx = 0.7;
		c.weighty = 0.6;
		main.add(graphPanel, c);
		// adding ranking panel
		c.gridwidth = 1;
		c.weightx = 0.3;
		c.gridx = 2;
		main.add(progressPanel, c);
		// add back button
		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.0;
		c.weighty = 0.0;
		main.add(back, c);
		// add export button, of RHS of screen space.
		c.gridx++;
		c.anchor = GridBagConstraints.FIRST_LINE_END;
		main.add(export,c);
		main.validate();
		return this;
	}
	
	
	
	public void createPanels() {
		progressLabel.setForeground(Color.WHITE);
		// setup progress panel
		progressPanel.setLayout(new BorderLayout());
		progressPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		progressPanel.setBackground(Color.DARK_GRAY);
		progressPanel.add(progressLabel,BorderLayout.NORTH);
		progressPanel.add(progresser, BorderLayout.SOUTH);
		progresser.setMaximum(this.source.getExoCharts().keySet().size());
		progresser.setValue(0);
		// set up ranking worker
		rankerWorker = EnhancementGraphSearchRankedList.builder()
				.data(this.source.getExoCharts())
				.progress(progresser)
				.build();
		rankerWorker.execute();
		rankerWorker.addPropertyChangeListener(new rankedListener(this));
		rankingPanel.setLayout( new BorderLayout());
		// set up  model panel 
		vis.setDirection(GraphDirection.leftRight);
		vis.changeDot( 
			createDotVis(),
			true);
		modelPanel.setPreferredSize(new Dimension(600,250));
		modelPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));
		modelPanel.setLayout(new BorderLayout());
		modelPanel.add(vis);
		modelPanel.validate();
		// set up graph panel
		graphPanel.setLayout(new BorderLayout());
		graphPanel.setPreferredSize(new Dimension(900,600));
		graphPanel.setBackground(Color.DARK_GRAY);
		graphPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));
//		currentGraphController = ((EnhancementExogenousDatasetGraphController) this.source.getExoCharts().get(this.source.getExoCharts().keySet().iterator().next()))
//				.createCopy();
//		graphPanel.add(
//				currentGraphController
//		);
		graphPanel.validate();
// 		add mouse listener to back button
		back.addMouseListener(new BackListener(back, controller));
//		add export listener to export button
		export.addMouseListener(new ExportListener(this));
		export.setEnabled(false);
	}
	
	private void swapGraphController(RankedListItem cont) {
		if (currentGraphController != null) {
			this.graphPanel.remove(currentGraphController);
		}
		currentGraphController = cont.getController()
				.createCopy();
		graphPanel.add(
				currentGraphController
		);
		graphPanel.validate();
	}
	
	public void changeToRankedList() {
		createRankPanel();
		c.gridwidth = 1;
		c.weightx = 0.3;
		c.gridx = 2;
		c.gridy = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.BOTH;
		main.remove(progressPanel);
		main.add(rankingPanel, c);
		this.export.setEnabled(true);
		main.validate();
	}
	
	public void changeToProgress() {
		c.gridwidth = 1;
		c.weightx = 0.3;
		c.gridx = 2;
		c.gridy = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.BOTH;
		main.remove(rankingPanel);
		main.add(progressPanel,c);
		main.validate();
	}
	
	private void createRankPanel() {
		rankList = new ProMList<RankedListItem>(
				"Exogenous Ranked Charts", 
				new ListModel(rankerWorker.getOutcome().toArray())
		);	
		rankList.setSelectionMode(ListModel.SINGLE_SELECTION);
		rankList.setSelectedIndex(0);
		rankList.setBackground(Color.GRAY);
		rankList.setMinimumSize(new Dimension(150,500));
		rankList.setPreferredSize(new Dimension(175,600));
		rankList.setMaximumSize(new Dimension(250,750));
		rankList.addMouseListener(new SelectionListener(this, rankList));
		rankList.validate();
		rankingPanel.add(rankList);
		rankingPanel.setBackground(Color.DARK_GRAY);
		rankingPanel.validate();
	}

	private void changeDotFocus(RankedListItem item) {
		this.vis.changeDot(createDotVis(item.getId()), true);
	}
	
	/**
	 * Attempt to center and zoom but seems not possible with public api.
	 * @param item
	 */
	private void tryToCenter(RankedListItem item) {
		ExoDotTransition exDot = findTransition(item.getId(), this.vis.getDot());
		double x=0.0,y=0.0;
		boolean xSet = false,ySet = false;
		System.out.println("-- DEBUG FIND SVG X,Y --");
		System.out.println("Can find element using id? :: "+this.vis.getSVG().getElement(exDot.getId()).toString());
		System.out.println("SVG inline attributes :: "+ this.vis.getSVG().getElement(exDot.getId()).getInlineAttributes().toString());
		System.out.println("SVG presentation attributes :: "+ this.vis.getSVG().getElement(exDot.getId()).getPresentationAttributes().toString());
		System.out.println("SVG children :: "+ this.vis.getSVG().getElement(exDot.getId()).getChildren().size());
		for (SVGElement el : this.vis.getSVG().getElement(exDot.getId()).getChildren()) {
			System.out.println("Child tagname ::"+ el.getTagName());
			if (el.getId() != null) {
				System.out.println("Child Id :: " + el.getId().toString());
			}
			System.out.println("Child inline attributes :: "+ el.getInlineAttributes().toString());
			System.out.println("Child presentation attributes :: "+ el.getPresentationAttributes().toString());
			if (el.getPresentationAttributes().contains("x")) {
				System.out.println("Child has x of :: "+ el.getPresAbsolute("x").getDoubleValue() );
				x = el.getPresAbsolute("x").getDoubleValue();
				xSet = true;
			}
			if (el.getPresentationAttributes().contains("y")) {
				System.out.println("Child has y of :: "+ el.getPresAbsolute("y").getDoubleValue() );
				y = el.getPresAbsolute("y").getDoubleValue();
				ySet =true;
			}
				
			
		}
//		find some way to call, however I need a x,y point on the svg 
//		found some x , y on child of element
//		using thoses and functions on vis to find a navigation point to focus on
		try {
			if (xSet && ySet) {
				Point2D p = this.vis.transformImageToNavigation(new Point2D.Double(x, y));
//				centers but does not account for the viewport size
//				also zoom in is private
				this.vis.centerImageAround(new Point((int)p.getX(),(int)p.getY()));
			}
		} catch (NoninvertibleTransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private Dot createDotVis() {
		ExogenousEnhancementDotPanel dotp = this.source.getSource().getVis();
		return DotGraphVisualisation.builder()
				.graph(dotp.getGraph())
				.updatedGraph(dotp.getUpdatedGraph())
				.swapMap(dotp.getSwapMap())
				.rules(dotp.getRules())
				.transMapping(dotp.getSource().getFocus().getTask().getTransMap())
				.build()
				.make()
				.getVisualisation();
	}
	
	private ExoDotTransition findTransition(String transId, Dot dot) {
		ExoDotTransition exNode = null;
//		find transition being displayed
		for(DotNode node : dot.getNodes()) {
			if (node.getClass().equals(ExoDotTransition.class)) {
				exNode = (ExoDotTransition) node;
//				System.out.println("checking node="+exNode.getControlFlowId());
				if (transId.contains(exNode.getControlFlowId())) {
					exNode.highlightNode();
					break;
				}
			}
		}
		return exNode;
	}
	
	private Dot createDotVis(String transId) {
		ExogenousEnhancementDotPanel dotp = this.source.getSource().getVis();
		Dot newDot = DotGraphVisualisation.builder()
				.graph(dotp.getGraph())
				.updatedGraph(dotp.getUpdatedGraph())
				.swapMap(dotp.getSwapMap())
				.rules(dotp.getRules())
				.transMapping(dotp.getSource().getFocus().getTask().getTransMap())
				.build()
				.make()
				.getVisualisation();
//		find transition being displayed
		ExoDotTransition exNode = findTransition(transId, newDot);
		if (exNode != null) {
			exNode.highlightNode();
		}
		return newDot;
	}
	
	
	@Builder
	public static class RankedListItem {
		
		@Getter private EnhancementExogenousDatasetGraphController controller;
		@Getter private String id;
		
		@Default @Getter private double rankDistance =0.0;
		@Default @Getter private boolean ranked = false;
		@Default @Getter @Setter private int rank = 1;
		@Default @Getter @Setter private int wrank = 1;
		
		@Default private List<Integer> common = new ArrayList();
		@Default @Getter private int commonLength = -1;
		@Default @Getter private double wilcoxonP = Double.MAX_VALUE;
		
		public RankedListItem rank() {
			System.out.println("[RankedItem] Starting ranking");
			double distance = Double.MIN_NORMAL;
			try {
//			check that the median graph has been computed
			if (controller.getCachedGraphs().containsKey("Median")) {
				EnhancementMedianGraph source = (EnhancementMedianGraph) controller.getCachedGraphs().get("Median");
				// check that the graph has been computed
				if (!source.isDone()) {
					source.make();
				}
//				once computed then collect data from median outcome
				List<YIntervalSeries> datasets = new ArrayList<YIntervalSeries>();
				if (source.getTrueMedianDataset() != null && source.getTrueMedianDataset().getItemCount() > 0) {
					datasets.add(source.getTrueMedianDataset());
				}
				if (source.getFalseMedianDataset() != null && source.getFalseMedianDataset().getItemCount() > 0) {
					datasets.add(source.getFalseMedianDataset());
				}
				if (source.getNullMedianDataset() != null && source.getNullMedianDataset().getItemCount() > 0) {
					datasets.add(source.getNullMedianDataset());
				}
//				compute distance between all datasets
				for (YIntervalSeries data : datasets) {
					if (data.getItemCount() < 1) {
						continue;
					}
					System.out.println("[RankedItem] building source vector");
//					create vector the source
					final double highY = IntStream.range(0, data.getItemCount())
							.sequential()
							.mapToDouble(data::getYValue)
							.reduce(Double.MIN_VALUE, Double::max);
					final double lowY = 
							IntStream.range(0, data.getItemCount())
							.sequential()
							.mapToDouble(data::getYValue)
							.reduce(Double.MAX_VALUE, Double::min);
					for(YIntervalSeries other: datasets) {
						if (other.equals(data) || other.getItemCount() < 1) {
							continue;
						}
//						create vector for other datasets
						System.out.println("[RankedItem] building other vector");
						final double otherHighY = 
								IntStream.range(0, other.getItemCount())
								.sequential()
								.mapToDouble(other::getYValue)
								.reduce(Double.MIN_VALUE, Double::max);
						final double otherLowY = 
								IntStream.range(0, other.getItemCount())
								.sequential()
								.mapToDouble(other::getYValue)
								.reduce(Double.MAX_VALUE, Double::min);
						final double lowlowY = Math.min(lowY, otherLowY);
						final double highhighY = Math.max(highY, otherHighY);
						FeatureVector dataVector = FeatureVectorImpl.builder()
								.values(
										IntStream.range(0, data.getItemCount())
										.sequential()
										.mapToDouble(data::getYValue)
										.map(val -> (val - lowlowY)/(lowlowY - highhighY)) // min-max feature scaling
										.boxed()
										.collect(Collectors.toList())
								)
								.columns(
										IntStream.range(0, data.getItemCount())
										.mapToDouble(idx -> data.getX(idx).doubleValue())
										.boxed()
										.map(item -> item.toString())
										.collect(Collectors.toList())
								).build();
						FeatureVector otherVector = FeatureVectorImpl.builder()
								.values(
										IntStream.range(0, other.getItemCount())
										.mapToDouble(other::getYValue)
										.map(val -> (val - lowlowY)/(lowlowY - highhighY)) // min-max feature scaling
										.boxed()
										.collect(Collectors.toList())
								)
								.columns(
										IntStream.range(0, other.getItemCount())
										.mapToDouble(idx -> other.getX(idx).doubleValue())
										.boxed()
										.map(item -> item.toString())
										.collect(Collectors.toList())
								).build();
//						work out the longest common length between vectors
						common = WilcoxonSignedRankTester.findLongestMatchingVector(otherVector, dataVector);
						System.out.println("[RankedItem] longest common length :: "+common.size());
						if (common.size() > 0) {
							commonLength = common.size();
//							find 20 samples over interval
							if (common.size() > 20) {
								int start = common.get(0);
								int end = common.get(common.size()-1);
								double spacing = (end-start)/19.0;
								double counter = start;
								List<Integer> temp = new ArrayList<Integer>();
								temp.add(start);
								counter += spacing; 
								start = (int) Math.ceil(counter);
								while (counter < end) {
									temp.add(start);
									counter += spacing; 
									start = (int) Math.ceil(counter);
								}
								temp.add(end);
								common = temp;
							}
							List<Double> X1 = common.stream()
									.map(i -> dataVector.getValues().get(i))
									.map(i -> (i * (lowlowY - highhighY))+ lowlowY)
									.collect(Collectors.toList());
							List<Double> X2 = common.stream()
									.map(i -> otherVector.getValues().get(i))
									.map(i -> (i * (lowlowY - highhighY))+ lowlowY)
									.collect(Collectors.toList());
							if (wilcoxonP == Double.MAX_VALUE) {
								wilcoxonP = WilcoxonSignedRankTester.computeTest(X1, X2);
							} else {
								wilcoxonP += WilcoxonSignedRankTester.computeTest(X1, X2);
							}
							System.out.println("[RankedItem] wilcoxon p-value ::" +wilcoxonP);
						} else {
//							wilcoxonP = -1;
						}
//						compute dynamic time warping distance between data and other
						System.out.println("[RankedItem] computing distance");
						distance = distance + DynamicTimeWarpingDistancer.builder()
							.build()
							.distance(dataVector, otherVector);
					}
				}
				
			}
			} catch (Exception e) {
				System.out.println("[ExogenousEnhancementGraphInvestigator-RankedItem] Unable to compute rank");
				e.printStackTrace();
			}
			System.out.println("[RankedItem] ranked computed :: "+distance);
			this.ranked = true;
			this.rankDistance = distance;
			return this;
		}
		
		public Boolean rankable() {
			return this.rankDistance != Double.MIN_NORMAL && this.wilcoxonP != Double.MAX_VALUE && commonLength != -1;
		}
		
		public String toString() {
			return "Rank=" + (this.ranked ? "("+this.rank+"-"+this.wrank+")" : "N/A") +
				   " || Common="+ commonLength +
				   " || Distance="  + (rankable() ? String.format("%.3f",this.rankDistance) : "N/A") +
				   " || wilcoxon="  + (rankable() ? String.format("%.3f",this.wilcoxonP) : "N/A");
		}
	}
	
	private class rankedListener implements PropertyChangeListener {

		private ExogenousEnhancementGraphInvestigator source;
		
		public rankedListener(ExogenousEnhancementGraphInvestigator source) {
			this.source = source;
		}
		
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().toString().equals("state")) {
//				System.out.println("state change");
				if(evt.getNewValue().equals(SwingWorker.StateValue.DONE)) {
//					System.out.println("work is done");
					this.source.changeToRankedList();
					
				}
			}
		}
		
	}
	
	private class SelectionListener implements MouseListener {
		
		private ExogenousEnhancementGraphInvestigator source; 
		private ProMList<RankedListItem> clicked;
		
		public SelectionListener(ExogenousEnhancementGraphInvestigator source, ProMList<RankedListItem> clicked) {
			this.source = source;
			this.clicked = clicked;
		}

		public void mouseClicked(MouseEvent e) {
			if (this.clicked.getSelectedValuesList().size() > 0) {
				this.source.swapGraphController(this.clicked.getSelectedValuesList().get(0));
				this.source.changeDotFocus(this.clicked.getSelectedValuesList().get(0));
			}
			
		}

		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
		
		
	}
	
	private class BackListener implements MouseListener {
		
		private JButton self;
		private ExogenousDiscoveryInvestigator controller;
		
		public BackListener(JButton self, ExogenousDiscoveryInvestigator controller) {
			this.self = self;
			this.controller = controller;
		}

		public void mouseClicked(MouseEvent e) {
			if (this.self.isEnabled()) {
				this.controller.switchView(this.controller.enhancementTracablityViewKey);
			}
			
		}

		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
	}

	private class ExportListener implements MouseListener {

		private ExogenousEnhancementGraphInvestigator gui;
		private JButton button;
		
		public ExportListener(ExogenousEnhancementGraphInvestigator gui) {
			this.gui = gui;
			this.button = this.gui.export;
		}
		
		public void mouseClicked(MouseEvent e) {
			if (this.button.isEnabled()) {
//				create worker and move to progress panel
				progressLabel.setText("Exporting graphs in ranked order...");
				this.gui.changeToProgress();
				EnhancementRankExporter.builder()
					.gui(gui)
					.ranks(gui.rankerWorker.getOutcome())
					.progress(gui.progresser)
					.build()
					.execute();
				this.button.setEnabled(false);
				this.gui.getBack().setEnabled(false);
			}
			
		}

		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
