package org.processmining.qut.exogenousaware.steps.slicing.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.processmining.qut.exogenousaware.data.ExogenousDataset;
import org.processmining.qut.exogenousaware.steps.slicing.data.SlicingConfiguration;

import lombok.Builder;
import lombok.Builder.Default;

@Builder
public class SlicingConfigurationDialog extends JPanel {

	@Default List<ExogenousDataset> datasets = new ArrayList<ExogenousDataset>();
	
	
	public SlicingConfigurationDialog setup() {
		return this;
	}
	
	public SlicingConfiguration generateConfig() {
		return SlicingConfiguration.builder()
				.generalSlicers(null)
				.targetedSlicers(null)
				.build();
	}
	
}
