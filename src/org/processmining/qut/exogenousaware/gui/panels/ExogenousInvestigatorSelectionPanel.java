package org.processmining.qut.exogenousaware.gui.panels;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.processmining.framework.util.ui.widgets.ProMList;
import org.processmining.qut.exogenousaware.gui.ExogenousDiscoveryInvestigator;

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
	@Getter @Default private ProMList<String> selectedEndoVariables = null;
	@Getter @Default private ProMList<String> selectedExoVariables = null;
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
		JLabel info = new JLabel("To investigate the influence between exogenous or endogenous variables and decision points, select a variable combination below then click investigate.");
		info.setForeground(Color.white);
		sub_c.gridwidth = 10;
		this.main.add(info, sub_c);
//		add selection lists
		sub_c.gridy = 1;
		sub_c.weightx = 0.5;
		sub_c.gridx = 0;
		sub_c.gridwidth = 5;
		sub_c.weighty = 1;
		sub_c.fill = GridBagConstraints.BOTH;
		ProMList<String> exoPanel = new ProMList<String>("Exogenous Variables", new ListModel(this.exoVariables.toArray()));
		this.selectedExoVariables = exoPanel;
		this.main.add(exoPanel,sub_c);
		sub_c.gridx = 5;
		sub_c.gridwidth = 5;
		sub_c.weightx = 0.5;
		ProMList<String> endoPanel = new ProMList<String>("Endogenous Variables", new ListModel(this.endoVariables.toArray()));
		this.selectedEndoVariables = endoPanel;
		this.main.add(endoPanel, sub_c);
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
	
	
}
