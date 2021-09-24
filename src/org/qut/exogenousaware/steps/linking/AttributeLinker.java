package org.qut.exogenousaware.steps.linking;

import java.util.ArrayList;
import java.util.List;

import org.deckfour.xes.model.XTrace;

import lombok.Builder;
import lombok.NonNull;

/**
 * A linking function that tests if an endogenous trace and exogenous time series have an attribute in common and that the value matches.<br>
 * 
 * This class uses the builder pattern. Use AttributeLinker.builder() to create new instances.
 * 
 * @author n7176546
 *
 */
@Builder
public class AttributeLinker implements Linker {
	
	@NonNull String attributeName;
	

	public List<XTrace> link(XTrace endogenous, List<XTrace> exoDataset) {
		List<XTrace> linkedSubset = new ArrayList<XTrace>();
//		check that endogenous trace has attribute of focus
		Object endoAttr = endogenous.getAttributes().get(this.attributeName);
		if (endoAttr == null) {
			return linkedSubset;
		}
//		check for each exogenous trace that it has the attribute and matches the endogenous trace
		for(XTrace exo: exoDataset) {
			Object exoAttr = exo.getAttributes().get(this.attributeName);
			if (exoAttr == null) {
				continue;
			}
			if (exoAttr.equals(endoAttr)) {
				linkedSubset.add(exo);
			}
		}
		return linkedSubset;
	}

}
