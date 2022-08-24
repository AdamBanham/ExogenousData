package org.processmining.qut.exogenousaware.gui.panels;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.framework.util.ui.widgets.ProMScrollPane;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithData;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.qut.exogenousaware.gui.dot.DotGraphVisualisation;
import org.processmining.qut.exogenousaware.gui.dot.panels.DotPanelG2;
import org.processmining.qut.exogenousaware.gui.styles.ScrollbarStyler;
import org.processmining.qut.exogenousaware.stats.models.ProcessModelStatistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExogenousInvestigatorDotPanel  {
//	builder parameters
	@Getter @NonNull private PetriNetWithData graph;

//	gui widgets
	@Getter @Default private DotController controller= null;
	@Getter private JPanel main;
	@Getter private DotPanelG2 vis;
	
//	internal states
	private DotGraphVisualisation visBuilder;
	@Default @Setter private Map<String, GuardExpression> rules = null;
	@Default @Setter private Map<String,String> swapMap = null;
	@Default @Setter private PetriNetWithData updatedGraph = null;
	@Getter @Setter private ProcessModelStatistics modelLogInfo;
	@Getter @Setter private Map<Transition,Transition> transMapping;
	
	public ExogenousInvestigatorDotPanel setup() {
		this.visBuilder = DotGraphVisualisation.builder()
				.graph(graph)
				.swapMap(new HashMap())
				.build()
				.make();
		this.controller = new DotController(this, visBuilder.getVisualisation());
		this.main = controller;
		this.vis = controller.getVis();
		this.main.validate();
		
		return this;
	}
	
	public void update() {
		
		this.visBuilder = DotGraphVisualisation.builder()
				.graph(this.graph)
				.updatedGraph(this.updatedGraph)
				.modelLogInfo(modelLogInfo)
				.swapMap(this.swapMap != null ? this.swapMap : new HashMap())
				.transMapping(transMapping)
				.rules(this.rules)
				.build()
				.make();
		this.vis.changeDot(visBuilder.getVisualisation(), false);
		
		if (modelLogInfo != null) {
			vis.setStatistics(modelLogInfo);
		}
	}
	
	@Builder
	public static class DotOverlayInformationDump {
		
		@NonNull DotNode node;
		@NonNull PetrinetNode controlNode;
		@NonNull ProcessModelStatistics statistics;
		@NonNull DotOverlay overlay;
		
		public DotOverlayInformationDump setup() {
			JPanel scrollable = overlay.getScrollable();
			int height = 0;
			scrollable.removeAll();
			
			GridBagConstraints c = overlay.createConstraints();
			
			scrollable.add(Box.createVerticalStrut(50), c);
			c.gridy++;
			
			
			scrollable.add(new JLabel(findSimpleLabel()), c);
			c.gridy++;
			
			scrollable.add(new JLabel("Transition Guard:"), c);
			c.gridy++;
			
			scrollable.add(new JLabel("TODO"), c);
			c.gridy++;
			
			if (controlNode instanceof Place) {
				if (statistics.isDecisionMoment((Place) controlNode)) {
//					TODO add work for overlay to build out 
				}
			}
			
			if (controlNode instanceof Transition) {
//				TODO add work for overlay to build out
			}
		
			overlay.setVisible(true);
			
			scrollable.validate();
			overlay.getScroller().revalidate();
			overlay.getScroller().getParent().revalidate();
			overlay.getScroller().getParent().repaint();
			return this;
		}
		
		private String findSimpleLabel() {
			String label = controlNode.getLabel();
			
			
			return label;
		}
		
		
		
	}
	
	public static class DotOverlay {
		
//		internal states
		@Getter private JPanel scrollable = new JPanel();
		@Getter private ProMScrollPane scroller = new ProMScrollPane(scrollable);
		private Color transGray = new Color(128, 128, 128, 175);
		
		public DotOverlay() {
			GridBagConstraints c = createConstraints();
//			setup scrollable
			scrollable.setBackground(transGray);
			scrollable.setLayout(new GridBagLayout());
			scrollable.add(new JLabel("Transition A"), c);
			c.gridy++;
			scrollable.add( Box.createVerticalStrut(800), c);
//			setup scroller
			scroller.setBackground(transGray);
			scroller.setPreferredSize(new Dimension(250,75));
			ScrollbarStyler.styleScrollBar(scroller.getHorizontalScrollBar());
			ScrollbarStyler.styleScrollBar(scroller.getVerticalScrollBar());
			scroller.setVisible(false);
		}
		
		public GridBagConstraints createConstraints() {
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = 1.0;
			c.weighty = 0.0;
			c.fill = c.NONE;
//			c.anchor = c.NORTH;
			c.insets = new Insets(5,5,5,5);
			c.gridx = 0;
			c.gridy = 0;
			
			return c;
		}
		
		public void setVisible(boolean visible) {
			scroller.setVisible(visible);
		}
		
		public void toggleVisible() {
			scroller.setVisible(!scroller.isVisible());
		}
		
		
	}
	
	public class DotController extends JPanel {
		
		@Getter private DotPanelG2 vis;
		@Getter private JPanel main;
		@Getter private JPanel clickable;
		@Getter private DotOverlay overlay;
		private Dot graph;
		private ExogenousInvestigatorDotPanel source;
		
		public DotController(ExogenousInvestigatorDotPanel source ,Dot graph) {
			this.source = source;
			this.graph = graph;
			this.main = this;
			setup();
		}
		
		private void setup() {
			setBackground(Color.LIGHT_GRAY);
			
//			setup layout settings
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = 1.0;
			c.weighty = 1.0;
//			make main panel
			setLayout(new GridBagLayout());	
			
//			add clickable for info panel
			clickable = new JPanel();
			clickable.setLayout(new GridBagLayout());
//			add some visible icons for hide/unhide
			c.gridx = 0;
			c.gridy = 0;
			c.weightx = 1.0;
			c.weighty = 1.0;
			clickable.add(Box.createGlue(), c);
			c.gridy++;
			c.weighty = 0.;
			clickable.add(new JLabel("<"), c);
			c.gridy++;
			clickable.add(new JLabel(">"), c);
			c.weighty = 1.0;
			c.gridy++;
			clickable.add(Box.createGlue(), c);
			clickable.setBackground(Color.LIGHT_GRAY);
			clickable.setPreferredSize(new Dimension(15,75));
//			add in clickable
			c.weightx = 0.1;
			c.weighty = 1.0;
			c.gridy = 0;
			c.gridwidth = 1;
			c.anchor = c.EAST;
			c.gridx = 4;
			c.fill = c.VERTICAL;
			add(clickable, c);
//			add overlay
			overlay = new DotOverlay();
			
//			add scroll info panel
			c.weightx = 0.1;
			c.weighty = 1.0;
			c.insets = new Insets(0,0,0,15);
			c.gridy = 0;
			c.gridx = 4;
			c.gridwidth = 1;
			c.anchor = c.EAST;
			c.fill = c.VERTICAL;
			add(overlay.getScroller(),c);

			
//			make dot visualiser
			this.vis = new DotPanelG2(this.graph);
			vis.setBackground(Color.LIGHT_GRAY);
			System.out.println("Adding dot graph visualiser...");
			c.weightx = 1.0;
			c.weighty = 1.0;
			c.gridx = 0;
			c.gridy = 0;
			c.anchor = c.WEST;
			c.fill = c.BOTH;
			c.gridwidth = 5;
			add(this.vis, c);
//			handle repaints on info panel
			overlay.getScroller().getVerticalScrollBar().
			addAdjustmentListener(new AdjustmentListener() {
				
				private long lastBounce = -1;
				
				public void adjustmentValueChanged(AdjustmentEvent e) {
					// TODO Auto-generated method stub
					long time = System.nanoTime();
					if (lastBounce < 0) {
						lastBounce = time;
						getMain().repaint();
						getOverlay().getScroller().repaint();
					}
					else if ((time - lastBounce) > 25) {
						lastBounce = time;
						getMain().repaint();
						getOverlay().getScroller().repaint();
					}
				}
			});
			
//			add listener to hide info panel
			clickable.addMouseListener(new MouseListener() {
				
						private DotOverlay overlay = getOverlay();
						private JPanel vis = getVis();
						private JPanel host = getMain();
											
						public void mouseClicked(MouseEvent e) {
							overlay.toggleVisible();
							host.revalidate();
							vis.repaint();
							host.repaint();
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
			});
			
			validate();
		}
		
	}
	
	
	
}
