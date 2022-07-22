package org.processmining.qut.exogenousaware.steps.slicing.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.qut.exogenousaware.data.ExogenousDataset;
import org.processmining.qut.exogenousaware.steps.determination.Determination;
import org.processmining.qut.exogenousaware.steps.slicing.FutureOutcomeSlicer;
import org.processmining.qut.exogenousaware.steps.slicing.PastOutcomeSlicer;
import org.processmining.qut.exogenousaware.steps.slicing.Slicer;
import org.processmining.qut.exogenousaware.steps.slicing.TimeAwareSlicer;
import org.processmining.qut.exogenousaware.steps.transform.type.EmptyTransform;

import com.fluxicon.slickerbox.util.ColorUtils;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Singular;

@Builder
public class DialogSlicerSelector extends JPanel {

	@Singular
	private List<ExogenousDataset> datasetNames;
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
	private ProMTextField namer;	

	@Getter
	private ProMComboBox<ExogenousDataset> targeter;

	public DialogSlicerSelector setup() {
		//		style panel
		setBackground(Color.GRAY);
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
			DefaultComboBoxModel<ExogenousDataset> model = new DefaultComboBoxModel<>();
			for (ExogenousDataset data : this.datasetNames) {
				model.addElement(data);
			}
			targeter = new ProMComboBox<ExogenousDataset>( model );
			c.insets = new Insets(5, 2, 0, 0);
			c.anchor = c.NORTHWEST;
			JLabel slicerLabel = new JLabel("Exo-Panel:");
			slicerLabel.setForeground(Color.white);
			add(slicerLabel, c);
			c.gridx++;
			c.insets = new Insets(0, 5, 5, 5);
			targeter.setMaximumSize(new Dimension(250,30));
			targeter.setMinimumSize(targeter.getMaximumSize());
			targeter.addActionListener(new NameUpdater(this));
			add(targeter, c);
			c.gridx++;
			c.gridy = 1;
		}
		//		add type drop down
		c.insets = new Insets(5, 2, 0, 0);
		c.anchor = c.NORTHWEST;
		JLabel slicerLabel = new JLabel("Slicer Type:");
		slicerLabel.setForeground(Color.white);
		add(slicerLabel, c);
		c.gridx++;
		c.insets = new Insets(0, 5, 5, 5);
		typeSelector = new ProMComboBox<SlicerChoices>( new DefaultComboBoxModel() );
		typeSelector.addItem(SlicerChoices.FUTURE_OUTCOME_SLICER);
		typeSelector.addItem(SlicerChoices.PAST_OUTCOME_SLICER);
		typeSelector.addItem(SlicerChoices.TIME_AWARE_SLICER);
		typeSelector.setFont(new Font("Times New Roman", Font.BOLD, 9));
		typeSelector.addItemListener(new SlicerTypeListener(this));
		add(typeSelector, c);
		c.gridx++;
		//		add period input
		c.insets = new Insets(5, 2, 0, 0);
		c.anchor = c.NORTHWEST;
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
		spinner.getComponent(2).setBackground(Color.LIGHT_GRAY);
		spinner.getComponent(2).setForeground(Color.LIGHT_GRAY);
		spinner.addChangeListener(new NameUpdater(this));
		add(spinner, c);
		//		add time period selector
		c.gridx++;
		c.insets = new Insets(0, 5, 5, 25);
		timeUnitSelector = new ProMComboBox<SlicerChoices>( new DefaultComboBoxModel() );
		timeUnitSelector.addItem(PeriodChoices.ms);
		timeUnitSelector.addItem(PeriodChoices.sec);
		timeUnitSelector.addItem(PeriodChoices.min);
		timeUnitSelector.addItem(PeriodChoices.hr);
		timeUnitSelector.addItem(PeriodChoices.d);
		timeUnitSelector.setFont(new Font("Times New Roman", Font.BOLD, 9));
		timeUnitSelector.setMaximumSize(new Dimension(75,30));
		timeUnitSelector.setMinimumSize(timeUnitSelector.getMaximumSize());
		timeUnitSelector.addActionListener(new NameUpdater(this));
		add(timeUnitSelector, c);
		c.gridx++;
		//		add name for slicer
		c.insets = new Insets(5, 2, 0, 0);
		c.anchor = c.NORTHWEST;
		spinnerlabel = new JLabel("Slicer Name:");
		spinnerlabel.setForeground(Color.white);
		add(spinnerlabel, c);
		c.gridx++;		
		namer = new ProMTextField(generateName());
		c.insets = new Insets(0, 0, 0, 15);
		add(namer, c);
		c.gridx++;
		//		add remove
		c.insets = new Insets(0, 0, 0, 0);
		JButton remove = new JButton("X");
		remove.setForeground(Color.white);
		remove.setBackground(ColorUtils.darken(Color.red, 150));
		remove.addMouseListener(new RemoveDialogListener(this));
		add(remove, c);
		//		set make size
		setMaximumSize(new Dimension(this.targeted ? 1300 : 900, 50));
		setMinimumSize(getMaximumSize());
		setPreferredSize(getMaximumSize());
		this.validate();
		return this;
	}

	public String generateName() {
		String name = "";

		if (this.targeted) {
			name = ((ExogenousDataset) this.targeter.getSelectedItem()).getName() + ":";
		}

		if (typeSelector.getSelectedItem().equals(SlicerChoices.FUTURE_OUTCOME_SLICER)) {
			name = name + "FOS";
		} else if (typeSelector.getSelectedItem().equals(SlicerChoices.PAST_OUTCOME_SLICER)) {
			name = name + "POS";
		} else if (typeSelector.getSelectedItem().equals(SlicerChoices.TIME_AWARE_SLICER)) {
			name = name + "TAS";
		}
		if (((SlicerChoices)typeSelector.getSelectedItem()).requirePeriod) {
			name = name + ":" + ((int) this.spinner.getValue()) + timeUnitSelector.getSelectedItem().toString();
		}

		return name;
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

	public List<Determination> makePartials() {
		List<Determination> partials = new ArrayList();
		
		if (this.targeted) {
			ExogenousDataset target = (ExogenousDataset) this.targeter.getSelectedItem();
			partials.add(
					Determination.builder()
					.panel(target)
					.linker(target.getLinker())
					.slicer(makeSlicer())
					.transformer(new EmptyTransform())
					.build()
			);
		} else {
			for(ExogenousDataset target: this.datasetNames) {
				partials.add(
						Determination.builder()
						.panel(target)
						.linker(target.getLinker())
						.slicer(makeSlicer())
						.transformer(new EmptyTransform())
						.build()
				);
			}
		}
		return partials;
	}

	private class NameUpdater implements ActionListener,ChangeListener{

		private DialogSlicerSelector source;

		public NameUpdater(DialogSlicerSelector source) {
			this.source = source;
		}

		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			this.source.namer.setText(this.source.generateName());
		}

		public void stateChanged(ChangeEvent e) {
			// TODO Auto-generated method stub
			this.source.namer.setText(this.source.generateName());
		}

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
				this.source.namer.setText(this.source.generateName());;
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
		ms(1), sec(1000), min(1000 * 60), hr(1000 * 60 * 60), d(1000 * 60 * 60 * 24);

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
