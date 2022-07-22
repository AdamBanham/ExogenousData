package org.processmining.qut.exogenousaware.steps.slicing.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.processmining.qut.exogenousaware.data.ExogenousDataset;
import org.processmining.qut.exogenousaware.steps.determination.Determination;
import org.processmining.qut.exogenousaware.steps.slicing.Slicer;
import org.processmining.qut.exogenousaware.steps.slicing.data.SlicingConfiguration;

import com.fluxicon.slickerbox.util.ColorUtils;

import lombok.Builder;
import lombok.Builder.Default;

@Builder
public class SlicingConfigurationDialog extends JPanel {

	@Default
	List<ExogenousDataset> datasets = new ArrayList<ExogenousDataset>();

	@Default
	private List<DialogSlicerSelector> generalSlicerSelectors = new ArrayList<DialogSlicerSelector>();
	@Default
	private List<DialogSlicerSelector> targetedSlicerSelectors = new ArrayList<DialogSlicerSelector>();
	@Default
	private JScrollPane selectedGeneralSlicers = new JScrollPane(new JPanel());
	@Default
	private JScrollPane selectedTargetedSlicers = new JScrollPane(new JPanel());

	public SlicingConfigurationDialog setup() {
		setBackground(Color.DARK_GRAY);
		//		set layout and create manager
		GridBagConstraints c = new GridBagConstraints();
		setLayout(new GridBagLayout());
		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1.0;
		//		add label for general slicers
		c.anchor = GridBagConstraints.LAST_LINE_START;
		c.insets = new Insets(5, 5, 5, 5);
		JLabel general = new JLabel("General Slicers:");
		general.setForeground(Color.white);
		general.setAlignmentX(CENTER_ALIGNMENT);
		general.setAlignmentY(CENTER_ALIGNMENT);
		add(general, c);
		c.gridx++;
		c.anchor = GridBagConstraints.LAST_LINE_END;
		c.weighty = 0.0;
		//		add button to add new slicer dialogs
		JButton gadd = new JButton("Add Slicer");
		gadd.setBackground(ColorUtils.darken(Color.GREEN, 100));
		gadd.setForeground(Color.white);
		gadd.addMouseListener(new GeneralSlicerListener(this));
		add(gadd, c);
		//		setup the scroll view and add
		c.anchor = GridBagConstraints.LAST_LINE_START;
		c.gridx = 0;
		c.gridwidth = 2;
		c.gridy++;
		this.selectedGeneralSlicers.setBackground(Color.DARK_GRAY);
		this.selectedGeneralSlicers.setMaximumSize(new Dimension(650, 400));
		this.selectedGeneralSlicers.setMinimumSize(new Dimension(650, 400));
		this.selectedGeneralSlicers.setPreferredSize(new Dimension(650, 400));
		add(selectedGeneralSlicers, c);
		JPanel view = (JPanel) this.selectedGeneralSlicers.getViewport().getView();
		view.add(Box.createRigidArea(new Dimension(600, 5)));
		view.setBackground(Color.DARK_GRAY);
		view.setLayout(new BoxLayout(view, BoxLayout.Y_AXIS));
		view.validate();
		c.gridy++;
		c.gridwidth = 1;
		c.gridx = 0;
		//	add targeted slicers
		//	add label for general slicers
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.LAST_LINE_START;
		c.insets = new Insets(5, 5, 5, 5);
		JLabel target = new JLabel("Targeted Slicers:");
		target.setForeground(Color.white);
		target.setAlignmentX(CENTER_ALIGNMENT);
		target.setAlignmentY(CENTER_ALIGNMENT);
		add(target, c);
		c.gridx++;
		c.anchor = GridBagConstraints.LAST_LINE_END;
		c.weighty = 0.0;
		// add button to add slicers
		JButton tadd = new JButton("Add Slicer");
		tadd.setBackground(ColorUtils.darken(Color.GREEN, 100));
		tadd.setForeground(Color.white);
		tadd.addMouseListener(new TargetedSlicerListener(this));
		add(tadd, c);
		// add scroll pane for dialogs
		c.anchor = GridBagConstraints.LAST_LINE_START;
		c.gridx = 0;
		c.gridwidth = 2;
		c.gridy++;
		this.selectedTargetedSlicers.setBackground(Color.DARK_GRAY);
		this.selectedTargetedSlicers.setMaximumSize(new Dimension(650, 400));
		this.selectedTargetedSlicers.setMinimumSize(new Dimension(650, 400));
		this.selectedTargetedSlicers.setPreferredSize(new Dimension(650, 400));
		add(selectedTargetedSlicers, c);
		JPanel tview = (JPanel) this.selectedTargetedSlicers.getViewport().getView();
		tview.add(Box.createRigidArea(new Dimension(600, 5)));
		tview.setBackground(Color.DARK_GRAY);
		tview.setLayout(new BoxLayout(tview, BoxLayout.Y_AXIS));
		tview.validate();
		return this;
	}

	public SlicingConfiguration generateConfig() {
		List<Slicer> general = new ArrayList<Slicer>();
		for (DialogSlicerSelector maker : this.generalSlicerSelectors) {
			general.add(maker.makeSlicer());
		}
		Map<String, List<Slicer>> target = new HashMap<String, List<Slicer>>();
		for (DialogSlicerSelector maker : this.targetedSlicerSelectors) {
				ExogenousDataset targeter = (ExogenousDataset) maker.getTargeter().getSelectedItem(); 
				if (target.containsKey(targeter.getName())) {
					target.get(targeter.getName()).add(maker.makeSlicer());
				} else {
					target.put(targeter.getName(), new ArrayList<Slicer>());
					target.get(targeter.getName()).add(maker.makeSlicer());
				}
		}
		return SlicingConfiguration.builder().generalSlicers(general).targetedSlicers(target).build();
	}

	public List<Determination> generatePartials() {
		List<Determination> generated = new ArrayList();
		
//		go through general slicers
		for (DialogSlicerSelector selector: this.generalSlicerSelectors) {
			generated.addAll(selector.makePartials());
		}
//		go through targeted slicers
		for (DialogSlicerSelector selector: this.targetedSlicerSelectors) {
			generated.addAll(selector.makePartials());
		}
		return generated;
	}
	
	private class RemovedTargetSlicerListener implements PropertyChangeListener {
		
		private DialogSlicerSelector maker;
		private SlicingConfigurationDialog source;

		public RemovedTargetSlicerListener(DialogSlicerSelector maker, SlicingConfigurationDialog source) {
			this.maker = maker;
			this.source = source;
		}

		public void propertyChange(PropertyChangeEvent evt) {
			this.source.targetedSlicerSelectors.remove(maker);
			JPanel view = (JPanel) this.source.selectedTargetedSlicers.getViewport().getView();
			for(int i=0; i<view.getComponentCount();i++) {
				if (view.getComponent(i).equals(maker)) {
					view.remove(i-1);
				}
			}
			view.remove(maker);
			//			for the edge case of no components in panel, triggers repaint
			view.setVisible(false);
			view.setVisible(true);
			// now update view
			view.validate();
			this.source.selectedTargetedSlicers.getViewport().validate();
			this.source.selectedTargetedSlicers.validate();
			this.source.validate();
		}
	}
	
	private class RemoveGeneralSlicerListener implements PropertyChangeListener {

		private DialogSlicerSelector maker;
		private SlicingConfigurationDialog source;

		public RemoveGeneralSlicerListener(DialogSlicerSelector maker, SlicingConfigurationDialog source) {
			this.maker = maker;
			this.source = source;
		}

		public void propertyChange(PropertyChangeEvent evt) {
			this.source.generalSlicerSelectors.remove(maker);
			JPanel view = (JPanel) this.source.selectedGeneralSlicers.getViewport().getView();
			for(int i=0; i<view.getComponentCount();i++) {
				if (view.getComponent(i).equals(maker)) {
					view.remove(i-1);
				}
			}
			view.remove(maker);
			//			for the edge case of no components in panel, triggers repaint
			view.setVisible(false);
			view.setVisible(true);
			// now update view
			view.validate();
			this.source.selectedGeneralSlicers.getViewport().validate();
			this.source.selectedGeneralSlicers.validate();
			this.source.validate();
		}

	}

	private class TargetedSlicerListener implements MouseListener {

		private SlicingConfigurationDialog source;

		public TargetedSlicerListener(SlicingConfigurationDialog source) {
			this.source = source;
		}

		public void mouseClicked(MouseEvent e) {
			JPanel view = (JPanel) this.source.selectedTargetedSlicers.getViewport().getView();
			DialogSlicerSelector maker = DialogSlicerSelector.builder()
					.targeted(true)
					.datasetNames(this.source.datasets)
					.build()
					.setup();
			this.source.targetedSlicerSelectors.add(maker);
			maker.addPropertyChangeListener("remove-parent", new RemovedTargetSlicerListener(maker, source));
			view.add(Box.createRigidArea(new Dimension(600, 5)));
			view.add(maker);
			view.validate();
			this.source.selectedTargetedSlicers.validate();
			this.source.validate();
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

	private class GeneralSlicerListener implements MouseListener {

		private SlicingConfigurationDialog source;
		private GridBagConstraints c = new GridBagConstraints();
		

		public GeneralSlicerListener(SlicingConfigurationDialog source) {
			this.source = source;
			c.insets = new Insets(0,0,0,5);
		}

		public void mouseClicked(MouseEvent e) {
			JPanel view = (JPanel) this.source.selectedGeneralSlicers.getViewport().getView();
			DialogSlicerSelector maker = DialogSlicerSelector.builder().datasetNames(datasets).build().setup();
			this.source.generalSlicerSelectors.add(maker);
			maker.addPropertyChangeListener("remove-parent", new RemoveGeneralSlicerListener(maker, source));
			view.add(Box.createRigidArea(new Dimension(600, 5)));
			view.add(maker, c);
			view.validate();
			this.source.selectedGeneralSlicers.validate();
			this.source.validate();
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
