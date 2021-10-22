package org.processmining.qut.exogenousaware.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithData;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.qut.exogenousaware.data.storage.ExogenousDiscoveryInvestigation;
import org.processmining.qut.exogenousaware.gui.dot.ExoDotTransition;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousEnhancementAnalysis;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousEnhancementDotPanel;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Builder
@Data
public class ExogenousEnhancementTracablity {

	@NonNull private ExogenousDiscoveryInvestigator source;
	@NonNull @Setter private ExogenousDiscoveryInvestigation focus;
	@NonNull private PetriNetWithData controlflow;
	@NonNull private PNRepResult alignment;
	
	@Default @Getter private JPanel main = new JPanel();
	@Default private GridBagConstraints c = new GridBagConstraints();
	@Default private ExogenousEnhancementDotPanel vis = null;
	@Default private ExogenousEnhancementAnalysis analysis = null;
	@Default @Getter private JButton back = new JButton("back");
	@Default @Getter private JButton right = new JButton("search");
	
	
	public ExogenousEnhancementTracablity setup() {
//		setup and style main
		this.main.setLayout(new GridBagLayout());
		this.main.setBackground(Color.DARK_GRAY);
//		configure constraints
		this.c.fill = GridBagConstraints.BOTH;
		this.c.gridheight = 1;
		this.c.gridwidth = 2;
		this.c.weightx = 1;
		this.c.weighty = 0.4;
		this.c.gridx = 0;
		this.c.gridy = 1;
		this.c.insets = new Insets(5,5,5,5);
//		build vis panel
		this.c.ipady = 100;
		if (this.vis == null) {
			this.vis = ExogenousEnhancementDotPanel.builder()
						.graph(this.controlflow)
						.rules(this.focus.getFoundExpressions())
						.swapMap(this.focus.getTask().getConveretedNames())
						.updatedGraph(this.focus.getOutcome())
						.build()
						.setup();
			this.vis.update(this);
		}
		this.main.add(this.vis.getMain(), c);
//		build point analysis
		this.c.ipady = 0;
		this.c.gridy++;
		this.c.weighty = 0.6;
		this.analysis = ExogenousEnhancementAnalysis.builder()
				.source(this)
				.build()
				.setup();
		this.main.add(this.analysis.getScroll(), c);
//		add control buttons below analysis
//		add back button to investigator panel
		this.c.fill = GridBagConstraints.NONE;
		this.c.weighty = 0.1;
		this.c.weightx = 0.5;
		this.c.gridwidth = 1;
		this.c.gridy++;
		this.c.gridx = 0;
		this.c.anchor = GridBagConstraints.FIRST_LINE_START;
		this.back.addMouseListener(new BackListener(this.back, this.source));
		this.main.add(this.back, this.c);
//		add button to go right ?
		this.c.gridx++;
		this.c.anchor = GridBagConstraints.FIRST_LINE_END;
		this.right.setEnabled(true);
		this.right.addMouseListener(new SearchListener(right, this.source));
		this.main.add(this.right, this.c);
//		clean up main
		this.main.validate();
		return this;
	}
	
	public void update(ExogenousDiscoveryInvestigation newFocus) {
		this.setFocus(newFocus);
		this.vis.setRules(this.focus.getFoundExpressions());
		this.vis.setSwapMap(this.focus.getTask().getConveretedNames());
		this.vis.setUpdatedGraph(this.focus.getOutcome());
		this.vis.update(this);
		this.analysis.reset();
		this.main.validate();
	}
	
	public ExogenousEnhancementTracablity precompute() {
		
		return this;
	}
	
	public void updateAnalysisPanel(ExoDotTransition node) {
		this.analysis.updateAnalysis(node);
	}
	
	public class SearchListener implements MouseListener {

		private JButton clicked;
		private ExogenousDiscoveryInvestigator source;
		
		public SearchListener(JButton clicked,ExogenousDiscoveryInvestigator source ) {
			this.clicked = clicked;
			this.source = source;
		}
		
		public void mouseClicked(MouseEvent e) {
			if (this.clicked.isEnabled()) {
				this.source.switchView(this.source.enhancementSearchViewKey);
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
	
	public class BackListener implements MouseListener {
		
		private JButton clicked;
		private ExogenousDiscoveryInvestigator source;
		
		public BackListener(JButton clicked,ExogenousDiscoveryInvestigator source ) {
			this.clicked = clicked;
			this.source = source;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (this.clicked.isEnabled()) {
				this.source.switchView();
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
	
}
