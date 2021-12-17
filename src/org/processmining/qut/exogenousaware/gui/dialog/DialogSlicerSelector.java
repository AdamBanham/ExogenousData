package org.processmining.qut.exogenousaware.gui.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.processmining.qut.exogenousaware.steps.slicing.FutureOutcomeSlicer;
import org.processmining.qut.exogenousaware.steps.slicing.PastOutcomeSlicer;
import org.processmining.qut.exogenousaware.steps.slicing.Slicer;
import org.processmining.qut.exogenousaware.steps.slicing.TimeAwareSlicer;

import com.fluxicon.slickerbox.util.ColorUtils;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Singular;

@Builder
public class DialogSlicerSelector extends JPanel {

	@Singular
	private List<String> datasetNames;
	@Default
	private Boolean targeted = false;
	@Default
	@Getter
	private String target = "n/a";

	@Default
	private GridBagConstraints c = new GridBagConstraints();

	private JSpinner spinner;
	private JLabel spinnerlabel;
	private JComboBox<PeriodChoices> timeUnitSelector;
	private JComboBox<SlicerChoices> typeSelector;
	@Getter
	private JList<String> targets;

	public DialogSlicerSelector setup() {
		//		style panel
		setBackground(Color.black);
		//		setup layout and manager
		setLayout(new GridBagLayout());
		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(5, 5, 5, 5);
		//		add targets if needed
		if (this.targeted) {
			c.gridheight = 3;
			DefaultListModel<String> model = new DefaultListModel<>();
			for (String name : this.datasetNames) {
				model.addElement(name);
			}
			targets = new JList<String>(model);
			targets.setForeground(Color.black);
			targets.setVisible(true);
			targets.setCellRenderer(new DefaultListCellRenderer());
			targets.setFont(new Font("Times New Roman", Font.BOLD, 9));
			targets.validate();
			JScrollPane scrollPane = new JScrollPane(targets);
			scrollPane.setMaximumSize(new Dimension(200, 100));
			scrollPane.setMinimumSize(new Dimension(200, 100));
			scrollPane.setPreferredSize(new Dimension(200, 100));
			scrollPane.setBackground(Color.red);
			scrollPane.setVisible(true);
			scrollPane.validate();
			add(scrollPane, c);
			c.gridx++;
			c.gridy = 1;
		}
		//		add type drop down
		c.insets = new Insets(5, 5, 5, 0);
		JLabel slicerLabel = new JLabel("Slicer Type:");
		slicerLabel.setForeground(Color.white);
		add(slicerLabel, c);
		c.gridx++;
		c.insets = new Insets(0, 5, 5, 25);
		typeSelector = new JComboBox<SlicerChoices>();
		typeSelector.addItem(SlicerChoices.FUTURE_OUTCOME_SLICER);
		typeSelector.addItem(SlicerChoices.PAST_OUTCOME_SLICER);
		typeSelector.addItem(SlicerChoices.TIME_AWARE_SLICER);
		typeSelector.setFont(new Font("Times New Roman", Font.BOLD, 9));
		typeSelector.addItemListener(new SlicerTypeListener(this));
		add(typeSelector, c);
		c.gridx++;
		//		add period input
		c.insets = new Insets(5, 5, 5, 0);
		spinnerlabel = new JLabel("Time Period:");
		spinnerlabel.setForeground(Color.white);
		add(spinnerlabel, c);
		c.gridx++;
		c.insets = new Insets(0, 5, 5, 0);
		SpinnerModel model = new SpinnerNumberModel(10, 0, Integer.MAX_VALUE, 1);
		spinner = new JSpinner(model);
		spinner.setMaximumSize(new Dimension(75,25));
		spinner.setMinimumSize(new Dimension(75,25));
		spinner.setPreferredSize(new Dimension(75,25));
		add(spinner, c);
		//		add time period selector
		c.gridx++;
		c.insets = new Insets(0, 5, 5, 25);
		timeUnitSelector = new JComboBox<PeriodChoices>();
		timeUnitSelector.addItem(PeriodChoices.ms);
		timeUnitSelector.addItem(PeriodChoices.sec);
		timeUnitSelector.addItem(PeriodChoices.min);
		timeUnitSelector.addItem(PeriodChoices.hour);
		timeUnitSelector.addItem(PeriodChoices.day);
		timeUnitSelector.setFont(new Font("Times New Roman", Font.BOLD, 9));
		add(timeUnitSelector, c);
		c.insets = new Insets(5, 5, 5, 25);
		c.gridx++;
		//		add name for slicer
		JLabel name = new JLabel("TODO::Name");
		name.setForeground(Color.white);
		add(name, c);
		c.gridx++;
		//		add remove
		c.insets = new Insets(5, 5, 5, 5);
		JButton remove = new JButton("X");
		remove.setForeground(Color.white);
		remove.setBackground(ColorUtils.darken(Color.red, 150));
		remove.addMouseListener(new RemoveDialogListener(this));
		add(remove, c);
		//		set make size
		setMaximumSize(new Dimension(this.targeted ? 975 : 775, this.targeted ? 110 : 65));
		setMinimumSize(getMaximumSize());
		setPreferredSize(getMaximumSize());
		this.validate();
		return this;
	}

	public Slicer makeSlicer() {
		PeriodChoices period = (PeriodChoices) this.timeUnitSelector.getSelectedItem();
		if (typeSelector.getSelectedItem().equals(SlicerChoices.FUTURE_OUTCOME_SLICER)) {
			return FutureOutcomeSlicer.builder().timePeriod(new Long((int) this.spinner.getValue()) * period.timeMut)
					.build();
		} else if (typeSelector.getSelectedItem().equals(SlicerChoices.PAST_OUTCOME_SLICER)) {
			return PastOutcomeSlicer.builder().build();
		} else if (typeSelector.getSelectedItem().equals(SlicerChoices.TIME_AWARE_SLICER)) {
			return TimeAwareSlicer.builder().timePeriod(new Long((int) this.spinner.getValue()) * period.timeMut)
					.build();
		}
		return null;
	}

	private class SlicerTypeListener implements ItemListener {

		private DialogSlicerSelector source;

		public SlicerTypeListener(DialogSlicerSelector source) {
			this.source = source;
		}

		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == e.SELECTED) {
				SlicerChoices option = (SlicerChoices) e.getItem();
				this.source.spinner.setVisible(option.requirePeriod);
				this.source.spinnerlabel.setVisible(option.requirePeriod);
				this.source.timeUnitSelector.setVisible(option.requirePeriod);
				this.source.validate();
			}
		}

	}

	private class RemoveDialogListener implements MouseListener {

		private DialogSlicerSelector source;

		public RemoveDialogListener(DialogSlicerSelector source) {
			this.source = source;
		}

		public void mouseClicked(MouseEvent e) {
			this.source.firePropertyChange("remove-parent", false, true);
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

	private enum PeriodChoices {
		ms(1), sec(1000), min(1000 * 60), hour(1000 * 60 * 60), day(1000 * 60 * 60 * 24);

		public long timeMut;

		private PeriodChoices(long time) {
			this.timeMut = time;
		}
	}

	private enum SlicerChoices {
		TIME_AWARE_SLICER("TAS", true), PAST_OUTCOME_SLICER("POS", false), FUTURE_OUTCOME_SLICER("FOS", true);

		public String label;
		public Boolean requirePeriod;

		private SlicerChoices(String label, Boolean require) {
			this.label = label;
			this.requirePeriod = require;
		}

	}
}
