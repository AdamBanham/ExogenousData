package org.processmining.qut.exogenousaware.steps.linking;

import java.util.List;

import org.deckfour.xes.model.XTrace;


/**
 * A set of common functions and a linking function needs to have to work within the xPM framework.
 * 
 * @author n7176546
 *
 */
public interface Linker {

	/**
	 * This function finds a subset of an exoDataset that are linked to an endogenous trace.<br>
	 * 
	 * Future steps can be proposed to handled subsets of greater than one but current implementation will only use one member of the subset.
	 *
	 * @param endogenous The trace under consideration for linkage 
	 * @param exoDataset The collection of exogenous time series within a exogenous data set.
	 * @return A subset of the collection, which have been found to have a link with the endogenous trace.
	 */
	public List<XTrace> link(XTrace endogenous, List<XTrace> exoDataset);
	
	/**
	 * This function checks for a link between an endogenous and exogenous trace.<br>
	 *
	 * @param endogenous The trace under consideration for linkage 
	 * @param exogenous The exogenous trace that could be linked.
	 * @return whether these two traces are linked.
	 */
	public Boolean linkedTo(XTrace endogenous, XTrace exogenous);
	
	
}
