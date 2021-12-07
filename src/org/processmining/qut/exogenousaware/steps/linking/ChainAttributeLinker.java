package org.processmining.qut.exogenousaware.steps.linking;

import java.util.ArrayList;
import java.util.List;

import org.deckfour.xes.model.XTrace;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Singular;

@Builder
public class ChainAttributeLinker implements Linker {

	@Getter @Singular List<String> attributes; 
	
	@Default private Boolean setupCompleted = false;
	@Default private List<AttributeLinker> linkers = new ArrayList<AttributeLinker>();
	
	public ChainAttributeLinker setup() {
		for(String attribute: this.attributes) {
			AttributeLinker temp = AttributeLinker.builder()
					.attributeName(attribute)
					.build();
			linkers.add(temp);
		}
		this.setupCompleted = true;
		return this;
	}
	
	public List<XTrace> link(XTrace endogenous, List<XTrace> exoDataset) {
//		check that setup was completed
		if (!this.setupCompleted) {
			setup();
		}
//		find all links
		List<XTrace> linkedSubset = new ArrayList<XTrace>();
//		check that endogenous trace has attribute of focus
		for(String attribute: this.attributes) {
			if (!endogenous.getAttributes().containsKey(attribute)) {
				return linkedSubset;
			}
		}
//		check for each exogenous trace that it has the attribute and matches the endogenous trace
		for(XTrace exo: exoDataset) {
			Boolean check = true;
			for(Linker linker: this.linkers) {
				check = check & linker.linkedTo(endogenous, exo);
			}
			if (check) {
				linkedSubset.add(exo);
			}
		}
		return linkedSubset;
	}

	public Boolean linkedTo(XTrace endogenous, XTrace exogenous) {
		for(String attribute: this.attributes) {
			if (!endogenous.getAttributes().containsKey(attribute)) {
				return false;
			}
		}
		Boolean check = true;
		for(Linker linker: this.linkers) {
			check = check & linker.linkedTo(endogenous, exogenous);
		}
		return check;
	}

}
