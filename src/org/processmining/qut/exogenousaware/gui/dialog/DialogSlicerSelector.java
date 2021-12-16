package org.processmining.qut.exogenousaware.gui.dialog;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.processmining.qut.exogenousaware.steps.slicing.Slicer;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Singular;

@Builder
public class DialogSlicerSelector extends JPanel {

	@Singular private List<String> datasetNames;
	@Default private Boolean targeted = false;
	
	@Default private GridBagConstraints c = new GridBagConstraints();
	
	private JSpinner spinner; 
	
	public DialogSlicerSelector setup() {
//		style panel
		setBackground(Color.black);
//		setup layout and manager
		setLayout(new GridBagLayout());
		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx= 0;
		c.gridy=0;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.NONE;
//		add type drop down
		c.insets = new Insets(5,5,5,0);
		c.anchor = GridBagConstraints.LAST_LINE_END;
		JLabel slicerLabel = new JLabel("Slicer Type:");
		slicerLabel.setForeground(Color.white);
		add(slicerLabel, c);
		c.gridx++;
		c.anchor = GridBagConstraints.LAST_LINE_START;
		c.insets = new Insets(0,5,5,25);
		JComboBox<String> list = new JComboBox<String>();
		list.addItem(SlicerChoices.FUTURE_OUTCOME_SLICER.label);
		list.addItem(SlicerChoices.PAST_OUTCOME_SLICER.label);
		list.addItem(SlicerChoices.TIME_AWARE_SLICER.label);
		add(list, c);
		c.gridx++;
//		add period input
		c.insets = new Insets(5,5,5,0);
		c.anchor = GridBagConstraints.LAST_LINE_END;
		JLabel spinnerlabel = new JLabel("Time Period:");
		spinnerlabel.setForeground(Color.white);
		add(spinnerlabel, c);
		c.gridx++;
		c.insets = new Insets(0,5,5,25);
		c.anchor = GridBagConstraints.LAST_LINE_START;
		SpinnerModel model = new SpinnerNumberModel(10, 0, Integer.MAX_VALUE, 1);
		spinner = new JSpinner(model);
		add(spinner, c);
		c.insets = new Insets(5,5,5,25);
		c.anchor = GridBagConstraints.CENTER;
		c.gridx++;
//		add name for slicer
		JLabel name = new JLabel("TODO::Name");
		name.setForeground(Color.white);
		add(name, c);
		c.gridx++;
//		add remove
		c.insets = new Insets(5,5,5,5);
		JButton remove = new JButton("X");
		remove.setForeground(Color.white);
		remove.setBackground(Color.gray);
		add(remove, c);
		this.validate();
		return this;
	}
	
	public Slicer makeSlicer() {
		return null;
	}
	
	private enum SlicerChoices {
		TIME_AWARE_SLICER("TAS", true),
		PAST_OUTCOME_SLICER("POS",  false),
		FUTURE_OUTCOME_SLICER("FOS", true);
		
		public String label;
		public Boolean requirePeriod;
		
		private SlicerChoices(String label,Boolean require) {
			this.label = label;
			this.requirePeriod = require;
		}
	}
}
