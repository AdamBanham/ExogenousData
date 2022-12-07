package org.processmining.qut.exogenousaware.gui.panels;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import org.processmining.framework.util.ui.widgets.ProMList;
import org.processmining.qut.exogenousaware.data.storage.workers.InvestigationTask.MinerType;
import org.processmining.qut.exogenousaware.gui.ExogenousDiscoveryInvestigator;
import org.processmining.qut.exogenousaware.gui.styles.PanelStyler;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import prefuse.data.query.ListModel;

@Builder
@Data
public class ExogenousInvestigatorSelectionPanel {
//	builder parameters
	@NonNull private List<String> exoVariables;
	@NonNull private List<String> endoVariables;
	@NonNull private ExogenousDiscoveryInvestigator source;
	
//	gui widgets
	@Getter @Default private JPanel main = new JPanel();
	@Getter @Default private JTabbedPane tabs = null;
//	miner selection
	@Getter @Default private ProMList<MinerType> selectedMiner = null;
//	miner parameters
	@Default @Getter private ExogenousInvestigatorDecisionMinerSelector decisionMinerConfig = 
			ExogenousInvestigatorDecisionMinerSelector.builder().build().setup();
//	variable selection
	@Getter @Default private ProMList<String> selectedEndoVariables = null;
	@Getter @Default private ProMList<String> selectedExoVariables = null;
//	controls 
	@Getter @Default private JButton investigate = new JButton("Start Decision Mining");
	@Getter @Default private JButton enhance = new JButton("Move to Enhancement");
	@Getter @Default private JButton measure = new JButton("Measure Model");
	
//	grid view of panel
	
//	| 			text -> span 10 			|
//	| list -> span 5    | list -> span 5    |
//	| b | b |   |   |   |   |   |   |   | b |
	
	public ExogenousInvestigatorSelectionPanel setup() {
		this.main.setLayout(new GridBagLayout());
		GridBagConstraints sub_c = new GridBagConstraints();
		sub_c.fill = GridBagConstraints.NONE;
		sub_c.gridx = 0;
		sub_c.gridy = 0;
		sub_c.weightx = 0;
		sub_c.weighty = 0;
		sub_c.insets = new Insets(5,5,5,5);
		sub_c.anchor = GridBagConstraints.LINE_START;
//		add compontents
//		add a description at top of panel
		JLabel info = new JLabel("To investigate the influence between exogenous "
				+ "or endogenous variables and decision points, select a decision "
				+ "miner "
				+ ", approciate variables, miner parameters, and then click the button "
				+ "labelled 'start decision mining'.");
		info.setForeground(Color.white);
		sub_c.gridwidth = 10;
		this.main.add(info, sub_c);
//		#1 add tabs for options
		sub_c.gridy = 1;
		sub_c.weightx = 0.5;
		sub_c.gridx = 0;
		sub_c.gridwidth = 10;
		sub_c.weighty = 1;
		sub_c.fill = GridBagConstraints.BOTH;
		this.tabs = new JTabbedPane();
		this.tabs.setUI(new CustomTabbedPaneUI());
		this.main.add(tabs, sub_c);
		this.tabs.setBackground(Color.LIGHT_GRAY);
//		#2 add decision miner selection 
		JPanel select = new JPanel(); 
		PanelStyler.StylePanel(select, true, BoxLayout.X_AXIS);
		tabs.addTab("Decision Miner", select);
//		add selection list for decision miners 
		this.selectedMiner = new ProMList<MinerType>("Decision Miner",
				new ListModel() {{
					addElement(MinerType.OVERLAPPING);
					addElement(MinerType.DECISIONTREE);
					addElement(MinerType.DISCRIMINATING);
				}}
		);
		this.selectedMiner.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.selectedMiner.setSelectedIndex(0);
		select.add(this.selectedMiner);
//		#3 add variable selection
		select = new JPanel();
		PanelStyler.StylePanel(select, true, BoxLayout.X_AXIS);
		tabs.addTab("Variables", select);
//		add selection lists
		ProMList<String> exoPanel = new ProMList<String>("Exogenous Variables",
				new ListModel(this.exoVariables.toArray()));
		this.selectedExoVariables = exoPanel;
		select.add(exoPanel,sub_c);
		ProMList<String> endoPanel = new ProMList<String>("Endogenous Variables",
				new ListModel(this.endoVariables.toArray()));
		this.selectedEndoVariables = endoPanel;
		select.add(endoPanel, sub_c);
		select.validate();
//		#4 add parameter selection
		select = decisionMinerConfig;
		PanelStyler.StylePanel(select);
		tabs.addTab("Miner Parameters", select);
//		add a investigate button
		sub_c.fill = GridBagConstraints.NONE;
		sub_c.gridwidth = 1;
		sub_c.gridx = 0;
		sub_c.gridy = 3;
		sub_c.weightx = 0;
		sub_c.weighty = 0.05;
		sub_c.anchor = GridBagConstraints.SOUTH;
		this.investigate.addMouseListener(new InvestigationListener(this.source));
		this.main.add(this.investigate, sub_c);
//		add a measurement button
		sub_c.gridx = 1;
		this.main.add(this.measure, sub_c);
		this.measure.addMouseListener(new MeasurementListener(this.measure, this.source));
		this.measure.setEnabled(false);
//		add a discovery button
		this.enhance.setEnabled(false);
		sub_c.anchor = GridBagConstraints.SOUTHEAST;
		sub_c.gridx = 9;
		this.enhance.addMouseListener(new EnhancementListener(this.enhance,this.source));
		this.main.add(this.enhance, sub_c);
//		add panel
		this.main.setBackground(Color.DARK_GRAY);
		return this;
	}
	
	public class MeasurementListener implements MouseListener{
		
		private JButton clicked;
		private ExogenousDiscoveryInvestigator source;
		
		public MeasurementListener(JButton clicked, ExogenousDiscoveryInvestigator source) {
			this.clicked = clicked;
			this.source = source;
		}

		public void mouseClicked(MouseEvent e) {
			if (this.clicked.isEnabled()) {
				this.source.runMeasurements();
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
	
	public class EnhancementListener implements MouseListener {

		private JButton clicked;
		private ExogenousDiscoveryInvestigator source;
		
		public EnhancementListener(JButton clicked, ExogenousDiscoveryInvestigator source) {
			this.clicked = clicked;
			this.source = source;
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			if (this.clicked.isEnabled()) {
				this.source.buildEnhancementView();
			}
			
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public class InvestigationListener implements MouseListener {
		
		private ExogenousDiscoveryInvestigator main;
		
		public InvestigationListener(ExogenousDiscoveryInvestigator main) {
			this.main = main;
		}

		@Override
		public void mouseClicked(MouseEvent arg0) {
			try {
				this.main.runInvestigation();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void mouseEntered(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseExited(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mousePressed(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseReleased(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	/**
	 * All this to change the look and feel...
	 * @author Adam Banham
	 *
	 */
	private class CustomTabbedPaneUI extends BasicTabbedPaneUI {
		
		protected Color selectedColor;
		protected boolean tabsOverlapBorder;
		protected boolean tabsOpaque;
		protected boolean contentOpaque;
		
		@Override 
		protected void installDefaults() {
	        LookAndFeel.installColorsAndFont(tabPane, "TabbedPane.background",
	                                    "TabbedPane.foreground", "TabbedPane.font");
	        highlight = Color.BLACK;
	        lightHighlight = Color.DARK_GRAY;
	        shadow = Color.LIGHT_GRAY;
	        darkShadow = Color.DARK_GRAY;
	        focus = Color.LIGHT_GRAY;
	        selectedColor = Color.LIGHT_GRAY;

	        textIconGap = UIManager.getInt("TabbedPane.textIconGap");
	        tabInsets = UIManager.getInsets("TabbedPane.tabInsets");
	        selectedTabPadInsets = UIManager.getInsets("TabbedPane.selectedTabPadInsets");
	        tabAreaInsets = new Insets(0,15,0,0);
	        tabsOverlapBorder = UIManager.getBoolean("TabbedPane.tabsOverlapBorder");
	        contentBorderInsets = new Insets(0,0,0,0);
	        tabRunOverlay = UIManager.getInt("TabbedPane.tabRunOverlay");
	        tabsOpaque = UIManager.getBoolean("TabbedPane.tabsOpaque");
	        contentOpaque = UIManager.getBoolean("TabbedPane.contentOpaque");
	        Object opaque = UIManager.get("TabbedPane.opaque");
	        if (opaque == null) {
	            opaque = Boolean.FALSE;
	        }
	        LookAndFeel.installProperty(tabPane, "opaque", opaque);

	        // Fix for 6711145 BasicTabbedPanuUI should not throw a NPE if these
	        // keys are missing. So we are setting them to there default values here
	        // if the keys are missing.
	        if (tabInsets == null) tabInsets = new Insets(0,4,1,4);
	        if (selectedTabPadInsets == null) selectedTabPadInsets = new Insets(2,2,2,1);
	        if (tabAreaInsets == null) tabAreaInsets = new Insets(3,2,0,2);
	        if (contentBorderInsets == null) contentBorderInsets = new Insets(2,2,3,3);
	    }
		
		
		@Override
		protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
	        int width = tabPane.getWidth();
	        int height = tabPane.getHeight();
	        Insets insets = tabPane.getInsets();
	        Insets tabAreaInsets = getTabAreaInsets(tabPlacement);

	        int x = insets.left;
	        int y = insets.top;
	        int w = width - insets.right - insets.left;
	        int h = height - insets.top - insets.bottom;

	        switch(tabPlacement) {
	          case LEFT:
	              x += calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
	              if (tabsOverlapBorder) {
	                  x -= tabAreaInsets.right;
	              }
	              w -= (x - insets.left);
	              break;
	          case RIGHT:
	              w -= calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
	              if (tabsOverlapBorder) {
	                  w += tabAreaInsets.left;
	              }
	              break;
	          case BOTTOM:
	              h -= calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
	              if (tabsOverlapBorder) {
	                  h += tabAreaInsets.top;
	              }
	              break;
	          case TOP:
	          default:
	              y += calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
	              if (tabsOverlapBorder) {
	                  y -= tabAreaInsets.bottom;
	              }
	              h -= (y - insets.top);
	        }

	            if ( tabPane.getTabCount() > 0 && (contentOpaque || tabPane.isOpaque()) ) {
	            // Fill region behind content area
	            Color color = Color.DARK_GRAY;
	            if (color != null) {
	                g.setColor(color);
	            }
	            else if ( selectedColor == null || selectedIndex == -1 ) {
	                g.setColor(tabPane.getBackground());
	            }
	            else {
	                g.setColor(selectedColor);
	            }
	            g.fillRect(x,y,w,h);
	        }

	        paintContentBorderTopEdge(g, tabPlacement, selectedIndex, x, y, w, h);
	        paintContentBorderLeftEdge(g, tabPlacement, selectedIndex, x, y, w, h);
	        paintContentBorderBottomEdge(g, tabPlacement, selectedIndex, x, y, w, h);
	        paintContentBorderRightEdge(g, tabPlacement, selectedIndex, x, y, w, h);

	    }
	}
	
	
}
