package org.processmining.qut.exogenousaware.steps.determination;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.qut.exogenousaware.data.ExogenousDataset;
import org.processmining.qut.exogenousaware.steps.linking.Linker;
import org.processmining.qut.exogenousaware.steps.slicing.Slicer;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;
import org.processmining.qut.exogenousaware.steps.transform.type.Transformer;

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
	
	/**
	 * Apply the internal steps of an determination to an endogenous trace. 
	 * This may result in transformed attributes being added to events that
	 * belong to the endogenous trace.
	 * @param endo : the endogenous trace to be considered
	 * @return A list of found subseries from this determination
	 */
	public List<SubSeries> apply(XTrace endo){
		List<SubSeries> foundSubSeries = new ArrayList<>();
//		perform the linkage
		List<XTrace> exogenousSeries = linker.link(endo, panel.getSource());
//		if we have some linkage, then perform slicing
		for(XTrace exogenous: exogenousSeries) {
			Map<XEvent, SubSeries> result = slicer.slice(endo, exogenous, panel);
//			if we have some subseries, then perform transformation
			for(Entry<XEvent, SubSeries> item: result.entrySet()) {
				TransformedAttribute xattr = transformer.transform(item.getValue());
				if (xattr != null) {
					item.getKey().getAttributes()
						.put(xattr.getKey(), xattr);
				}
				foundSubSeries.add(item.getValue());
			}
		}			
		return foundSubSeries;
	}

}
