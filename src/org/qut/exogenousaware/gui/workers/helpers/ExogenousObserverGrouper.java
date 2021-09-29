package org.qut.exogenousaware.gui.workers.helpers;

import org.deckfour.xes.model.XTrace;
import org.qut.exogenousaware.steps.slicing.data.SubSeries;

/**
 * This is a interface for a grouper class, which defines clear groups of subseries based on some interesting aspect of the related trace 
 * or sliced subseries.
 * 
 * @author Adam Banham
 *
 */
public interface ExogenousObserverGrouper {

	/**
	 * This function finds a group for a given trace and subseries to be used later in enhancement graphs.
	 * @param trace
	 * @param sliced
	 * @return a group identifier
	 */
	public int findGroup(XTrace trace, SubSeries sliced);
	
	/**
	 * This function finds a group for a given trace and subseries, but only considers up to the n-th event in a trace.
	 * @param trace
	 * @param sliced
	 * @param n
	 * @return a group identifier
	 */
	public int findGroup(XTrace trace, SubSeries sliced,int n);
	
	/**
	 * Finds a human readable string representation for a group identifier.
	 * @param group
	 * @return group name
	 */
	public String getGroupName(int group);
	
}
