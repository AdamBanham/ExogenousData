package org.processmining.qut.exogenousaware.data;

import java.util.ArrayList;
import java.util.List;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.qut.exogenousaware.exceptions.LinkNotFoundException;
import org.processmining.qut.exogenousaware.steps.linking.Linker;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class ExogenousDataset {

	@NonNull @Getter XLog source;
	
	@Default @Getter ExogenousDatasetLinkType linkType = null;
	@Default @Getter ExogenousDatasetType dataType = null;
	@Default @Getter Linker linker = null;
	
	public ExogenousDataset setup() {
		
		
		return this;
	}
	
	/**
	 * Checks for a link between a trace and this exogenous dataset.
	 * @param trace to check for linkage
	 * @return whether linkage was found.
	 */
	public boolean checkLink(XTrace trace) {
		return false;
	}
	
	
	/**
	 * Checks for a link between a trace and returns the links found.
	 * @param trace to use as link source
	 * @return a collection of links
	 */
	public List<XTrace> findLinkage(XTrace trace) throws LinkNotFoundException {
//		check for link
		if (!checkLink(trace)) {
			throw new LinkNotFoundException();
		}
//		otherwise, find links
		List<XTrace> links = new ArrayList<XTrace>();
		
		return links;
	}
	
}
