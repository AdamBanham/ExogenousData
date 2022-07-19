package org.processmining.qut.exogenousaware.steps.determination;

import org.processmining.qut.exogenousaware.data.ExogenousDataset;
import org.processmining.qut.exogenousaware.steps.linking.Linker;
import org.processmining.qut.exogenousaware.steps.slicing.Slicer;
import org.processmining.qut.exogenousaware.steps.transform.Transformer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
public class Determination {
	
	@Getter protected ExogenousDataset panel;
	@Getter protected Linker linker;
	@Getter protected Slicer slicer;
	@Getter protected Transformer transformer;
	
	
	@Override
	public String toString() {
		String name = panel.getName() + " -> ";
		name = name + linker.getName() + " -> ";
		name = name + slicer.getName() + " -> ";
		name = name + transformer.getName();
		return name;
	}

}
